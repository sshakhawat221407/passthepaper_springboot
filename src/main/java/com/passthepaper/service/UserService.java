package com.passthepaper.service;

import com.passthepaper.entity.*;
import com.passthepaper.exception.AppException;
import com.passthepaper.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final NotificationService notifService;
    private final LogService logService;

    public User getById(UUID id) {
        return userRepo.findById(id).orElseThrow(() -> new AppException("User not found"));
    }

    @Transactional
    public void updateProfile(UUID userId, com.passthepaper.dto.UserDto.UpdateProfileRequest req) {
        User user = getById(userId);
        if (req.name() != null) user.setName(req.name());
        if (req.university() != null) user.setUniversity(req.university());
        if (req.studentId() != null) user.setStudentId(req.studentId());
        if (req.profilePicture() != null) user.setProfilePicture(req.profilePicture());
        userRepo.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, String currentRaw, String newRaw,
                               org.springframework.security.crypto.password.PasswordEncoder encoder) {
        User user = getById(userId);
        if (!encoder.matches(currentRaw, user.getPasswordHash())) {
            throw new AppException("Current password is incorrect");
        }
        user.setPasswordHash(encoder.encode(newRaw));
        userRepo.save(user);
    }
@Transactional
public void deleteAccount(UUID userId, String currentPassword,
                           org.springframework.security.crypto.password.PasswordEncoder encoder) {
    User user = getById(userId);
    if (!encoder.matches(currentPassword, user.getPasswordHash())) {
        throw new AppException("Incorrect password. Please try again.");
    }
    // All related rows (resources, transactions, notifications, purchases,
    // cart_items, withdrawals, feedbacks, appeals, logs) are deleted
    // automatically because every FK has ON DELETE CASCADE in the schema.
    userRepo.deleteById(userId);
}
    @Transactional
    public void uploadIdCard(UUID userId, String base64Image) {
        User user = getById(userId);
        user.setIdCardImage(base64Image);
        user.setIdCardStatus(User.IdCardStatus.pending);
        userRepo.save(user);
        logService.log(Log.LogType.verification, "ID_CARD_SUBMITTED",
                "User submitted ID card for verification", userId, user.getName(), null, null, null);
    }

    // ─── Admin actions ───────────────────────────────────

    @Transactional
    public void approveIdCard(UUID userId, boolean approve, UUID adminId) {
        User user = getById(userId);
        user.setIdCardStatus(approve ? User.IdCardStatus.approved : User.IdCardStatus.rejected);
        if (approve) user.setIsVerified(true);
        userRepo.save(user);
        notifService.send(user, Notification.NotificationType.system,
                approve ? "ID Card Approved" : "ID Card Rejected",
                approve ? "Your student ID card has been approved. Your account is verified."
                        : "Your ID card was rejected. Please upload a clear and valid ID card again.",
                null);
        logService.log(Log.LogType.admin_action,
                approve ? "ID_CARD_APPROVED" : "ID_CARD_REJECTED",
                (approve ? "Approved" : "Rejected") + " ID card for " + user.getName(),
                adminId, null, userId, user.getName(), null);
    }

    @Transactional
    public void banUser(UUID userId, String reason, UUID adminId) {
        User user = getById(userId);
        user.setIsBanned(true);
        user.setBanReason(reason);
        userRepo.save(user);
        notifService.send(user, Notification.NotificationType.system,
                "Account Banned",
                "Your account has been banned. Reason: " + reason + ". Submit an appeal if in error.", null);
        logService.log(Log.LogType.admin_action, "USER_BANNED",
                "Admin banned user " + user.getName() + ". Reason: " + reason,
                adminId, null, userId, user.getName(), Map.of("reason", reason));
    }

    @Transactional
    public void unbanUser(UUID userId, UUID adminId) {
        User user = getById(userId);
        user.setIsBanned(false);
        user.setBanReason(null);
        userRepo.save(user);
        logService.log(Log.LogType.admin_action, "USER_UNBANNED",
                "Admin unbanned user " + user.getName(), adminId, null, userId, user.getName(), null);
    }

    @Transactional
    public void setRestrictions(UUID userId, com.passthepaper.dto.UserDto.RestrictionsRequest req) {
        User user = getById(userId);
        if (req.canUpload() != null) user.setCanUpload(req.canUpload());
        if (req.canPurchase() != null) user.setCanPurchase(req.canPurchase());
        if (req.canComment() != null) user.setCanComment(req.canComment());
        userRepo.save(user);
    }

    @Transactional
    public void removeVerification(UUID userId, String reason, UUID adminId) {
        User user = getById(userId);
        user.setIsVerified(false);
        user.setIdCardStatus(User.IdCardStatus.none);
        user.setIdCardImage(null);
        userRepo.save(user);
        notifService.send(user, Notification.NotificationType.system,
                "Verification Removed",
                "Your student verification was removed. Reason: " + reason + ". Please re-verify.", null);
    }

    /** Scheduled job: expire memberships */
    @Scheduled(fixedDelay = 60_000)
    public void expireMemberships() {
        Instant now = Instant.now();
        userRepo.findAll().stream()
                .filter(u -> u.getMembershipType() != User.MembershipPlan.free
                        && u.getMembershipExpiry() != null
                        && u.getMembershipExpiry().isBefore(now))
                .forEach(u -> {
                    u.setMembershipType(User.MembershipPlan.free);
                    u.setMembershipExpiry(null);
                    userRepo.save(u);
                });
    }

    public List<User> getAllStudents() { return userRepo.findByIsAdminFalseAndIsBannedFalse(); }
    public List<User> getPendingIdCards() { return userRepo.findByIdCardStatus(User.IdCardStatus.pending); }
}
