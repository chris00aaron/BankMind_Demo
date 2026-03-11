package com.naal.bankmind.dto.Login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para actualización de usuario.
 * Similar a CreateUserRequest pero con contraseña opcional.
 * El admin puede cambiar la contraseña directamente si lo desea.
 */
@Data
public class UpdateUserRequest {

    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 8, max = 20, message = "El DNI debe tener entre 8 y 20 caracteres")
    private String dni;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    private String fullName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    // Contraseña opcional: si se envía, se actualiza; si está vacía o null, no
    // cambia
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @Size(min = 9, max = 15, message = "El teléfono debe tener entre 9 y 15 caracteres")
    private String phone;

    @NotNull(message = "El rol es obligatorio")
    private Short roleId;
}
