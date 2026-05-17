package com.passthepaper.controller;

import com.passthepaper.dto.*;
import com.passthepaper.entity.*;
import com.passthepaper.exception.AppException;
import com.passthepaper.repository.UserRepository;
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
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody UserDto.UpdateProfileRequest req) {
        userService.updateProfile(currentUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", null));
    }

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

        // Save file to local disk (swap for S3 / cloud storage in production)
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
//  CART & PURCHASE CONTROLLER  /cart  /checkout
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
    public ResponseEntity<ApiResponse<List<ResourceDto.Response>>> getCart(@AuthenticationPrincipal UserDetails ud) {
        List<Resource> items = purchaseService.getCart(currentUserId(ud));
        List<ResourceDto.Response> dtos = items.stream().map(ResourceDto.Response::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @PostMapping("/cart/{resourceId}")
    public ResponseEntity<ApiResponse<String>> addToCart(
            @AuthenticationPrincipal UserDetails ud, @PathVariable UUID resourceId) {
        purchaseService.addToCart(currentUserId(ud), resourceId);
        return ResponseEntity.ok(ApiResponse.ok("Added to cart", null));
    }

    @DeleteMapping("/cart/{resourceId}")
    public ResponseEntity<ApiResponse<String>> removeFromCart(
            @AuthenticationPrincipal UserDetails ud, @PathVariable UUID resourceId) {
        purchaseService.removeFromCart(currentUserId(ud), resourceId);
        return ResponseEntity.ok(ApiResponse.ok("Removed from cart", null));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<String>> checkout(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody PurchaseDto.CheckoutRequest req) {
        purchaseService.checkout(currentUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.ok("Purchase successful", null));
    }

    @GetMapping("/purchases")
    public ResponseEntity<ApiResponse<List<Purchase>>> myPurchases(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(purchaseService.getMyPurchases(currentUserId(ud))));
    }

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

    @PostMapping("/topup")
    public ResponseEntity<ApiResponse<String>> requestTopup(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody TransactionDto.AddFundsRequest req) {
        walletService.requestTopup(currentUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.ok("Top-up request submitted", null));
    }

    @PostMapping("/topup-points")
    public ResponseEntity<ApiResponse<String>> topupPoints(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody TransactionDto.TopupPointsRequest req) {
        walletService.topupPoints(currentUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.ok("Points added", null));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<String>> withdraw(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody WalletDto.WithdrawRequest req) {
        walletService.requestWithdrawal(currentUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal request submitted", null));
    }

    @DeleteMapping("/withdraw/{id}")
    public ResponseEntity<ApiResponse<String>> cancelWithdraw(
            @AuthenticationPrincipal UserDetails ud, @PathVariable UUID id) {
        walletService.cancelWithdrawal(id, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal cancelled", null));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<Transaction>>> myTransactions(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getMyTransactions(currentUserId(ud))));
    }

    @GetMapping("/withdrawals")
    public ResponseEntity<ApiResponse<List<Withdrawal>>> myWithdrawals(@AuthenticationPrincipal UserDetails ud) {
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
    public ResponseEntity<ApiResponse<List<Notification>>> list(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(notifService.getForUser(currentUserId(ud))));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(notifService.countUnread(currentUserId(ud))));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markRead(
            @AuthenticationPrincipal UserDetails ud, @PathVariable UUID id) {
        notifService.markRead(id, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Marked read", null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllRead(@AuthenticationPrincipal UserDetails ud) {
        notifService.markAllRead(currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("All marked read", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @AuthenticationPrincipal UserDetails ud, @PathVariable UUID id) {
        notifService.delete(id, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Deleted", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> deleteAll(@AuthenticationPrincipal UserDetails ud) {
        notifService.deleteAll(currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("All deleted", null));
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
    public ResponseEntity<ApiResponse<String>> submit(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody AppealDto.CreateRequest req) {
        appealService.submitAppeal(currentUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.ok("Appeal submitted", null));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Appeal>>> myAppeals(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(appealService.getMyAppeals(currentUserId(ud))));
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

    @GetMapping("/users/pending-id-cards")
    public ResponseEntity<ApiResponse<List<UserDto.Response>>> pendingIdCards() {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.getPendingIdCards().stream().map(UserDto.Response::from).toList()));
    }

    @PostMapping("/users/{userId}/id-card")
    public ResponseEntity<ApiResponse<String>> reviewIdCard(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID userId,
            @RequestBody AdminDto.ApproveIdCardRequest req) {
        userService.approveIdCard(userId, req.approve(), currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok(req.approve() ? "Approved" : "Rejected", null));
    }

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<ApiResponse<String>> ban(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID userId,
            @RequestBody AdminDto.BanRequest req) {
        userService.banUser(userId, req.reason(), currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("User banned", null));
    }

    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<ApiResponse<String>> unban(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID userId) {
        userService.unbanUser(userId, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("User unbanned", null));
    }

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

    @GetMapping("/transactions/pending")
    public ResponseEntity<ApiResponse<List<Transaction>>> pendingTransactions() {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getAllPendingTransactions()));
    }

    @PostMapping("/transactions/{txnId}/approve")
    public ResponseEntity<ApiResponse<String>> approveTxn(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID txnId) {
        walletService.approveTransaction(txnId, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Transaction approved", null));
    }

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

    @PostMapping("/withdrawals/{id}/approve")
    public ResponseEntity<ApiResponse<String>> approveWithdrawal(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID id) {
        walletService.approveWithdrawal(id, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal approved", null));
    }

    @PostMapping("/withdrawals/{id}/reject")
    public ResponseEntity<ApiResponse<String>> rejectWithdrawal(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable UUID id) {
        walletService.rejectWithdrawal(id, currentUserId(ud));
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal rejected", null));
    }

    // ─── Appeals ──────────────────────────────────────────

    @GetMapping("/appeals/pending")
    public ResponseEntity<ApiResponse<List<Appeal>>> pendingAppeals() {
        return ResponseEntity.ok(ApiResponse.ok(appealService.getAllPending()));
    }

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
    public ResponseEntity<ApiResponse<?>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(logService.getLogs(page, size)));
    }
}
