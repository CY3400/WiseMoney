package com.charbel.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "password_reset_token",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_password_reset_token_token", columnNames = "token")
    },
    indexes = {
        @Index(name = "idx_password_reset_token_user", columnList = "user_id"),
        @Index(name = "idx_password_reset_token_expires", columnList = "expires_at")
    }
)
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_password_reset_token_user"))
    private Users user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public Long getId() { return id; }
    public void setId(Long id){ this.id = id; }

    public String getToken(){ return token; }
    public void setToken(String token){ this.token = token; }

    public Users getUser(){ return user; }
    public void setUser(Users user){ this.user = user; }

    public LocalDateTime getExpiresAt(){ return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt){ this.expiresAt = expiresAt; }

    public LocalDateTime getUsedAt(){ return usedAt; }
    public void setUsedAt(LocalDateTime usedAt){ this.usedAt = usedAt; }
}