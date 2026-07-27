package com.charbel.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(
    name = "categories",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_category_user_name",
            columnNames = {"user_id","name"}
        )
    },
    indexes = {
        @Index(name = "idx_categories_user", columnList = "user_id"),
        @Index(name = "idx_categories_type", columnList = "type")
    }
)
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", nullable = false, foreignKey = @ForeignKey(name="fk_category_user"))
    private Users user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name="parent_id", nullable = true, foreignKey = @ForeignKey(name="fk_category_parent"))
    private Category parent;

    @NotBlank
    @Size(min=2,max=100)
    @Column(nullable = false)
    private String name;

    @Min(0) @Max(1)
    @Column(nullable = false)
    private int status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_allocation", nullable = false, length = 20)
    private TypeAllocation typeAllocation = TypeAllocation.LBP;

    @Min(1)
    @Max(2)
    @Column(nullable = false)
    private int frequency = 1;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        status = 1;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }

    public Users getUser(){ return user; }
    public void setUser(Users user){ this.user = user; }

    public CategoryType getType(){ return type; }
    public void setType(CategoryType type){ this.type = type; }

    public Category getParent(){ return parent; }
    public void setParent(Category parent){ this.parent = parent; }

    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }

    public int getStatus(){ return status; }
    public void setStatus(int status){ this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TypeAllocation getTypeAllocation() {
        return typeAllocation;
    }

    public void setTypeAllocation(TypeAllocation typeAllocation) {
        this.typeAllocation = typeAllocation;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
}
