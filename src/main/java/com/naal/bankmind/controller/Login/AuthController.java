package com.naal.bankmind.controller.Login;

import com.naal.bankmind.dto.Login.AuthResponse;
import com.naal.bankmind.dto.Login.ChangePasswordRequest;
import com.naal.bankmind.dto.Login.ForgotPasswordRequest;
import com.naal.bankmind.dto.Login.LoginRequest;
import com.naal.bankmind.dto.Login.LoginResponse;
import com.naal.bankmind.dto.Login.RefreshTokenRequest;
import com.naal.bankmind.dto.Login.ResendOtpRequest;
import com.naal.bankmind.dto.Login.VerifyOtpRequest;
import com.naal.bankmind.dto.Shared.ApiResponse;
import com.naal.bankmind.service.Login.AuthService;
import com.naal.bankmind.service.Login.JwtService;
import com.naal.bankmind.service.Login.PasswordResetService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final JwtService jwtService;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Paso 1: Login con email y contraseña
     * Envía código OTP al correo electrónico registrado
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            LoginResponse response = authService.authenticate(request, ipAddress, userAgent);
            return ResponseEntity.ok(ApiResponse.success("Código de verificación enviado", response));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Paso 2: Verificar código OTP y obtener tokens
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        try {
            String ipAddress = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            AuthResponse authResponse = authService.verifyOtpAndLogin(request, ipAddress, userAgent);

            // Configurar cookies httpOnly seguras
            addSecureCookie(response, "accessToken", authResponse.getAccessToken(),
                    (int) (accessTokenExpiration / 1000));
            addSecureCookie(response, "refreshToken", authResponse.getRefreshToken(),
                    (int) (refreshTokenExpiration / 1000));

            return ResponseEntity.ok(ApiResponse.success("Autenticación exitosa", authResponse));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Reenviar código OTP al correo del usuario
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<LoginResponse>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        try {
            LoginResponse response = authService.resendOtp(request.getMfaToken());
            return ResponseEntity.ok(ApiResponse.success("Código de verificación reenviado", response));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error al reenviar OTP: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al reenviar código de verificación"));
        }
    }

    /**
     * Renovar access token usando refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletResponse response) {
        try {
            // Preferir token de cookie, luego del body
            String refreshToken = refreshTokenCookie != null ? refreshTokenCookie
                    : (request != null ? request.getRefreshToken() : null);

            if (refreshToken == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Refresh token no proporcionado"));
            }

            AuthResponse authResponse = authService.refreshAccessToken(refreshToken);

            // Actualizar cookie de access token
            addSecureCookie(response, "accessToken", authResponse.getAccessToken(),
                    (int) (accessTokenExpiration / 1000));

            return ResponseEntity.ok(ApiResponse.success("Token renovado", authResponse));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Cerrar sesión (revocar refresh token)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            HttpServletResponse response) {

        if (refreshTokenCookie != null) {
            authService.logout(refreshTokenCookie);
        }

        // Eliminar cookies
        deleteCookie(response, "accessToken");
        deleteCookie(response, "refreshToken");

        return ResponseEntity.ok(ApiResponse.success("Sesión cerrada exitosamente"));
    }

    /**
     * Solicitar cambio de contraseña
     * Solo crea una solicitud que el admin procesará
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.requestPasswordReset(request.getEmail());
            // Siempre devolver éxito para no revelar si el email existe
            return ResponseEntity.ok(ApiResponse.success(
                    "Se ha enviado una solicitud de cambio de contraseña al administrador. " +
                            "Será contactado cuando su solicitud sea procesada."));
        } catch (Exception e) {
            // Log interno pero respuesta genérica
            log.error("Error en solicitud de cambio de contraseña: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(
                    "Se ha enviado una solicitud de cambio de contraseña al administrador. " +
                            "Será contactado cuando su solicitud sea procesada."));
        }
    }

    /**
     * Cambiar contraseña (para usuarios con contraseña predeterminada)
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @CookieValue(name = "accessToken", required = false) String cookieToken,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Validar que las contraseñas coincidan
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Las contraseñas no coinciden"));
            }

            // Obtener token (prioridad cookie, luego header)
            String token = cookieToken;
            if (token == null || token.isEmpty()) {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Token no proporcionado"));
            }

            String email = jwtService.extractUsername(token);
            authService.changePassword(email, request.getNewPassword());

            return ResponseEntity.ok(ApiResponse.success(
                    "Contraseña actualizada exitosamente. Por favor inicie sesión nuevamente."));
        } catch (Exception e) {
            log.error("Error al cambiar contraseña: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al cambiar contraseña: " + e.getMessage()));
        }
    }

    private void addSecureCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // En producción usar HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
