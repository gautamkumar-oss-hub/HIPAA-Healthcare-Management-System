package com.healthcare.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;
    private final Totp totp;
    private final FieldEncryption fieldEncryption;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       AuthProperties properties, Totp totp, FieldEncryption fieldEncryption) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.totp = totp;
        this.fieldEncryption = fieldEncryption;
    }

    @Transactional
    public TokenResponse register(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);
        if (users.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists");
        }
        AppUser user = users.save(new AppUser(
                normalizedEmail, passwordEncoder.encode(password), UserRole.PATIENT));
        return issueTokens(user);
    }

    @Transactional
    public UserSummary createUser(String email, String password, UserRole role) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);
        if (users.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists");
        }
        AppUser user = users.save(new AppUser(
                normalizedEmail, passwordEncoder.encode(password), role));
        return summary(user);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return users.findAll().stream().map(AuthService::summary).toList();
    }

    @Transactional(readOnly = true)
    public UserSummary getUser(UUID userId) {
        return users.findById(userId)
                .map(AuthService::summary)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    @Transactional
    public void deleteUser(UUID userId) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        users.delete(user);
    }

    @Transactional
    public TokenResponse login(String email, String password, String mfaCode) {
        AppUser user = users.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (requiresMfa(user) && !user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MFA enrollment required");
        }
        if (user.isMfaEnabled() && !totp.verify(fieldEncryption.decrypt(user.getMfaSecret()), mfaCode)) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        RefreshToken oldToken = refreshTokens.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (oldToken.getRevokedAt() != null || !oldToken.getExpiresAt().isAfter(Instant.now())) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        oldToken.revoke();
        return issueTokens(oldToken.getUser());
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(hashToken(rawToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public MfaSetup setupMfa(String email, String password) {
        AppUser user = authenticatePassword(email, password);
        if (user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MFA is already enabled");
        }
        byte[] secret = totp.newSecret();
        user.setMfaSecret(fieldEncryption.encrypt(secret));
        return new MfaSetup(totp.provisioningUri(user.getEmail(), secret, properties.issuer()));
    }

    @Transactional
    public void verifyMfa(String email, String password, String code) {
        AppUser user = authenticatePassword(email, password);
        byte[] encryptedSecret = user.getMfaSecret();
        if (encryptedSecret == null || !totp.verify(fieldEncryption.decrypt(encryptedSecret), code)) {
            throw new BadCredentialsException("Invalid MFA code");
        }
        user.setMfaEnabled(true);
    }

    private AppUser authenticatePassword(String email, String password) {
        AppUser user = users.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return user;
    }

    private TokenResponse issueTokens(AppUser user) {
        Instant now = Instant.now();
        Instant accessExpiry = now.plus(properties.accessTokenMinutes(), ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(accessExpiry)
                .subject(user.getId().toString())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();
        String refreshToken = generateRefreshToken();
        refreshTokens.save(new RefreshToken(user, hashToken(refreshToken),
                now.plus(properties.refreshTokenDays(), ChronoUnit.DAYS)));
        return new TokenResponse(accessToken, refreshToken, accessExpiry, user.getRole());
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 128
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().noneMatch(character -> !Character.isLetterOrDigit(character))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be 12-128 characters and include upper, lower, number, and symbol");
        }
    }

    private static boolean requiresMfa(AppUser user) {
        return user.getRole() == UserRole.DOCTOR || user.getRole() == UserRole.ADMIN;
    }

    private static UserSummary summary(AppUser user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getRole(), user.isMfaEnabled());
    }

    private static String generateRefreshToken() {
        byte[] bytes = new byte[48];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String rawToken) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not hash refresh token", exception);
        }
    }

    public record TokenResponse(String accessToken, String refreshToken, Instant expiresAt, UserRole role) {}
    public record MfaSetup(String provisioningUri) {}
    public record UserSummary(UUID id, String email, UserRole role, boolean mfaEnabled) {}
}
