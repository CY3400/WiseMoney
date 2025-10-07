package com.charbel.backend.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charbel.backend.model.Budget;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.BudgetRepo;

@Service
@Transactional
public class BudgetService {
    private final BudgetRepo repo;

    public BudgetService(BudgetRepo repo) {
        this.repo = repo;
    }

    public Budget createBudget(Users user, BigDecimal amount) {
        if(user == null) {
            throw new IllegalArgumentException("Utilisateur requis");
        }

        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant invalide");
        }

        var now = YearMonth.now();
        int m = now.getMonthValue();
        int y = now.getYear();

        if(repo.existsByUserAndMonthAndYear(user, m, y)) {
            throw new IllegalArgumentException("Budget déjà existant pour ce mois");
        }

        Budget b = new Budget();
        b.setUser(user);
        b.setAmount(amount);

        return repo.save(b);
    }

    public Budget updateBudget(Long id, BigDecimal amount) {
        if(id == null) {
            throw new IllegalArgumentException("Identifiant du budget requis");
        }

        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant invalide");
        }

        Budget existing = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Budget introuvable"));

        existing.setAmount(amount);
        return repo.save(existing);
    }

    @Transactional(readOnly = true)
    public Optional<Budget> getBudgetForMonth(Users user, int year, int month) {
        Objects.requireNonNull(user, "Utilisateur requis");

        return repo.findByUserAndYearAndMonth(user, year, month);
    }

    @Transactional
    public void ensureCurrentMonthBudget(Users user) {
        var now = YearMonth.now();
        int m = now.getMonthValue(), y = now.getYear();

        if(!repo.existsByUserAndMonthAndYear(user, m, y)) {
            repo.findTopByUserOrderByYearDescMonthDesc(user).ifPresent(last -> createBudget(user, last.getAmount()));
        }
    }
}
