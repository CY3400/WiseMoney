package com.charbel.backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charbel.backend.model.BudgetManagement;
import com.charbel.backend.model.Category;
import com.charbel.backend.model.TypeAllocation;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.BudgetManagementRepo;

@Service
@Transactional
public class BudgetManagementService {
    private final BudgetManagementRepo repo;

    public BudgetManagementService(BudgetManagementRepo repo) {
        this.repo = repo;
    }

    public BudgetManagement createBudgetManagement(Users user, Category category, BigDecimal amount, TypeAllocation type) {
        if(user == null) {
            throw new IllegalArgumentException("Utilisateur requis");
        }

        if(category == null) {
            throw new IllegalArgumentException("Catégorie requise");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Montant requis (≥ 0)");
        }

        if(type == null) {
            throw new IllegalArgumentException("Type d'allocation requis");
        }

        BudgetManagement b = new BudgetManagement();
        b.setUser(user);
        b.setCategory(category);
        b.setAmount(amount);
        b.setType(type);

        return repo.save(b);
    }

    public BudgetManagement updateBudgetManagement(Long id, Users user, Category category, BigDecimal amount, TypeAllocation type) {
        if(id == null) {
            throw new IllegalArgumentException("Identifiant de la transaction requise");
        }

        if(user == null) {
            throw new IllegalArgumentException("Utilisateur requis");
        }

        if(category == null) {
            throw new IllegalArgumentException("Catégorie requise");
        }

        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant requis");
        }

        if(type == null) {
            throw new IllegalArgumentException("Type d'allocation requis");
        }

        BudgetManagement existing = repo.findByIdAndUser(id, user).orElseThrow(() -> new IllegalArgumentException("Gestion introuvable"));

        existing.setCategory(category);
        existing.setAmount(amount);
        existing.setType(type);

        return repo.save(existing);
    }

    @Transactional(readOnly = true)
    public List<BudgetManagement> getBudgetManagementForUser(Users user) {
        Objects.requireNonNull(user, "Utilisateur requis");

        return repo.findByUser(user);
    }
}
