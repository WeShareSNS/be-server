package com.weshare.api.v1.service.auth;

import com.weshare.api.v1.controller.auth.dto.SignupRequest;
import com.weshare.api.v1.controller.auth.dto.UserLoginDto;
import com.weshare.api.v1.domain.user.Role;
import com.weshare.api.v1.domain.user.Social;
import com.weshare.api.v1.domain.user.User;
import com.weshare.api.v1.domain.user.exception.EmailDuplicateException;
import com.weshare.api.v1.domain.user.exception.UsernameDuplicateException;
import com.weshare.api.v1.repository.user.UserRepository;
import com.weshare.api.v1.token.RefreshToken;
import com.weshare.api.v1.token.RefreshTokenRepository;
import com.weshare.api.v1.token.TokenType;
import com.weshare.api.v1.token.exception.InvalidTokenException;
import com.weshare.api.v1.token.exception.TokenNotFoundException;
import com.weshare.api.v1.token.jwt.JwtService;
import com.weshare.api.v1.token.logout.LogoutAccessTokenFromRedis;
import com.weshare.api.v1.token.logout.LogoutAccessTokenRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {
    private static final String DEFAULT_PROFILE_IMG_URL = "https://static.vecteezy.com/system/resources/thumbnails/020/765/399/small/default-profile-account-unknown-icon-black-silhouette-free-vector.jpg";

    private final UserRepository repository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LogoutAccessTokenRedisRepository logoutTokenRedisRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public User join(SignupRequest request) {
        checkDuplicateEmailForSignup(request.email());
        checkDuplicateNameForSignup(request.userName());
        return repository.save(createUser(request));
    }

    private boolean isDuplicateEmail(String email) throws EmailDuplicateException {
        return repository.findByEmail(email).isPresent();
    }

    // 회원 가입시 사용자는 사용할 수 있는 이메일인지 확인할 수 있다.
    public void checkDuplicateEmailForSignup(String email) {
        if (isDuplicateEmail(email)) {
            throw new EmailDuplicateException(email + "은 가입된 이메일 입니다.");
        }
    }

    public void checkDuplicateNameForSignup(String name) {
        if (isDuplicateName(name)) {
            throw new UsernameDuplicateException(name + "은 가입된 닉네임 입니다.");
        }
    }

    private boolean isDuplicateName(String name) {
        return repository.findByName(name).isPresent();
    }

    private User createUser(SignupRequest request) {
        LocalDate birthDate = LocalDate.parse(request.birthDate());
        if (isBirthDateInFuture(birthDate)) {
            throw new IllegalArgumentException("생년월일은 미래 날짜를 입력하실 수 없습니다.");
        }

        return User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.userName())
                .birthDate(birthDate)
                .profileImg(DEFAULT_PROFILE_IMG_URL)
                .role(Role.USER)
                .social(Social.DEFAULT)
                .build();
    }

    private boolean isBirthDateInFuture(LocalDate birthDate) {
        return LocalDate.now().isBefore(birthDate);
    }

    private void reissueRefreshTokenByUser(User user, String refreshToken) {
        RefreshToken token = refreshTokenRepository.findTokenByUser(user)
                .orElse(createRefreshTokenWithUser(user, refreshToken));

        token.updateToken(refreshToken);
        refreshTokenRepository.save(token);
    }

    private RefreshToken createRefreshTokenWithUser(User user, String refreshToken) {
        return RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .tokenType(TokenType.BEARER)
                .build();
    }

    public UserLoginDto reissueToken(Optional<String> token, Date issuedAt) {
        if (token.isEmpty()) {
            throw new InvalidTokenException("토큰이 존재하지 않습니다.");
        }
        String refreshToken = token.get();
        User user = findUserByValidRefreshToken(refreshToken);

        String accessToken = jwtService.generateAccessToken(user, issuedAt);
        String reissueToken = jwtService.generateRefreshToken(user, issuedAt);
        reissueRefreshTokenByUser(user, reissueToken);
        return new UserLoginDto(accessToken, reissueToken, user.getName());
    }

    private User findUserByValidRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenWithUser(token)
                .orElseThrow(() -> {
                    throw new TokenNotFoundException("Refresh Token이 존재하지 않습니다.");
                });
        User user = refreshToken.getUser();
        if (user == null) {
            throw new UsernameNotFoundException("token의 사용자가 존재하지 않습니다.");
        }

        if (!jwtService.isTokenValid(token, user)) {
            throw new InvalidTokenException("토큰이 유효하지 않습니다.");
        }
        return user;
    }

    public void logout(final String jwt) {
        String userEmail = jwtService.extractEmail(jwt);
        saveLogoutToken(jwt);
        refreshTokenRepository.findTokenByUserEmail(userEmail)
                .ifPresent(this::deleteRefreshToken);
    }

    private void saveLogoutToken(String accessToken) {
        long expireTimeFromToken = jwtService.getExpireTimeFromToken(accessToken);

        LogoutAccessTokenFromRedis logoutToken = LogoutAccessTokenFromRedis.builder()
                .id(accessToken)
                .expiration(expireTimeFromToken)
                .build();

        logoutTokenRedisRepository.save(logoutToken);
    }

    private void deleteRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }
}
