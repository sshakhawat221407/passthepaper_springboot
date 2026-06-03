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
        User user = User.builder()
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .name(req.name())
                .university(req.university())
                .studentId(req.studentId())
                .isVerified(false)
                .isAdmin(false)
                .walletBalance(java.math.BigDecimal.ZERO)
                .rewardPoints(0)
                .membershipType(User.MembershipPlan.free)
                .idCardStatus(User.IdCardStatus.none)
                .build();
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
