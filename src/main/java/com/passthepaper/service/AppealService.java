package com.passthepaper.service;

import com.passthepaper.dto.AppealDto;
import com.passthepaper.entity.*;
import com.passthepaper.exception.AppException;
import com.passthepaper.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AppealService {

    private final AppealRepository appealRepo;
    private final UserRepository userRepo;
    private final NotificationService notifService;
    private final LogService logService;

    @Transactional
    public Appeal submitAppeal(UUID userId, AppealDto.CreateRequest req) {
        User user = userRepo.findById(userId).orElseThrow(() -> new AppException("User not found"));
        Appeal appeal = new Appeal();
        appeal.setUser(user);
        appeal.setUserName(user.getName());
        appeal.setUserEmail(user.getEmail());
        appeal.setReason(req.reason());
        appeal.setStatus(Appeal.AppealStatus.pending);
        Appeal saved = appealRepo.save(appeal);
        logService.log(Log.LogType.user_action, "APPEAL_SUBMITTED",
                "User submitted a support message: " + user.getName(),
                user.getId(), user.getName(), null, null, null);
        return saved;
    }

    @Transactional
    public void deleteAppeal(UUID appealId, UUID userId) {
        Appeal appeal = appealRepo.findById(appealId)
                .orElseThrow(() -> new AppException("Message not found"));
        if (!appeal.getUser().getId().equals(userId)) {
            throw new AppException("Not authorized to delete this message");
        }
        User user = appeal.getUser();
        appealRepo.deleteById(appealId);
        logService.log(Log.LogType.user_action, "APPEAL_DELETED",
                "User deleted their support message: " + user.getName(),
                user.getId(), user.getName(), null, null, null);
    }

    @Transactional
    public void reviewAppeal(UUID appealId, AppealDto.ReviewRequest req, UUID adminId) {
        Appeal appeal = appealRepo.findById(appealId)
                .orElseThrow(() -> new AppException("Appeal not found"));
        appeal.setStatus(req.approve() ? Appeal.AppealStatus.approved : Appeal.AppealStatus.rejected);
        appeal.setAdminResponse(req.adminResponse());
        appeal.setReviewedAt(Instant.now());
        appealRepo.save(appeal);
        if (req.approve()) {
            User user = appeal.getUser();
            user.setIsBanned(false);
            user.setBanReason(null);
            userRepo.save(user);
        }
        User admin = userRepo.findById(adminId).orElse(null);
        String adminName = admin != null ? admin.getName() : "Admin";
        logService.log(Log.LogType.admin_action,
                req.approve() ? "APPEAL_APPROVED" : "APPEAL_REJECTED",
                "Admin " + (req.approve() ? "approved" : "rejected") + " appeal from " + appeal.getUserName(),
                adminId, adminName, appeal.getUser().getId(), appeal.getUserName(), null);
        notifService.send(appeal.getUser(), Notification.NotificationType.system,
                req.approve() ? "Appeal Approved" : "Appeal Rejected",
                (req.approve() ? "Your appeal has been approved. Account unbanned."
                        : "Your appeal has been rejected.")
                + (req.adminResponse() != null ? " Admin: " + req.adminResponse() : ""),
                null);
    }

    public List<Appeal> getMyAppeals(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return appealRepo.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Appeal> getAllPending() {
        return appealRepo.findByStatusOrderByCreatedAtDesc(Appeal.AppealStatus.pending);
    }
}
