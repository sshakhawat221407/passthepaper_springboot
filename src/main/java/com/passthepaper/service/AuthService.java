package com.passthepaper.service;

import com.passthepaper.dto.*;
import com.passthepaper.entity.*;
import com.passthepaper.exception.AppException;
import com.passthepaper.repository.UserRepository;
import com.passthepaper.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final LogService logService;

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw new AppException("Email already registered");
        }
        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setName(req.name());
        user.setUniversity(req.university());
        user.setStudentId(req.studentId());
        user.setIsVerified(false);
        user.setIsAdmin(false);
        user.setIsBanned(false);
        user.setWalletBalance(BigDecimal.ZERO);
        user.setPendingBalance(BigDecimal.ZERO);
        user.setRewardPoints(0);
        user.setMembershipType(User.MembershipPlan.free);
        user.setIdCardStatus(User.IdCardStatus.none);
        user.setCanUpload(true);
        user.setCanPurchase(true);
        user.setCanComment(true);
        user.setSellerRating(BigDecimal.ZERO);
        user.setTotalRatings(0);
        userRepo.save(user);
        logService.log(Log.LogType.user_action, "USER_REGISTERED",
                "New user registered: " + user.getName() + " (" + user.getEmail() + ") from " + user.getUniversity(),
                user.getId(), user.getName(), null, null, null);
        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), false);
        return new AuthDto.AuthResponse(token, UserDto.Response.from(user));
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new AppException("Invalid email or password"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new AppException("Invalid email or password");
        }
        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new AppException("Account is banned: " + user.getBanReason());
        }
        logService.log(Log.LogType.user_action, "USER_LOGIN",
                "User logged in: " + user.getName() + " (" + user.getEmail() + ")",
                user.getId(), user.getName(), null, null, null);
        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), user.getIsAdmin());
        return new AuthDto.AuthResponse(token, UserDto.Response.from(user));
    }
}
