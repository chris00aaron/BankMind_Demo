package com.naal.bankmind.entity.Login;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "\"user\"", schema = "public")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long idUser;

    @Column(name = "dni", length = 100, nullable = false, unique = true)
    private String dni;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "phone", length = 9)
    private String phone;

    @ManyToOne
    @JoinColumn(name = "rol", nullable = false)
    private Role rol;

    @Column(name = "enable", nullable = false)
    private Boolean enable;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_access")
    private LocalDateTime lastAccess;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;
}
