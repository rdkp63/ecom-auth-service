package com.rdkp63.ecom_auth_services.integration;

import com.rdkp63.ecom_auth_services.dto.AuthToken;
import com.rdkp63.ecom_auth_services.dto.requestDTO.LoginRequest;
import com.rdkp63.ecom_auth_services.dto.requestDTO.RegisterRequest;
import com.rdkp63.ecom_auth_services.entity.RefreshToken;
import com.rdkp63.ecom_auth_services.repository.RefreshTokenRepository;
import com.rdkp63.ecom_auth_services.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
public class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_auth")
            .withUsername("test")
            .withPassword("test")
            .withStartupAttempts(3)
            .withStartupTimeout(Duration.ofSeconds(60));

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // ensure flyway runs automatically (spring.flyway.enabled default true)
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    static String testEmail = "it.user@example.com";
    static String testPass = "P@ssword123";
    static String testFullName = "Integration Test User";

    static String accessToken;
    static String refreshToken;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    void cleanUp() {
        // ensure clean DB between tests (or rely on transaction rollback)
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void dockerIsWorking() {
        try (GenericContainer<?> c =
                     new GenericContainer<>("alpine").withCommand("echo hello")) {
            c.start();
        }
    }


    @Test
    @Order(1)
    public void testRegister() {
        RegisterRequest req = RegisterRequest.builder()
                .email(testEmail)
                .password(testPass)
                .fullName(testFullName)
                .build();

        ResponseEntity<Map> resp = restTemplate.postForEntity(baseUrl() + "/auth/register", req, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("email")).isEqualTo(testEmail);
        assertThat(body.get("fullName")).isEqualTo(testFullName);
        assertThat(body.get("id")).isNotNull();
    }

    @Test
    @Order(2)
    public void testLoginReturnsTokens() {
        // register first
        testRegister();

        LoginRequest login = LoginRequest.builder()
                .email(testEmail)
                .password(testPass)
                .build();

        ResponseEntity<AuthToken> resp = restTemplate.postForEntity(baseUrl() + "/auth/login", login, AuthToken.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthToken token = resp.getBody();
        assertThat(token).isNotNull();
        assertThat(token.getAccessToken()).isNotNull();
        assertThat(token.getRefreshToken()).isNotNull();
        assertThat(token.getExpiresIn()).isGreaterThan(0);

        accessToken = token.getAccessToken();
        refreshToken = token.getRefreshToken();

        // verify refresh token persisted
        Optional<RefreshToken> rt = refreshTokenRepository.findByToken(refreshToken);
        assertThat(rt).isPresent();
    }

    @Test
    @Order(3)
    public void testRefreshRotation() throws InterruptedException {
        // ensure login executed
        testLoginReturnsTokens();

        // Call refresh endpoint with existing refresh token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("refreshToken", refreshToken);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<AuthToken> resp = restTemplate.postForEntity(baseUrl() + "/auth/refresh", request, AuthToken.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthToken newToken = resp.getBody();
        assertThat(newToken).isNotNull();
        assertThat(newToken.getAccessToken()).isNotNull();
        assertThat(newToken.getRefreshToken()).isNotNull();

        // rotated refresh token should be different
        assertThat(newToken.getRefreshToken()).isNotEqualTo(refreshToken);

        // old refresh token must be revoked in DB
        Optional<RefreshToken> old = refreshTokenRepository.findByToken(refreshToken);
        assertThat(old).isPresent();
        assertThat(old.get().isRevoked()).isTrue();

        // new token must be persisted and valid
        Optional<RefreshToken> current = refreshTokenRepository.findByToken(newToken.getRefreshToken());
        assertThat(current).isPresent();
        assertThat(current.get().isRevoked()).isFalse();

        // update static tokens for further tests
        accessToken = newToken.getAccessToken();
        refreshToken = newToken.getRefreshToken();
    }

    @Test
    @Order(4)
    public void testLogoutRevokesAllTokens() {
        // ensure rotation done and tokens available
        try {
            testRefreshRotation();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // call logout as authenticated user: set Authorization header
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(null, headers);

        ResponseEntity<Map> resp = restTemplate.postForEntity(baseUrl() + "/auth/logout", entity, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // check DB: all tokens for user should be revoked
        var user = userRepository.findByEmail(testEmail).orElseThrow();
        var tokens = refreshTokenRepository.findAll();
        assertThat(tokens).isNotEmpty();
        assertThat(tokens.stream().allMatch(RefreshToken::isRevoked)).isTrue();
    }
}
