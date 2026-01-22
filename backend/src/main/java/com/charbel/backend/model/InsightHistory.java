package com.charbel.backend.model;

import java.time.LocalDateTime;

import com.charbel.backend.DTO.InsightSeverity;
import com.charbel.backend.DTO.InsightType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "insight_history", uniqueConstraints = @UniqueConstraint(name = "uq_insight_history_user_fp", columnNames = {"user_id", "fingerprint"}))
public class InsightHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", nullable = false, foreignKey = @ForeignKey(name="fk_history_user"))
    private Users user;

    @NotBlank
    @Column(name = "month", nullable = false, length = 7)
    private String month;

    @Enumerated(EnumType.STRING)
    @Column(name="type", nullable=false, length=50)
    private InsightType type;

    @NotBlank
    @Column(name="fingerprint", nullable=false, length=255)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name="severity", nullable=false, length=10)
    private InsightSeverity severity;

    @Min(0) @Max(100)
    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Users getUser() { return user; }
    public void setUser(Users user) { this.user = user; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public InsightType getType() { return type; }
    public void setType(InsightType type) { this.type = type; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public InsightSeverity getSeverity() { return severity; }
    public void setSeverity(InsightSeverity severity) { this.severity = severity; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
