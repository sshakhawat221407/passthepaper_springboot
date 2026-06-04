package com.passthepaper.service;

import com.passthepaper.dto.TransactionDto;
import com.passthepaper.dto.WalletDto;
import com.passthepaper.entity.*;
import com.passthepaper.exception.AppException;
import com.passthepaper.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserRepository userRepo;
    private final TransactionRepository txnRepo;
    private final WithdrawalRepository withdrawalRepo;
    private final NotificationService notificationService;
    private final LogService logService;
    // Add these two fields to WalletService (with @RequiredArgsConstructor they inject automatically):
private final PurchaseRepository purchaseRepo;
private final CartItemRepository cartRepo;
private final ResourceRepository resourceRepo;
    /** User requests a wallet top-up (requires admin approval) */
    @Transactional
    public void requestTopup(UUID userId, TransactionDto.AddFundsRequest req) {
        User user = userRepo.findById(userId).orElseThrow(() -> new AppException("User not found"));

        Transaction.PaymentMethod pm;
        try { pm = Transaction.PaymentMethod.valueOf(req.paymentMethod()); }
        catch (Exception e) { throw new AppException("Invalid payment method"); }

        Transaction txn = Transaction.builder()
                .user(user)
                .type(Transaction.TxnType.add)
                .amount(req.amount())
                .currency(Transaction.TxnCurrency.BDT)
                .description("Wallet top-up via " + pm.name())
                .paymentMethod(pm)
                .status(Transaction.TxnStatus.pending)
                .paymentPhone(req.paymentPhone())
                .transactionNumber(req.transactionNumber())
                .build();
        txnRepo.save(txn);
        // Increment pending balance
        user.setPendingBalance(user.getPendingBalance().add(req.amount()));
        userRepo.save(user);
    }

    /** Admin approves a top-up transaction */
   @Transactional
