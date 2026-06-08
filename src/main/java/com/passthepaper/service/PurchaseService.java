package com.passthepaper.service;

import com.passthepaper.dto.PurchaseDto;
import com.passthepaper.entity.*;
import com.passthepaper.exception.AppException;
import com.passthepaper.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final UserRepository userRepo;
    private final ResourceRepository resourceRepo;
    private final CartItemRepository cartRepo;
    private final PurchaseRepository purchaseRepo;
    private final TransactionRepository txnRepo;
    private final NotificationService notificationService;
    private final LogService logService;

    public void addToCart(UUID userId, UUID resourceId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new AppException("User not found"));
        Resource res = resourceRepo.findById(resourceId).orElseThrow(() -> new AppException("Resource not found"));
        if (res.getStatus() != Resource.ResourceStatus.approved) throw new AppException("Resource not available");
        if (purchaseRepo.existsByUserAndResource(user, res)) throw new AppException("Already purchased");
        if (!cartRepo.existsByUserAndResource(user, res)) {
            CartItem item = new CartItem();
            item.setUser(user);
            item.setResource(res);
            cartRepo.save(item);
        }
    }

    public void removeFromCart(UUID userId, UUID resourceId) {
        User user = userRepo.findById(userId).orElseThrow();
        Resource res = resourceRepo.findById(resourceId).orElseThrow();
        cartRepo.deleteByUserAndResource(user, res);
    }

    public List<Resource> getCart(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return cartRepo.findByUser(user).stream()
                .map(CartItem::getResource)
                .collect(Collectors.toList());
    }

    @Transactional
    public void checkout(UUID userId, PurchaseDto.CheckoutRequest req) {
        User user = userRepo.findById(userId).orElseThrow(() -> new AppException("User not found"));
        if (!Boolean.TRUE.equals(user.getCanPurchase())) throw new AppException("Purchase permission revoked");

        Transaction.PaymentMethod pm;
        try { pm = Transaction.PaymentMethod.valueOf(req.paymentMethod()); }
        catch (Exception e) { throw new AppException("Invalid payment method"); }

        List<Resource> resources = req.resourceIds().stream()
                .map(id -> resourceRepo.findById(id).orElseThrow(() -> new AppException("Resource not found: " + id)))
                .collect(Collectors.toList());

        BigDecimal totalBdt = resources.stream()
                .filter(r -> r.getPriceType() == Resource.PriceType.money)
                .map(Resource::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalPoints = resources.stream()
                .filter(r -> r.getPriceType() == Resource.PriceType.points)
                .mapToInt(r -> r.getPrice().intValue()).sum();

        if (pm == Transaction.PaymentMethod.Bkash || pm == Transaction.PaymentMethod.Nagad) {
            String resourceIdsCsv = req.resourceIds().stream()
                    .map(UUID::toString).collect(Collectors.joining(","));
            Transaction txn = new Transaction();
            txn.setUser(user);
            txn.setType(Transaction.TxnType.purchase);
            txn.setAmount(totalBdt);
            txn.setCurrency(Transaction.TxnCurrency.BDT);
            txn.setDescription("Pending purchase via " + pm.name() + " | resources:" + resourceIdsCsv);
            txn.setStatus(Transaction.TxnStatus.pending);
            txn.setPaymentMethod(pm);
            txn.setPaymentPhone(req.paymentPhone());
            txn.setTransactionNumber(req.transactionNumber());
            txnRepo.save(txn);
            logService.log(Log.LogType.transaction, "PURCHASE_PENDING",
                    "User " + user.getName() + " submitted " + pm.name() + " payment for "
                    + resources.size() + " resource(s). TrxID: " + req.transactionNumber(),
                    user.getId(), user.getName(), null, null,
                    Map.of("method", pm.name(), "amount", totalBdt.toString(),
                           "trxId", req.transactionNumber() != null ? req.transactionNumber() : ""));
            return;
        }

        if (user.getWalletBalance().compareTo(totalBdt) < 0)
            throw new AppException("Insufficient wallet balance");
        if (user.getRewardPoints() < totalPoints)
            throw new AppException("Insufficient reward points");
        user.setWalletBalance(user.getWalletBalance().subtract(totalBdt));
        user.setRewardPoints(user.getRewardPoints() - totalPoints);
        userRepo.save(user);

        for (Resource res : resources) {
            Purchase purchase = new Purchase();
            purchase.setUser(user);
            purchase.setResource(res);
            purchase.setPrice(res.getPrice());
            purchase.setPriceType(res.getPriceType());
            purchase.setPaymentMethod(pm);
            purchaseRepo.save(purchase);

            res.setDownloads(res.getDownloads() + 1);

            // If priced in points: credit the seller immediately on each sale
            if (res.getPriceType() == Resource.PriceType.points && res.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                User seller = userRepo.findById(res.getUploadedBy().getId())
                        .orElse(res.getUploadedBy());
                seller.setRewardPoints(seller.getRewardPoints() + res.getPrice().intValue());
                userRepo.save(seller);
            }

            // If the resource has hit its max sales limit, notify the seller
            if (res.getMaxSales() != null && res.getDownloads() >= res.getMaxSales()) {
                notificationService.send(res.getUploadedBy(), Notification.NotificationType.sale,
                        "Resource Sold Out",
                        "Your resource "" + res.getTitle() + "" has reached its maximum sales limit and is now hidden from the store.",
                        null);
            }

            resourceRepo.save(res);

            Transaction txn = new Transaction();
            txn.setUser(user);
            txn.setType(Transaction.TxnType.purchase);
            txn.setAmount(res.getPrice());
            txn.setCurrency(res.getPriceType() == Resource.PriceType.money
                    ? Transaction.TxnCurrency.BDT : Transaction.TxnCurrency.Points);
            txn.setDescription("Purchase of " + res.getTitle());
            txn.setStatus(Transaction.TxnStatus.approved);
            txn.setPaymentMethod(pm);
            txnRepo.save(txn);

            notificationService.send(res.getUploadedBy(), Notification.NotificationType.sale,
                    "New Purchase", user.getName() + " purchased your resource: " + res.getTitle(), null);
        }
        req.resourceIds().forEach(rid ->
                resourceRepo.findById(rid).ifPresent(r -> cartRepo.deleteByUserAndResource(user, r)));
        logService.log(Log.LogType.transaction, "PURCHASE_COMPLETED",
                "User " + user.getName() + " purchased " + resources.size() + " resource(s) via Wallet",
                user.getId(), user.getName(), null, null,
                Map.of("resourceCount", String.valueOf(resources.size()), "totalBdt", totalBdt.toString()));
    }

    @Transactional
    public void submitRating(UUID userId, PurchaseDto.RatingRequest req) {
        Purchase p = purchaseRepo.findById(req.purchaseId())
                .orElseThrow(() -> new AppException("Purchase not found"));
        if (!p.getUser().getId().equals(userId)) throw new AppException("Not your purchase");
        if (p.getRating() != null) throw new AppException("Already rated");
        p.setRating(req.rating());
        p.setFeedback(req.feedback());
        purchaseRepo.save(p);
        updateSellerRating(p.getResource().getUploadedBy());
    }

    private void updateSellerRating(User seller) {
        Double avg = purchaseRepo.avgRatingByUploader(seller);
        Long count = purchaseRepo.countRatingsByUploader(seller);
        if (avg != null) {
            seller.setSellerRating(BigDecimal.valueOf(avg).setScale(2, java.math.RoundingMode.HALF_UP));
            seller.setTotalRatings(count.intValue());
            userRepo.save(seller);
        }
    }

    public List<Purchase> getMyPurchases(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return purchaseRepo.findByUserOrderByPurchasedAtDesc(user);
    }
}
