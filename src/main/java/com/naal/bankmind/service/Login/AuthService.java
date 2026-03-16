package com.naal.bankmind.service.Login;

import com.naal.bankmind.dto.Login.AuthResponse;
import com.naal.bankmind.dto.Login.LoginRequest;
import com.naal.bankmind.dto.Login.LoginResponse;
import com.naal.bankmind.dto.Login.VerifyOtpRequest;
import com.naal.bankmind.dto.Shared.UserDto;
import com.naal.bankmind.entity.Login.OtpVerification;
import com.naal.bankmind.entity.Login.RefreshToken;
import com.naal.bankmind.entity.Login.User;
import com.naal.bankmind.repository.Login.RefreshTokenRepository;
import com.naal.bankmind.repository.Shared.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public LoginResponse authenticate(LoginRequest request, String ipAddress, String userAgent) {
        try {
            // Validar credenciales
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));
        } catch (BadCredentialsException e) {
            log.warn("Intento de login fallido para: {}", request.getEmail());
            // Registrar intento fallido en auditoría
            auditService.logFailedLogin(request.getEmail(), ipAddress, userAgent, "Credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        // Buscar usuario
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        // Si el usuario debe cambiar contraseña (contraseña predeterminada),
        // saltar OTP y devolver flag especial
        if (Boolean.TRUE.equals(user.getMustChangePassword())) {
            log.info("🔐 Usuario con contraseña predeterminada detectado: {}", user.getEmail());

            // Generar tokens temporales para cambio de contraseña
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            String accessToken = jwtService.generateAccessToken(userDetails);

            return LoginResponse.builder()
                    .requiresMfa(false)
                    .requiresPasswordChange(true)
                    .accessToken(accessToken)
                    .userId(user.getIdUser())
                    .message("Debe cambiar su contraseña")
                    .build();
        }

        // Generar y enviar OTP por email
        OtpVerification otp = otpService.generateOtp(user);

        return LoginResponse.builder()
                .requiresMfa(true)
                .requiresPasswordChange(false)
                .mfaToken(otp.getMfaToken())
                .emailHint(otpService.getEmailHint(user.getEmail()))
                .message("Código de verificación enviado al correo " + otpService.getEmailHint(user.getEmail()))
                .build();
    }

    /**
     * Reenviar código OTP al email del usuario.
     * Busca el usuario por el mfaToken activo y genera un nuevo OTP.
     */
    @Transactional
    public LoginResponse resendOtp(String mfaToken) {
        User user = otpService.findUserByMfaToken(mfaToken)
                .orElseThrow(() -> new BadCredentialsException(
                        "Sesión MFA no encontrada o expirada. Inicie sesión nuevamente."));

        // Generar nuevo OTP (invalida el anterior automáticamente)
        OtpVerification newOtp = otpService.generateOtp(user);

        log.info("🔄 OTP reenviado por correo al usuario: {}", otpService.getEmailHint(user.getEmail()));

        return LoginResponse.builder()
                .requiresMfa(true)
                .requiresPasswordChange(false)
                .mfaToken(newOtp.getMfaToken())
                .emailHint(otpService.getEmailHint(user.getEmail()))
                .message("Nuevo código de verificación enviado al correo " + otpService.getEmailHint(user.getEmail()))
                .build();
    }

    @Transactional
    public AuthResponse verifyOtpAndLogin(VerifyOtpRequest request, String ipAddress, String userAgent) {
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

        // Registrar login exitoso en auditoría
        auditService.logLogin(user, ipAddress, userAgent, "SUCCESS", null);

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
                .role(user.getRol() != null ? user.getRol().getCodRole() : null)
                .roleName(user.getRol() != null ? user.getRol().getName() : null)
                .build();
    }

    /**
     * Cambiar contraseña de usuario (para usuarios con contraseña predeterminada)
     */
    @Transactional
    public void changePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        // Actualizar contraseña
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false); // Ya no requiere cambio
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Revocar todos los tokens del usuario para forzar nuevo login
        refreshTokenRepository.revokeAllUserTokens(user);

        log.info("🔐 Contraseña cambiada exitosamente para usuario: {}", email);
    }
}
