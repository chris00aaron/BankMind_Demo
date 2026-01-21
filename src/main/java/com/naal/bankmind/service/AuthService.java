package com.naal.bankmind.service;

import com.naal.bankmind.dto.*;
import com.naal.bankmind.entity.OtpVerification;
import com.naal.bankmind.entity.RefreshToken;
import com.naal.bankmind.entity.User;
import com.naal.bankmind.repository.RefreshTokenRepository;
import com.naal.bankmind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public LoginResponse authenticate(LoginRequest request) {
        try {
            // Validar credenciales
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));
        } catch (BadCredentialsException e) {
            log.warn("Intento de login fallido para: {}", request.getEmail());
            throw new BadCredentialsException("Credenciales inválidas");
        }

        // Buscar usuario
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        // Validar que tenga teléfono para MFA
        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            log.error("Usuario sin teléfono registrado: {}", user.getEmail());
            throw new IllegalStateException("El usuario no tiene un número de teléfono registrado para MFA");
        }

        // Generar y enviar OTP
        OtpVerification otp = otpService.generateOtp(user);

        return LoginResponse.builder()
                .requiresMfa(true)
                .mfaToken(otp.getMfaToken())
                .phoneHint(otpService.getPhoneHint(user.getPhone()))
                .message("Código de verificación enviado al teléfono " + otpService.getPhoneHint(user.getPhone()))
                .build();
    }

    @Transactional
    public AuthResponse verifyOtpAndLogin(VerifyOtpRequest request) {
        Optional<OtpVerification> otpOpt = otpService.verifyOtp(request.getMfaToken(), request.getCode());

        if (otpOpt.isEmpty()) {
            int remaining = otpService.getRemainingAttempts(request.getMfaToken());
            throw new BadCredentialsException(
                    remaining > 0
                            ? "Código incorrecto. Intentos restantes: " + remaining
                            : "Código expirado o máximo de intentos alcanzado. Solicite uno nuevo.");
        }

        User user = otpOpt.get().getUser();

        // Actualizar último acceso
        user.setLastAccess(LocalDateTime.now());
        userRepository.save(user);

        // Generar tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Guardar refresh token en base de datos
        saveRefreshToken(user, refreshToken);

        log.info("✅ Login exitoso para usuario: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserDto(user))
                .message("Autenticación exitosa")
                .build();
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));

        if (storedToken.isExpired()) {
            throw new BadCredentialsException("Refresh token expirado");
        }

        User user = storedToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String newAccessToken = jwtService.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Mantener el mismo refresh token
                .user(mapToUserDto(user))
                .message("Token renovado exitosamente")
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    log.info("Refresh token revocado para usuario: {}", token.getUser().getEmail());
                });
    }

    @Transactional
    public void logoutAll(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));
        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("Todos los tokens revocados para usuario: {}", email);
    }

    private void saveRefreshToken(User user, String token) {
        // Revocar tokens anteriores del usuario
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpiration() / 1000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getIdUser())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .dni(user.getDni())
                .phone(otpService.getPhoneHint(user.getPhone()))
                .role(user.getRol() != null ? user.getRol().getCodRole() : null)
                .roleName(user.getRol() != null ? user.getRol().getName() : null)
                .build();
    }
}
