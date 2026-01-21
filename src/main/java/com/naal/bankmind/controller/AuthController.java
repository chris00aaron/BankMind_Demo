package com.naal.bankmind.controller;

import com.naal.bankmind.dto.*;
import com.naal.bankmind.service.AuthService;
import com.naal.bankmind.service.PasswordResetService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Paso 1: Login con email y contraseña
     * Envía código OTP al teléfono registrado
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.authenticate(request);
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
            HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.verifyOtpAndLogin(request);

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
}
