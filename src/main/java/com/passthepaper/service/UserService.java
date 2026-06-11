package com.passthepaper.service;

import com.passthepaper.entity.*;
import com.passthepaper.exception.AppException;
import com.passthepaper.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository        userRepo;
    private final NotificationService   notifService;
    private final LogService            logService;
    // For cascade-safe account deletion
    private final FeedbackRepository    feedbackRepo;
    private final ResourceRepository    resourceRepo;
    private final CartItemRepository    cartRepo;
    private final PurchaseRepository    purchaseRepo;
    private final TransactionRepository txnRepo;
    private final WithdrawalRepository  withdrawalRepo;
    private final AppealRepository      appealRepo;
    private final NotificationRepository notifRepo;

    public User getById(UUID id) {
        return userRepo.findById(id).orElseThrow(() -> new AppException("User not found"));
    }

    @Transactional
    public void updateProfile(UUID userId, com.passthepaper.dto.UserDto.UpdateProfileRequest req) {
        User user = getById(userId);
        if (req.name()           != null) user.setName(req.name());
        if (req.university()     != null) user.setUniversity(req.university());
        if (req.studentId()      != null) user.setStudentId(req.studentId());
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

    /**
     * Deletes account + ALL associated data in FK order so no constraint is violated.
     * Hibernate ddl-auto=update does NOT add ON DELETE CASCADE, so we delete explicitly.
     *
     * Order:
     *  1  feedbacks written BY this user
     *  2  for each resource uploaded by this user:
     *       feedbacks by OTHERS about that resource
     *       cart_items of OTHERS for that resource
     *       purchases  of OTHERS for that resource
     *  3  the resources themselves
     *  4  purchases made BY this user (of other resources)
     *  5  cart items in this user's cart
     *  6  transactions
     *  7  withdrawals
     *  8  notifications
     *  9  appeals
     * 10  user record
     */
    @Transactional
    public void deleteAccount(UUID userId, String currentPassword,
                               org.springframework.security.crypto.password.PasswordEncoder encoder) {
        User user = getById(userId);
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AppException("Incorrect password. Please try again.");
        }

        // 1. Feedbacks this user wrote
        feedbackRepo.deleteByUser(user);

        // 2+3. Resources this user uploaded and their dependents
        List<Resource> mine = resourceRepo.findByUploadedByOrderByCreatedAtDesc(user);
        for (Resource r : mine) {
            feedbackRepo.deleteByItem(r);
            cartRepo.deleteByResource(r);
            purchaseRepo.deleteByResource(r);
        }
        if (!mine.isEmpty()) resourceRepo.deleteAll(mine);

        // 4. Purchases made BY this user
        purchaseRepo.deleteByUser(user);

        // 5. Cart items IN this user's cart
        cartRepo.deleteByUser(user);

        // 6. Transactions
        txnRepo.deleteByUser(user);

        // 7. Withdrawals
        withdrawalRepo.deleteByUser(user);

        // 8. Notifications
        notifRepo.deleteByUser(user);

        // 9. Appeals
        appealRepo.deleteByUser(user);

        // 10. The user
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

    @Transactional
    public void approveIdCard(UUID userId, boolean approve, UUID adminId) {
        User user = getById(userId);
        user.setIdCardStatus(approve ? User.IdCardStatus.approved : User.IdCardStatus.rejected);
        if (approve) user.setIsVerified(true);
        userRepo.save(user);
        notifService.send(user, Notification.NotificationType.system,
                approve ? "ID Card Approved" : "ID Card Rejected",
                approve ? "Your student ID card has been approved. Your account is verified."
                        : "Your ID card was rejected. Please upload a clear and valid ID card again.", null);
        logService.log(Log.LogType.admin_action, approve ? "ID_CARD_APPROVED" : "ID_CARD_REJECTED",
                (approve ? "Approved" : "Rejected") + " ID card for " + user.getName(),
                adminId, null, userId, user.getName(), null);
    }

    @Transactional
    public void banUser(UUID userId, String reason, UUID adminId) {
        User user = getById(userId);
        user.setIsBanned(true);
        user.setBanReason(reason);
        userRepo.save(user);
        notifService.send(user, Notification.NotificationType.system, "Account Banned",
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
        if (req.canUpload()   != null) user.setCanUpload(req.canUpload());
        if (req.canPurchase() != null) user.setCanPurchase(req.canPurchase());
        if (req.canComment()  != null) user.setCanComment(req.canComment());
        userRepo.save(user);
    }

    @Transactional
    public void removeVerification(UUID userId, String reason, UUID adminId) {
        User user = getById(userId);
        user.setIsVerified(false);
        user.setIdCardStatus(User.IdCardStatus.none);
        user.setIdCardImage(null);
        userRepo.save(user);
        notifService.send(user, Notification.NotificationType.system, "Verification Removed",
                "Your student verification was removed. Reason: " + reason + ". Please re-verify.", null);
    }

    @Scheduled(fixedDelay = 60_000)
    public void expireMemberships() {
        Instant now = Instant.now();
        userRepo.findAll().stream()
                .filter(u -> u.getMembershipType() != User.MembershipPlan.free
                        && u.getMembershipExpiry() != null
                        && u.getMembershipExpiry().isBefore(now))
                .forEach(u -> { u.setMembershipType(User.MembershipPlan.free); u.setMembershipExpiry(null); userRepo.save(u); });
    }

    public List<User> getAllStudents()    { return userRepo.findByIsAdminFalseAndIsBannedFalse(); }
    public List<User> getPendingIdCards() { return userRepo.findByIdCardStatus(User.IdCardStatus.pending); }
}
