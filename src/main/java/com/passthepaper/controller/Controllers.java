package com.passthepaper.controller;
import com.passthepaper.dto.TransactionDto;
import com.passthepaper.dto.*;
import com.passthepaper.dto.AppealDto;
import com.passthepaper.entity.*;
import com.passthepaper.exception.AppException;
import com.passthepaper.repository.*;
import com.passthepaper.security.JwtUtils;
import com.passthepaper.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

// ─────────────────────────────────────────────────────
//  AUTH CONTROLLER  /auth/**
// ─────────────────────────────────────────────────────
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> register(
            @Valid @RequestBody AuthDto.RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.register(req)));
    }

    // Single /auth/login endpoint — works for both admin and student
    // Frontend checks isAdmin flag on the returned user object
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
    }
}

// ─────────────────────────────────────────────────────
//  USER CONTROLLER  /users/**
// ─────────────────────────────────────────────────────
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
class UserController {

    private final UserService userService;
    private final UserRepository userRepo;
    private final JwtUtils jwtUtils;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;

    private UUID currentUserId(UserDetails ud) {
        return userRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new AppException("User not found")).getId();
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.Response>> me(@AuthenticationPrincipal UserDetails ud) {
        User user = userService.getById(currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok(UserDto.Response.from(user)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.Response>> updateProfile(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody UserDto.UpdateProfileRequest req) {
        userService.updateProfile(currentUserId(ud), req);
        User updated = userService.getById(currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok(UserDto.Response.from(updated)));
    }

    // FIX: frontend sends POST /users/me/change-password (not /me/password)
    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody UserDto.ChangePasswordRequest req) {
        userService.changePassword(currentUserId(ud), req.currentPassword(), req.newPassword(), encoder);
        return ResponseEntity.ok(ApiResponse.ok("Password changed", null));
    }

    @PostMapping("/me/id-card")
    public ResponseEntity<ApiResponse<String>> uploadIdCard(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String, String> body) {
        userService.uploadIdCard(currentUserId(ud), body.get("imageBase64"));
        return ResponseEntity.ok(ApiResponse.ok("ID card submitted for review", null));
    }
    @DeleteMapping("/me")
public ResponseEntity<ApiResponse<String>> deleteAccount(
        @AuthenticationPrincipal UserDetails ud,
        @RequestBody Map<String, String> body) {
    userService.deleteAccount(currentUserId(ud), body.get("password"), encoder);
    return ResponseEntity.ok(ApiResponse.ok("Account deleted successfully", null));
}
}

// ─────────────────────────────────────────────────────
//  RESOURCE CONTROLLER  /resources/**
// ─────────────────────────────────────────────────────
@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
class ResourceController {

    private final ResourceService resourceService;
    private final UserRepository userRepo;
    private static final String UPLOAD_DIR = "uploads/";

    private UUID currentUserId(UserDetails ud) {
        return userRepo.findByEmail(ud.getUsername()).orElseThrow().getId();
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ResourceDto.Response>>> featured() {
        return ResponseEntity.ok(ApiResponse.ok(resourceService.getFeatured()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResourceDto.Response>>> browse(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(resourceService.browse(category, search, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceDto.Response>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(resourceService.getById(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ResourceDto.Response>> upload(
            @AuthenticationPrincipal UserDetails ud,
            @RequestPart("data") @Valid ResourceDto.CreateRequest req,
            @RequestPart("file") MultipartFile file) throws IOException {

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadPath);
        Files.copy(file.getInputStream(), uploadPath.resolve(filename));
        String fileUrl = "/files/" + filename;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(resourceService.upload(currentUserId(ud), req, fileUrl)));
    }

    @GetMapping("/my-uploads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ResourceDto.Response>>> myUploads(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(resourceService.getMyUploads(currentUserId(ud))));
    }
}

// ─────────────────────────────────────────────────────
//  CART & PURCHASE CONTROLLER
// ─────────────────────────────────────────────────────
@RestController
@RequiredArgsConstructor
class CartController {

    private final PurchaseService purchaseService;
    private final UserRepository userRepo;

    private UUID currentUserId(UserDetails ud) {
        return userRepo.findByEmail(ud.getUsername()).orElseThrow().getId();
    }

    @GetMapping("/cart")
    public ResponseEntity<ApiResponse<List<ResourceDto.Response>>> getCart(
            @AuthenticationPrincipal UserDetails ud) {
        List<Resource> items = purchaseService.getCart(currentUserId(ud));
        List<ResourceDto.Response> dtos = items.stream().map(ResourceDto.Response::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    // FIX: frontend sends POST /cart/{resourceId} (path param, no body)
    @PostMapping("/cart/{resourceId}")
    public ResponseEntity<ApiResponse<String>> addToCart(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID resourceId) {
        purchaseService.addToCart(currentUserId(ud), resourceId);
        return ResponseEntity.ok(ApiResponse.ok("Added to cart", null));
    }

    @DeleteMapping("/cart/{resourceId}")
    public ResponseEntity<ApiResponse<String>> removeFromCart(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID resourceId) {
        purchaseService.removeFromCart(currentUserId(ud), resourceId);
        return ResponseEntity.ok(ApiResponse.ok("Removed from cart", null));
    }

    // FIX: frontend checkout sends { paymentMethod, useRewardPoints, resourceIds[] }
    // If resourceIds is empty/null, use entire cart
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<String>> checkout(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody PurchaseDto.CheckoutRequest req) {
        purchaseService.checkout(currentUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.ok("Purchase successful", null));
    }

    @GetMapping("/purchases")
    public ResponseEntity<ApiResponse<List<PurchaseDto.Response>>> myPurchases(
            @AuthenticationPrincipal UserDetails ud) {
        List<Purchase> purchases = purchaseService.getMyPurchases(currentUserId(ud));
         List<PurchaseDto.Response> dtos = purchases.stream().map(p ->
                new PurchaseDto.Response(
                        p.getId(),
                        p.getResource().getId(),
                        p.getResource().getTitle(),
                        p.getPrice(),
                        p.getPriceType().name(),
                        p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null,
                        p.getFeedback(),
                        p.getRating(),
                        p.getPurchasedAt()
                )
        ).toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    // FIX: POST /purchases/rate with body { purchaseId, rating, feedback }
    @PostMapping("/purchases/rate")
    public ResponseEntity<ApiResponse<String>> rate(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody PurchaseDto.RatingRequest req) {
        purchaseService.submitRating(currentUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.ok("Rating submitted", null));
    }
}

// ─────────────────────────────────────────────────────
//  WALLET CONTROLLER  /wallet/**
// ─────────────────────────────────────────────────────
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepo;

    private UUID currentUserId(UserDetails ud) {
        return userRepo.findByEmail(ud.getUsername()).orElseThrow().getId();
    }

    // FIX: POST /wallet/topup
    @PostMapping("/topup")
    public ResponseEntity<ApiResponse<String>> requestTopup(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody TransactionDto.AddFundsRequest req) {
        walletService.requestTopup(currentUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.ok("Top-up request submitted", null));
    }

    // FIX: POST /wallet/topup-points
    @PostMapping("/topup-points")
    public ResponseEntity<ApiResponse<String>> topupPoints(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody TransactionDto.TopupPointsRequest req) {
        walletService.topupPoints(currentUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.ok("Points added", null));
    }

    // FIX: POST /wallet/withdraw
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<String>> withdraw(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody WalletDto.WithdrawRequest req) {
        walletService.requestWithdrawal(currentUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal request submitted", null));
    }

    // FIX: DELETE /wallet/withdraw/{id} (frontend uses DELETE to cancel)
    @DeleteMapping("/withdraw/{id}")
    public ResponseEntity<ApiResponse<String>> cancelWithdraw(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID id) {
        walletService.cancelWithdrawal(id, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal cancelled", null));
    }

    // FIX: GET /wallet/transactions
    @GetMapping("/transactions")
   public ResponseEntity<ApiResponse<List<TransactionDto.Response>>> getMyTransactions(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getMyTransactions(currentUserId(ud))));
    }

    // FIX: GET /wallet/withdrawals
    @GetMapping("/withdrawals")
    public ResponseEntity<ApiResponse<List<Withdrawal>>> myWithdrawals(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getMyWithdrawals(currentUserId(ud))));
    }
}

// ─────────────────────────────────────────────────────
//  NOTIFICATION CONTROLLER  /notifications/**
// ─────────────────────────────────────────────────────
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
class NotificationController {

    private final NotificationService notifService;
    private final UserRepository userRepo;

    private UUID currentUserId(UserDetails ud) {
        return userRepo.findByEmail(ud.getUsername()).orElseThrow().getId();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> list(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(notifService.getForUser(currentUserId(ud))));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(notifService.countUnread(currentUserId(ud))));
    }

    // FIX: PATCH /notifications/{id}/read (frontend sends PATCH)
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markRead(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID id) {
        notifService.markRead(id, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Marked read", null));
    }

    // FIX: PATCH /notifications/read-all (frontend sends PATCH)
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllRead(
            @AuthenticationPrincipal UserDetails ud) {
        notifService.markAllRead(currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("All marked read", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID id) {
        notifService.delete(id, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Deleted", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> deleteAll(
            @AuthenticationPrincipal UserDetails ud) {
        notifService.deleteAll(currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("All deleted", null));
    }
}

// ─────────────────────────────────────────────────────
//  FEEDBACK CONTROLLER  /feedbacks/**
//  FIX: was missing entirely — added to support feedbackApi calls
// ─────────────────────────────────────────────────────
@RestController
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
class FeedbackController {

    private final com.passthepaper.repository.FeedbackRepository feedbackRepo;
    private final UserRepository userRepo;
    private final com.passthepaper.repository.ResourceRepository resourceRepo;

    private UUID currentUserId(UserDetails ud) {
        return userRepo.findByEmail(ud.getUsername()).orElseThrow().getId();
    }

    // GET /feedbacks — returns current user's feedbacks
    @GetMapping
    public ResponseEntity<ApiResponse<List<Feedback>>> mine(
            @AuthenticationPrincipal UserDetails ud) {
        User user = userRepo.findById(currentUserId(ud)).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok(feedbackRepo.findByUserOrderByCreatedAtDesc(user)));
    }

    // POST /feedbacks — create system or item feedback
    @PostMapping
    public ResponseEntity<ApiResponse<Feedback>> create(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody FeedbackDto.CreateRequest req) {
        User user = userRepo.findById(currentUserId(ud)).orElseThrow();
        Feedback.FeedbackType type;
        try {
            type = Feedback.FeedbackType.valueOf(req.type());
        } catch (Exception e) {
            type = Feedback.FeedbackType.system;
        }
         Feedback fb = new Feedback();
        fb.setUser(user);
        fb.setType(type);
        fb.setRating(req.rating());
        fb.setComment(req.comment());
        fb.setItemTitle(req.itemTitle());
        if (req.itemId() != null) {
            resourceRepo.findById(req.itemId()).ifPresent(fb::setItem);
        }
        Feedback saved = feedbackRepo.save(fb);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }
}

// ─────────────────────────────────────────────────────
//  APPEAL CONTROLLER  /appeals/**
// ─────────────────────────────────────────────────────
@RestController
@RequestMapping("/appeals")
@RequiredArgsConstructor
class AppealController {

    private final AppealService appealService;
    private final UserRepository userRepo;

    private UUID currentUserId(UserDetails ud) {
        return userRepo.findByEmail(ud.getUsername()).orElseThrow().getId();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppealDto.Response>> submit(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody AppealDto.CreateRequest req) {
        Appeal saved = appealService.submitAppeal(currentUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.ok(AppealDto.Response.from(saved)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<AppealDto.Response>>> myAppeals(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(
            appealService.getMyAppeals(currentUserId(ud))
                .stream().map(AppealDto.Response::from).toList()
        ));
    }

    @DeleteMapping("/{appealId}")
    public ResponseEntity<ApiResponse<String>> deleteAppeal(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID appealId) {
        appealService.deleteAppeal(appealId, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Message deleted", null));
    }
}

// ─────────────────────────────────────────────────────
//  ADMIN CONTROLLER  /admin/**
// ─────────────────────────────────────────────────────
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class AdminController {

    private final ResourceService resourceService;
    private final UserService userService;
    private final WalletService walletService;
    private final AppealService appealService;
    private final LogService logService;
    private final UserRepository userRepo;

    private UUID currentUserId(UserDetails ud) {
        return userRepo.findByEmail(ud.getUsername()).orElseThrow().getId();
    }

    // ─── Users ────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto.Response>>> allUsers() {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.getAllStudents().stream().map(UserDto.Response::from).toList()));
    }

    // FIX: GET /admin/users/pending-id-cards
    @GetMapping("/users/pending-id-cards")
    public ResponseEntity<ApiResponse<List<UserDto.Response>>> pendingIdCards() {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.getPendingIdCards().stream().map(UserDto.Response::from).toList()));
    }

    // FIX: POST /admin/users/{userId}/id-card with { approve: bool }
    // This is now the unified endpoint for both approveIdCard and rejectIdCard
    @PostMapping("/users/{userId}/id-card")
    public ResponseEntity<ApiResponse<String>> reviewIdCard(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID userId,
            @RequestBody AdminDto.ApproveIdCardRequest req) {
        userService.approveIdCard(userId, req.approve(), currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok(req.approve() ? "Approved" : "Rejected", null));
    }

    // FIX: POST /admin/users/{userId}/ban (frontend expects POST)
    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<ApiResponse<String>> ban(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID userId,
            @RequestBody AdminDto.BanRequest req) {
        userService.banUser(userId, req.reason(), currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("User banned", null));
    }

    // FIX: POST /admin/users/{userId}/unban (frontend expects POST)
    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<ApiResponse<String>> unban(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID userId) {
        userService.unbanUser(userId, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("User unbanned", null));
    }

    // FIX: POST /admin/users/{userId}/restrictions (frontend expects POST)
    @PostMapping("/users/{userId}/restrictions")
    public ResponseEntity<ApiResponse<String>> setRestrictions(
            @PathVariable UUID userId,
            @RequestBody UserDto.RestrictionsRequest req) {
        userService.setRestrictions(userId, req);
        return ResponseEntity.ok(ApiResponse.ok("Restrictions updated", null));
    }

    // ─── Resources ────────────────────────────────────────

    @GetMapping("/resources/pending")
    public ResponseEntity<ApiResponse<List<ResourceDto.Response>>> pendingResources() {
        return ResponseEntity.ok(ApiResponse.ok(resourceService.getPendingResources()));
    }

    // FIX: POST /admin/resources/{resourceId}/approve (frontend expects POST)
    @PostMapping("/resources/{resourceId}/approve")
    public ResponseEntity<ApiResponse<String>> approveResource(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID resourceId,
            @RequestBody Map<String, Boolean> body) {
        resourceService.approveResource(resourceId, body.get("approve"), currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Done", null));
    }

    @DeleteMapping("/resources/{resourceId}")
    public ResponseEntity<ApiResponse<String>> deleteResource(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID resourceId) {
        resourceService.deleteResource(resourceId, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Deleted", null));
    }

    // ─── Transactions ─────────────────────────────────────

    // FIX: GET /admin/transactions/pending (frontend requests this path)
    @GetMapping("/transactions/pending")
    public ResponseEntity<ApiResponse<List<TransactionDto.Response>>> pendingTransactions() {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getAllPendingTransactions()));
    }

    // FIX: POST /admin/transactions/{txnId}/approve (frontend expects POST)
    @PostMapping("/transactions/{txnId}/approve")
    public ResponseEntity<ApiResponse<String>> approveTxn(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID txnId) {
        walletService.approveTransaction(txnId, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Transaction approved", null));
    }

    // FIX: POST /admin/transactions/{txnId}/reject (frontend expects POST)
    @PostMapping("/transactions/{txnId}/reject")
    public ResponseEntity<ApiResponse<String>> rejectTxn(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID txnId) {
        walletService.rejectTransaction(txnId, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Transaction rejected", null));
    }

    // ─── Withdrawals ──────────────────────────────────────

    @GetMapping("/withdrawals/pending")
    public ResponseEntity<ApiResponse<List<Withdrawal>>> pendingWithdrawals() {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getAllPendingWithdrawals()));
    }

    // FIX: POST /admin/withdrawals/{id}/approve (frontend expects POST)
    @PostMapping("/withdrawals/{id}/approve")
    public ResponseEntity<ApiResponse<String>> approveWithdrawal(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID id) {
        walletService.approveWithdrawal(id, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal approved", null));
    }

    // FIX: POST /admin/withdrawals/{id}/reject (frontend expects POST)
    @PostMapping("/withdrawals/{id}/reject")
    public ResponseEntity<ApiResponse<String>> rejectWithdrawal(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID id) {
        walletService.rejectWithdrawal(id, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal rejected", null));
    }

    // ─── Appeals ──────────────────────────────────────────

    // FIX: GET /admin/appeals/pending (frontend requests this path)
   @GetMapping("/appeals/pending")
public ResponseEntity<ApiResponse<List<AppealDto.Response>>> pendingAppeals() {
    return ResponseEntity.ok(ApiResponse.ok(
        appealService.getAllPending()
            .stream().map(AppealDto.Response::from).toList()
    ));
}

    // FIX: POST /admin/appeals/{appealId}/review (frontend expects POST)
    @PostMapping("/appeals/{appealId}/review")
    public ResponseEntity<ApiResponse<String>> reviewAppeal(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID appealId,
            @RequestBody AppealDto.ReviewRequest req) {
        appealService.reviewAppeal(appealId, req, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Appeal reviewed", null));
    }

// ─── Logs ─────────────────────────────────────────────

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<Log>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size) {
        return ResponseEntity.ok(ApiResponse.ok(logService.getLogs(page, size)));
    }
}
