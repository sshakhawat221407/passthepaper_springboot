package com.passthepaper.service;

import com.passthepaper.dto.AppealDto;
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
public class AppealService {

    private final AppealRepository appealRepo;
    private final UserRepository userRepo;
    private final NotificationService notifService;

@Transactional
public Appeal submitAppeal(UUID userId, AppealDto.CreateRequest req) {
    User user = userRepo.findById(userId).orElseThrow(() -> new AppException("User not found"));
    return appealRepo.save(Appeal.builder()
            .user(user).userName(user.getName()).userEmail(user.getEmail())
            .reason(req.reason()).status(Appeal.AppealStatus.pending).build());
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
