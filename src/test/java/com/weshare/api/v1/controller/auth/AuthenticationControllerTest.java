package com.weshare.api.v1.controller.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.weshare.api.v1.controller.IntegrationMvcTestSupport;
import com.weshare.api.v1.controller.auth.dto.DuplicateEmailRequest;
import com.weshare.api.v1.controller.auth.dto.LoginRequest;
import com.weshare.api.v1.controller.auth.dto.SignupRequest;
import com.weshare.api.v1.domain.user.Role;
import com.weshare.api.v1.domain.user.User;
import com.weshare.api.v1.repository.user.UserRepository;
import com.weshare.api.v1.token.RefreshToken;
import com.weshare.api.v1.token.RefreshTokenRepository;
import com.weshare.api.v1.token.TokenType;
import com.weshare.api.v1.token.jwt.JwtService;
import com.weshare.api.v1.token.logout.LogoutAccessTokenFromRedis;
import com.weshare.api.v1.token.logout.LogoutAccessTokenRedisRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationControllerTest extends IntegrationMvcTestSupport {

    private static final String PREFIX_ENDPOINT = "/api/v1/auth";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RefreshTokenRepository tokenRepository;
    @Autowired
    private LogoutAccessTokenRedisRepository logoutTokenRepository;
    @Autowired
    private JwtService jwtService;

    @AfterEach
    void tearDown() {
        tokenRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("사용자는 회원가입을 할 수 있다.")
    public void signup() throws Exception {
        // given
        SignupRequest request = createSignupRequest(
                "email@asd.com",
                "password",
                "1999-09-27"
        );
        String content = getContent(request);

        // when // then
        mockMvc.perform(post(PREFIX_ENDPOINT + "/signup")
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andDo(print());
    }

    @Test
    @DisplayName("사용자 이메일 중복시 409를 반환한다.")
    public void duplicateEmailConflict() throws Exception {
        // given
        String email = "email@asd.com";
        String password = "password";
        createAndSaveUser(email, password);
        DuplicateEmailRequest request = new DuplicateEmailRequest(email);
        String content = getContent(request);

        // when // then
        mockMvc.perform(get(PREFIX_ENDPOINT + "/signup/duplicate-email")
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andDo(print());
    }

    @Test
    @DisplayName("사용자 이메일이 중복 되지 않았는지 확인할 수 있다.")
    public void duplicateEmailOk() throws Exception {
        // given
        String email = "email@asd.com";
        DuplicateEmailRequest request = new DuplicateEmailRequest(email);
        String content = getContent(request);

        // when // then
        mockMvc.perform(get(PREFIX_ENDPOINT + "/signup/duplicate-email")
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("사용자가 이메일 양식을 지키지 않으면 401을 반환한다.")
    public void duplicateEmailBadRequest() throws Exception {
        // given
        String email = "email";
        DuplicateEmailRequest request = new DuplicateEmailRequest(email);
        String content = getContent(request);

        // when // then
        mockMvc.perform(get(PREFIX_ENDPOINT + "/signup/duplicate-email")
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("사용자는 로그인을 할 수 있다.")
    public void login() throws Exception {
        // given
        String email = "email@asd.com";
        String password = "password";
        createAndSaveUser(email, password);

        LoginRequest request = createLoginRequest(email, password);
        String content = getContent(request);
        // when // then
        mockMvc.perform(post(PREFIX_ENDPOINT + "/login")
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("사용자는 refresh 토큰을 통해서 access 토큰을 발급받을 수 있다.")
    public void reissueToken() throws Exception {
        // given
        String cookieName = "Refresh-Token";
        User user = createAndSaveUser("email@asd.com", "password");
        String refreshToken = jwtService.generateRefreshToken(user, new Date(System.nanoTime()));
        createAndSaveToken(user, refreshToken);
        // when // then
        mockMvc.perform(get(PREFIX_ENDPOINT + "/reissue-token")
                        .cookie(new Cookie(cookieName, refreshToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

        String reissueRefreshToken = tokenRepository.findTokenByUser(user).get().getToken();
        Assertions.assertFalse(refreshToken.equals(reissueRefreshToken));
    }

    @Test
    @DisplayName("사용자는 logout할 수 있다.")
    public void logout() throws Exception {
        // given
        User user = createAndSaveUser("email@asd.com", "password");
        String accessToken = jwtService.generateAccessToken(user, new Date(System.nanoTime()));
        // when // then
        mockMvc.perform(post(PREFIX_ENDPOINT + "/logout")
                        .header(HttpHeaders.AUTHORIZATION, TokenType.BEARER.getType() + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

        Optional<LogoutAccessTokenFromRedis> logoutToken = logoutTokenRepository.findById(accessToken);
        Assertions.assertTrue(logoutToken.isPresent());
    }

    private SignupRequest createSignupRequest(String email, String password, String birthDate) {
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

    private User createAndSaveUser(String email, String password) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name("name")
                .birthDate(LocalDate.of(1999, 9, 27))
                .role(Role.USER)
                .build();

        return userRepository.save(user);
    }

    private RefreshToken createAndSaveToken(User user, String refreshToken) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenType(TokenType.BEARER)
                .token(refreshToken)
                .build();

        return tokenRepository.save(token);
    }

    private String getContent(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }
}