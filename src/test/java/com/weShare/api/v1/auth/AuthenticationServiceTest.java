package com.weShare.api.v1.auth;

import com.weShare.api.IntegrationTestSupport;
import com.weShare.api.v1.domain.user.Role;
import com.weShare.api.v1.domain.user.entity.User;
import com.weShare.api.v1.domain.user.repository.UserRepository;
import com.weShare.api.v1.token.RefreshToken;
import com.weShare.api.v1.token.RefreshTokenRepository;
import com.weShare.api.v1.token.TokenType;
import com.weShare.api.v1.token.jwt.JwtService;
import com.weShare.api.v1.token.jwt.logout.LogoutAccessTokenFromRedis;
import com.weShare.api.v1.token.jwt.logout.LogoutAccessTokenRedisRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


// jwtService를 수정하면 모든 테스트가 깨질 수 있음... 어떤식으로 설계해야 하는걸까
class AuthenticationServiceTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticationService authService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RefreshTokenRepository tokenRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private LogoutAccessTokenRedisRepository logoutTokenRepository;

    @AfterEach
    void tearDown(){
        logoutTokenRepository.deleteAll();
        tokenRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    // dto에 의존하는 test 상관 없으려나,,, test때문에 빌더도 쓰는데
    @Test
    @DisplayName("사용자는 회원가입을 할 수 있다.")
    public void join() {
        // given
        String email = "test@exam.com";
        String password = "password";
        LocalDate birthDate = LocalDate.of(1999, 9, 27);
        SignupRequest request = createJoinRequest(email, password, birthDate);
        // when
        authService.signup(request);
        // then
        User findUSer = userRepository.findByEmail(email).get();
        assertAll(
                () -> assertEquals(findUSer.getEmail(), email),
                () -> assertTrue(passwordEncoder.matches(password, findUSer.getPassword())),
                () -> assertEquals(findUSer.getBirthDate(), birthDate)
        );
    }

    @Test
    @DisplayName("이미 가입된 이메일인 경우 예외가 발생한다.")
    public void AuthenticationServiceTest() {
        // given
        String email = "test@exam.com";
        String password = "password";
        LocalDate birthDate = LocalDate.of(1999, 9, 27);
        createAndSaveUser(email, password);
        SignupRequest request = createJoinRequest(email, password, birthDate);

        // when //then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format("%s은 가입된 이메일 입니다.", email));
    }

    @Test
    @DisplayName("사용자가 로그인하면 refresh 토큰을 발급 받는다.")
    public void login_refreshToken() {
        // given
        String email ="test";
        String password = "pass";
        User user = createAndSaveUser(email, password);
        LoginRequest request = createLoginRequest(email, password);
        // when
        AuthenticationResponse response = authService.login(request);
        // then
        RefreshToken refreshToken = tokenRepository.findTokenByUser(user).get();
        assertEquals(response.getRefreshToken(), refreshToken.getToken());
    }

    @Test
    @DisplayName("사용자가 로그인하면 access 토큰을 발급 받는다.")
    public void login_accessToken() {
        // given
        String email ="test";
        String password = "pass";
        User user = createAndSaveUser(email, password);
        LoginRequest request = createLoginRequest(email, password);
        // when
        AuthenticationResponse response = authService.login(request);
        // then
        String findEmail = jwtService.extractEmail(response.getAccessToken());
        assertEquals(user.getEmail(), findEmail);
    }

    @Test
    @DisplayName("refresh token을 통해서 accessToken을 재발행 할 수있다.")
    public void refreshToken_reissue() {
        // given
        User user = createAndSaveUser("email", "password");
        String refreshToken = jwtService.generateRefreshToken(user);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn(TokenType.BEARER.getType() + refreshToken);

        createAndSaveRefreshToken(user, refreshToken);
        // when
        AuthenticationResponse response = authService.reissueToken(request);
        // then
        assertTrue(jwtService.isTokenValid(response.getAccessToken(), user));
    }

    @Test
    @DisplayName("refresh 토큰을 통해서 access 토큰을 재발행시 기존 refresh 토큰을 재발행한다.")
    public void refreshToken() {
        // given
        User user = createAndSaveUser("email", "password");
        String refreshToken = jwtService.generateRefreshToken(user);
        createAndSaveRefreshToken(user, refreshToken);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn(TokenType.BEARER.getType() + refreshToken);
        // when
        AuthenticationResponse response = authService.reissueToken(request);
        // then
        Optional<User> userByOldToken = tokenRepository.findUserByToken(refreshToken);
        Optional<User> userByNewToken = tokenRepository.findUserByToken(response.getRefreshToken());

        assertTrue(userByOldToken.isEmpty());
        assertTrue(userByNewToken.isPresent());
    }

    //jwt service를 테스트할 때마다 넣어서 처리해주는 일이 생길거같은 느낌,,
    @Test
    @DisplayName("사용자는 로그아웃을 할 수 있다.")
    public void logout() {
        // given
        String email = "email@test.com";
        String password = "password";
        User user = createAndSaveUser(email, password);
        String jwt = jwtService.generateAccessToken(user);

        // request Mock 처리하기
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn(TokenType.BEARER.getType() + jwt);

        LoginRequest loginRequest = createLoginRequest(email, password);
        authService.login(loginRequest);
        // when
        authService.logout(request);
        // then
        Optional<LogoutAccessTokenFromRedis> logoutToken = logoutTokenRepository.findById(jwt);
        Optional<RefreshToken> refreshToken = tokenRepository.findTokenByUser(user);

        assertTrue(logoutToken.isPresent());
        assertTrue(refreshToken.isEmpty());
    }

    private User createAndSaveUser(String email, String password) {
        User user = User.builder()
                .email(email)
                .name("not null")
                .password(passwordEncoder.encode(password))
                .role(Role.USER)
                .birthDate(LocalDate.of(1999, 9, 27))
                .build();

        return userRepository.save(user);
    }

    private SignupRequest createJoinRequest(String email, String password, LocalDate birthDate) {
        return SignupRequest.builder()
                .email(email)
                .password(password)
                .birthDate(birthDate)
                .build();
    }

    private LoginRequest createLoginRequest(String email, String password) {
        return LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
    }

    private RefreshToken createAndSaveRefreshToken(User user, String refreshToken) {
        RefreshToken token = RefreshToken.builder()
                .token(refreshToken)
                .tokenType(TokenType.BEARER)
                .user(user)
                .build();

        return tokenRepository.save(token);
    }
}