public void approveTransaction(UUID txnId, UUID adminId) {
    Transaction txn = txnRepo.findById(txnId)
            .orElseThrow(() -> new AppException("Transaction not found"));
    if (txn.getStatus() != Transaction.TxnStatus.pending) throw new AppException("Already processed");

    txn.setStatus(Transaction.TxnStatus.approved);
    txnRepo.save(txn);

    User user = txn.getUser();

    if (txn.getType() == Transaction.TxnType.add) {
        // Wallet top-up approval
        user.setWalletBalance(user.getWalletBalance().add(txn.getAmount()));
        user.setPendingBalance(user.getPendingBalance().subtract(txn.getAmount()));
        userRepo.save(user);
        notificationService.send(user, Notification.NotificationType.system,
                "Wallet Top-up Approved",
                "Your BDT " + txn.getAmount() + " wallet top-up has been approved.", null);
        logService.log(Log.LogType.admin_action, "TOPUP_APPROVED",
                "Admin approved top-up of ৳" + txn.getAmount() + " for " + user.getName(),
                adminId, null, user.getId(), user.getName(),
                Map.of("amount", txn.getAmount().toString()));

    } else if (txn.getType() == Transaction.TxnType.purchase) {
        // Bkash/Nagad purchase approval — create actual Purchase records now
        String desc = txn.getDescription() != null ? txn.getDescription() : "";
        String resourcesCsv = "";
        if (desc.contains("resources:")) {
            resourcesCsv = desc.substring(desc.indexOf("resources:") + "resources:".length()).trim();
        }
        if (!resourcesCsv.isEmpty()) {
            for (String idStr : resourcesCsv.split(",")) {
                try {
                    UUID resourceId = UUID.fromString(idStr.trim());
                    resourceRepo.findById(resourceId).ifPresent(res -> {
                        // Only create purchase if not already purchased
                        if (!purchaseRepo.existsByUserAndResource(user, res)) {
                            purchaseRepo.save(Purchase.builder()
                                    .user(user).resource(res)
                                    .price(res.getPrice()).priceType(res.getPriceType())
                                    .paymentMethod(txn.getPaymentMethod()).build());
                            res.setDownloads(res.getDownloads() + 1);
                            resourceRepo.save(res);
                            // Clear from cart
                            cartRepo.deleteByUserAndResource(user, res);
                            // Notify seller
                            notificationService.send(res.getUploadedBy(),
                                    Notification.NotificationType.sale, "New Purchase",
                                    user.getName() + " purchased your resource: " + res.getTitle(), null);
                        }
                    });
                } catch (IllegalArgumentException ignored) {}
            }
        }
        notificationService.send(user, Notification.NotificationType.system,
                "Purchase Approved",
                "Your " + txn.getPaymentMethod().name() + " payment has been verified. Resources are now accessible.", null);
        logService.log(Log.LogType.admin_action, "PURCHASE_APPROVED",
                "Admin approved " + txn.getPaymentMethod().name() + " purchase for " + user.getName(),
                adminId, null, user.getId(), user.getName(),
                Map.of("amount", txn.getAmount().toString()));
    }
}

    /** Admin rejects a top-up transaction */
    @Transactional
    public void rejectTransaction(UUID txnId, UUID adminId) {
        Transaction txn = txnRepo.findById(txnId)
                .orElseThrow(() -> new AppException("Transaction not found"));
        if (txn.getStatus() != Transaction.TxnStatus.pending) throw new AppException("Already processed");

        txn.setStatus(Transaction.TxnStatus.rejected);
        txnRepo.save(txn);

        User user = txn.getUser();
        if (txn.getType() == Transaction.TxnType.add) {
            user.setPendingBalance(user.getPendingBalance().subtract(txn.getAmount()));
            userRepo.save(user);
        }
        notificationService.send(user, Notification.NotificationType.system,
                "Transaction Rejected",
                "Your transaction request for BDT " + txn.getAmount() + " was rejected.", null);
    }

    /** User converts BDT to reward points instantly */
    @Transactional
    public void topupPoints(UUID userId, TransactionDto.TopupPointsRequest req) {
        User user = userRepo.findById(userId).orElseThrow(() -> new AppException("User not found"));
        if (user.getWalletBalance().compareTo(req.bdtCost()) < 0) {
            throw new AppException("Insufficient BDT balance");
        }
        user.setWalletBalance(user.getWalletBalance().subtract(req.bdtCost()));
        user.setRewardPoints(user.getRewardPoints() + req.points());
        userRepo.save(user);

        Transaction txn = Transaction.builder()
                .user(user)
                .type(Transaction.TxnType.topup_points)
                .amount(BigDecimal.valueOf(req.points()))
                .currency(Transaction.TxnCurrency.Points)
                .description("Topped up " + req.points() + " points for ৳" + req.bdtCost())
                .status(Transaction.TxnStatus.approved)
                .pointsTopupRate(req.bdtCost())
                .build();
        txnRepo.save(txn);
    }

    /** User requests a withdrawal */
    @Transactional
    public void requestWithdrawal(UUID userId, WalletDto.WithdrawRequest req) {
        User user = userRepo.findById(userId).orElseThrow(() -> new AppException("User not found"));
        if (user.getWalletBalance().compareTo(req.amount()) < 0) {
            throw new AppException("Insufficient balance");
        }
        Transaction.PaymentMethod pm;
        try { pm = Transaction.PaymentMethod.valueOf(req.method()); }
        catch (Exception e) { throw new AppException("Invalid method"); }

        // Deduct balance immediately (refunded if rejected)
        user.setWalletBalance(user.getWalletBalance().subtract(req.amount()));
        userRepo.save(user);

        Withdrawal w = Withdrawal.builder()
                .user(user).amount(req.amount()).method(pm)
                .accountNumber(req.accountNumber()).status(Withdrawal.WithdrawalStatus.pending)
                .build();
        withdrawalRepo.save(w);

        logService.log(Log.LogType.transaction, "WITHDRAWAL_REQUESTED",
                "User requested withdrawal of ৳" + req.amount() + " via " + pm.name(),
                userId, user.getName(), null, null,
                Map.of("amount", req.amount().toString(), "method", pm.name()));
    }

    /** Admin approves withdrawal */
    @Transactional
    public void approveWithdrawal(UUID withdrawalId, UUID adminId) {
        Withdrawal w = withdrawalRepo.findById(withdrawalId)
                .orElseThrow(() -> new AppException("Withdrawal not found"));
        if (w.getStatus() != Withdrawal.WithdrawalStatus.pending) throw new AppException("Already processed");
        w.setStatus(Withdrawal.WithdrawalStatus.completed);
        w.setCompletedAt(Instant.now());
        withdrawalRepo.save(w);
        notificationService.send(w.getUser(), Notification.NotificationType.system,
                "Withdrawal Approved",
                "Your BDT " + w.getAmount() + " withdrawal via " + w.getMethod().name() + " has been processed.", null);
        logService.log(Log.LogType.admin_action, "WITHDRAWAL_APPROVED",
                "Admin approved withdrawal of ৳" + w.getAmount(), adminId, null,
                w.getUser().getId(), w.getUser().getName(), null);
    }

    /** Admin rejects withdrawal — refunds the user */
    @Transactional
    public void rejectWithdrawal(UUID withdrawalId, UUID adminId) {
        Withdrawal w = withdrawalRepo.findById(withdrawalId)
                .orElseThrow(() -> new AppException("Withdrawal not found"));
        if (w.getStatus() != Withdrawal.WithdrawalStatus.pending) throw new AppException("Already processed");
        w.setStatus(Withdrawal.WithdrawalStatus.rejected);
        withdrawalRepo.save(w);

        User user = w.getUser();
        user.setWalletBalance(user.getWalletBalance().add(w.getAmount()));
        userRepo.save(user);
        notificationService.send(user, Notification.NotificationType.system,
                "Withdrawal Rejected",
                "Your BDT " + w.getAmount() + " withdrawal was rejected. Amount refunded.", null);
    }

    /** User cancels their own pending withdrawal */
    @Transactional
    public void cancelWithdrawal(UUID withdrawalId, UUID userId) {
        Withdrawal w = withdrawalRepo.findById(withdrawalId)
                .orElseThrow(() -> new AppException("Withdrawal not found"));
        if (!w.getUser().getId().equals(userId)) throw new AppException("Not your withdrawal");
        if (w.getStatus() != Withdrawal.WithdrawalStatus.pending) throw new AppException("Cannot cancel");

        w.setStatus(Withdrawal.WithdrawalStatus.rejected);
        withdrawalRepo.save(w);
        User user = w.getUser();
        user.setWalletBalance(user.getWalletBalance().add(w.getAmount()));
        userRepo.save(user);
    }

    public List<Transaction> getMyTransactions(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return txnRepo.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Withdrawal> getMyWithdrawals(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return withdrawalRepo.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Transaction> getAllPendingTransactions() {
        return txnRepo.findByStatusOrderByCreatedAtDesc(Transaction.TxnStatus.pending);
    }

    public List<Withdrawal> getAllPendingWithdrawals() {
        return withdrawalRepo.findByStatusOrderByCreatedAtDesc(Withdrawal.WithdrawalStatus.pending);
    }
}
