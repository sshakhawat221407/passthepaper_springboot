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
    private final PurchaseRepository purchaseRepo;
    private final CartItemRepository cartRepo;
    private final ResourceRepository resourceRepo;

    @Transactional
    public void requestTopup(UUID userId, TransactionDto.AddFundsRequest req) {
        User user = userRepo.findById(userId).orElseThrow(() -> new AppException("User not found"));
        Transaction.PaymentMethod pm;
        try { pm = Transaction.PaymentMethod.valueOf(req.paymentMethod()); }
        catch (Exception e) { throw new AppException("Invalid payment method"); }

        Transaction txn = new Transaction();
        txn.setUser(user);
        txn.setType(Transaction.TxnType.add);
        txn.setAmount(req.amount());
        txn.setCurrency(Transaction.TxnCurrency.BDT);
        txn.setDescription("Wallet top-up via " + pm.name());
        txn.setPaymentMethod(pm);
        txn.setStatus(Transaction.TxnStatus.pending);
        txn.setPaymentPhone(req.paymentPhone());
        txn.setTransactionNumber(req.transactionNumber());
        txnRepo.save(txn);

        user.setPendingBalance(user.getPendingBalance().add(req.amount()));
        userRepo.save(user);
    }

    @Transactional
    public void approveTransaction(UUID txnId, UUID adminId) {
        Transaction txn = txnRepo.findById(txnId)
                .orElseThrow(() -> new AppException("Transaction not found"));
        if (txn.getStatus() != Transaction.TxnStatus.pending) throw new AppException("Already processed");

        txn.setStatus(Transaction.TxnStatus.approved);
        txnRepo.save(txn);

        User user = txn.getUser();

        if (txn.getType() == Transaction.TxnType.add) {
            user.setWalletBalance(user.getWalletBalance().add(txn.getAmount()));
            user.setPendingBalance(user.getPendingBalance().subtract(txn.getAmount()));
            userRepo.save(user);
            notificationService.send(user, Notification.NotificationType.system,
                    "Wallet Top-up Approved",
                    "Your BDT " + txn.getAmount() + " wallet top-up has been approved.", null);
            logService.log(Log.LogType.admin_action, "TOPUP_APPROVED",
                    "Admin approved top-up of " + txn.getAmount() + " for " + user.getName(),
                    adminId, null, user.getId(), user.getName(),
                    Map.of("amount", txn.getAmount().toString()));

        } else if (txn.getType() == Transaction.TxnType.purchase) {
            String desc = txn.getDescription() != null ? txn.getDescription() : "";
            if (desc.contains("resources:")) {
                String csv = desc.substring(desc.indexOf("resources:") + "resources:".length()).trim();
                for (String idStr : csv.split(",")) {
                    try {
                        UUID rid = UUID.fromString(idStr.trim());
                        resourceRepo.findById(rid).ifPresent(res -> {
                            if (!purchaseRepo.existsByUserAndResource(user, res)) {
                                Purchase purchase = new Purchase();
                                purchase.setUser(user);
                                purchase.setResource(res);
                                purchase.setPrice(res.getPrice());
                                purchase.setPriceType(res.getPriceType());
                                purchase.setPaymentMethod(txn.getPaymentMethod());
                                purchaseRepo.save(purchase);
                                res.setDownloads(res.getDownloads() + 1);
                                resourceRepo.save(res);
                                cartRepo.deleteByUserAndResource(user, res);
                                notificationService.send(res.getUploadedBy(),
                                        Notification.NotificationType.sale, "New Purchase",
                                        user.getName() + " purchased: " + res.getTitle(), null);
                            }
                        });
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            notificationService.send(user, Notification.NotificationType.system,
                    "Purchase Approved",
                    "Your " + txn.getPaymentMethod().name() + " payment verified. Resources are now accessible.", null);
            logService.log(Log.LogType.admin_action, "PURCHASE_APPROVED",
                    "Admin approved " + txn.getPaymentMethod().name() + " purchase for " + user.getName(),
                    adminId, null, user.getId(), user.getName(),
                    Map.of("amount", txn.getAmount().toString()));
        }
    }

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

    @Transactional
    public void topupPoints(UUID userId, TransactionDto.TopupPointsRequest req) {
        User user = userRepo.findById(userId).orElseThrow(() -> new AppException("User not found"));
        if (user.getWalletBalance().compareTo(req.bdtCost()) < 0) {
            throw new AppException("Insufficient BDT balance");
        }
        user.setWalletBalance(user.getWalletBalance().subtract(req.bdtCost()));
        user.setRewardPoints(user.getRewardPoints() + req.points());
        userRepo.save(user);

        Transaction txn = new Transaction();
        txn.setUser(user);
        txn.setType(Transaction.TxnType.topup_points);
        txn.setAmount(BigDecimal.valueOf(req.points()));
        txn.setCurrency(Transaction.TxnCurrency.Points);
        txn.setDescription("Topped up " + req.points() + " points for " + req.bdtCost());
        txn.setStatus(Transaction.TxnStatus.approved);
        txn.setPointsTopupRate(req.bdtCost());
        txnRepo.save(txn);
    }

    @Transactional
    public void requestWithdrawal(UUID userId, WalletDto.WithdrawRequest req) {
        User user = userRepo.findById(userId).orElseThrow(() -> new AppException("User not found"));
        if (user.getWalletBalance().compareTo(req.amount()) < 0) {
            throw new AppException("Insufficient balance");
        }
        Transaction.PaymentMethod pm;
        try { pm = Transaction.PaymentMethod.valueOf(req.method()); }
        catch (Exception e) { throw new AppException("Invalid method"); }

        user.setWalletBalance(user.getWalletBalance().subtract(req.amount()));
        userRepo.save(user);

        Withdrawal w = new Withdrawal();
        w.setUser(user);
        w.setAmount(req.amount());
        w.setMethod(pm);
        w.setAccountNumber(req.accountNumber());
        w.setStatus(Withdrawal.WithdrawalStatus.pending);
        withdrawalRepo.save(w);

        logService.log(Log.LogType.transaction, "WITHDRAWAL_REQUESTED",
                "User requested withdrawal of " + req.amount() + " via " + pm.name(),
                userId, user.getName(), null, null,
                Map.of("amount", req.amount().toString(), "method", pm.name()));
    }

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
                "Admin approved withdrawal of " + w.getAmount(), adminId, null,
                w.getUser().getId(), w.getUser().getName(), null);
    }

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

    @Transactional(readOnly = true)
    public List<TransactionDto.Response> getMyTransactions(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return txnRepo.findByUserOrderByCreatedAtDesc(user)
                .stream().map(TransactionDto.Response::from).toList();
    }

    public List<Withdrawal> getMyWithdrawals(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return withdrawalRepo.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto.Response> getAllPendingTransactions() {
        return txnRepo.findByStatusOrderByCreatedAtDesc(Transaction.TxnStatus.pending)
                .stream().map(TransactionDto.Response::from).toList();
    }

    public List<Withdrawal> getAllPendingWithdrawals() {
        return withdrawalRepo.findByStatusOrderByCreatedAtDesc(Withdrawal.WithdrawalStatus.pending);
    }
}
