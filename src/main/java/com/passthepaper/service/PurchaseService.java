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

    // ─── Cart ───────────────────────────────────────────

    public void addToCart(UUID userId, UUID resourceId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new AppException("User not found"));
        Resource res = resourceRepo.findById(resourceId).orElseThrow(() -> new AppException("Resource not found"));
        if (res.getStatus() != Resource.ResourceStatus.approved) throw new AppException("Resource not available");
        if (purchaseRepo.existsByUserAndResource(user, res)) throw new AppException("Already purchased");
        if (!cartRepo.existsByUserAndResource(user, res)) {
            cartRepo.save(CartItem.builder().user(user).resource(res).build());
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

    // ─── Checkout ───────────────────────────────────────

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

        // Calculate totals
        BigDecimal totalBdt = resources.stream()
                .filter(r -> r.getPriceType() == Resource.PriceType.money)
                .map(Resource::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalPoints = resources.stream()
                .filter(r -> r.getPriceType() == Resource.PriceType.points)
                .mapToInt(r -> r.getPrice().intValue()).sum();

        // Deduct from wallet/points if paying via wallet
        if (pm == Transaction.PaymentMethod.Wallet) {
            if (user.getWalletBalance().compareTo(totalBdt) < 0)
                throw new AppException("Insufficient wallet balance");
            if (user.getRewardPoints() < totalPoints)
                throw new AppException("Insufficient reward points");
            user.setWalletBalance(user.getWalletBalance().subtract(totalBdt));
            user.setRewardPoints(user.getRewardPoints() - totalPoints);
            userRepo.save(user);
        }

        for (Resource res : resources) {
            Purchase purchase = Purchase.builder()
                    .user(user).resource(res)
                    .price(res.getPrice()).priceType(res.getPriceType())
                    .paymentMethod(pm).build();
            purchaseRepo.save(purchase);

            // Increment download count
            res.setDownloads(res.getDownloads() + 1);
            resourceRepo.save(res);

            // Record transaction
            txnRepo.save(Transaction.builder()
                    .user(user).type(Transaction.TxnType.purchase)
                    .amount(res.getPrice())
                    .currency(res.getPriceType() == Resource.PriceType.money
                            ? Transaction.TxnCurrency.BDT : Transaction.TxnCurrency.Points)
                    .description("Purchase of " + res.getTitle())
                    .status(Transaction.TxnStatus.approved)
                    .paymentMethod(pm).build());

            // Notify seller
            notificationService.send(res.getUploadedBy(), Notification.NotificationType.sale,
                    "New Purchase",
                    user.getName() + " purchased your resource: " + res.getTitle(), null);
        }

        // Clear purchased items from cart
        req.resourceIds().forEach(rid ->
                resourceRepo.findById(rid).ifPresent(r -> cartRepo.deleteByUserAndResource(user, r)));
    }

    // ─── Ratings ────────────────────────────────────────

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
