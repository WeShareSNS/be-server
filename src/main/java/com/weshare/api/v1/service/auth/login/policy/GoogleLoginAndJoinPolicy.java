package com.weshare.api.v1.service.auth.login.policy;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.weshare.api.v1.service.auth.login.OAuthApiException;
import com.weshare.api.v1.service.auth.login.ResponseAuthToken;
import com.weshare.api.v1.domain.user.User;
import com.weshare.api.v1.repository.user.UserRepository;
import com.weshare.api.v1.token.jwt.JwtService;
import com.weshare.api.v1.token.RefreshTokenRepository;
import com.weshare.api.v1.token.TokenType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static com.weshare.api.v1.domain.user.Social.GOOGLE;

@Component
public class GoogleLoginAndJoinPolicy extends AbstractProviderLoginAndJoinPolicy {

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUrl;
    @Value("${spring.security.oauth2.client.registration.google.authorization-grant-type}")
    private String grantType;
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;
    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoUri;

    public GoogleLoginAndJoinPolicy(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        super(userRepository, refreshTokenRepository, jwtService);
    }

    @Override
    public boolean isIdentityProvider(String providerName) {
        return GOOGLE.getProviderName().equals(providerName);
    }

    @Override
    protected ResponseAuthToken getToken(String code) {
        String requestTokenUrl = tokenUrl;
        MultiValueMap<String, String> requestBody = getTokenRequestBody(code);
        RestClient restClient = RestClient.create(requestTokenUrl);

        return restClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new OAuthApiException(response.getStatusCode(), response.getHeaders());
                })
                .toEntity(ResponseAuthToken.class)
                .getBody();
    }

    private MultiValueMap<String, String> getTokenRequestBody(String code) {
        var body = new LinkedMultiValueMap<String, String>();
        body.add("grant_type", grantType);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);
        return body;
    }

    @Override
    protected String getResponseBody(String accessToken) {
        String reqURL = userInfoUri;

        RestClient restClient = RestClient.create(reqURL);
        return restClient.get()
                .header(HttpHeaders.AUTHORIZATION, TokenType.BEARER.getType() + accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, rep) -> {
                    throw new OAuthApiException(rep.getStatusCode(), rep.getHeaders());
                })
                .body(String.class);
    }

    @Override
    protected User getAuthUser(String responseBody) {
        JsonElement element = JsonParser.parseString(responseBody);
        var email = element.getAsJsonObject().get("email").getAsString();
        var profileImg = element.getAsJsonObject().get("picture").getAsString();
        return createAuthUser(email, profileImg, GOOGLE);
    }
}