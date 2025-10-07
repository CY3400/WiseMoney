package com.charbel.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charbel.backend.model.Category;
import com.charbel.backend.model.Transaction;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.TransactionRepo;

@Service
@Transactional
public class TransactionService {
    private final TransactionRepo repo;

    public TransactionService(TransactionRepo repo) {
        this.repo = repo;
    }

    public Transaction createTransaction(Users user, Category category, BigDecimal amount, LocalDate transactionDate, String notes) {
        if(user == null) {
            throw new IllegalArgumentException("Utilisateur requis");
        }

        if(category == null) {
            throw new IllegalArgumentException("Catégorie requise");
        }

        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant requis");
        }

        if (transactionDate == null) {
            throw new IllegalArgumentException("Date de transaction requise");
        }

        Transaction t = new Transaction();
        t.setUser(user);
        t.setCategory(category);
        t.setAmount(amount);
        t.setTransactionDate(transactionDate);
        t.setNotes(notes);

        return repo.save(t);
    }

    public Transaction updateTransaction(Long id, Users user, Category category, BigDecimal amount, LocalDate transactionDate, String notes) {
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

        if (transactionDate == null) {
            throw new IllegalArgumentException("Date de transaction requise");
        }

        Transaction existing = repo.findByIdAndUser(id, user).orElseThrow(() -> new IllegalArgumentException("Transaction introuvable"));

        existing.setCategory(category);
        existing.setAmount(amount);
        existing.setTransactionDate(transactionDate);
        existing.setNotes(notes);

        return repo.save(existing);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsForUser(Users user) {
        Objects.requireNonNull(user, "Utilisateur requis");

        return repo.findByUserOrderByTransactionDateDesc(user);
    }

    public void deleteTransaction(Long id, Users user) {
        if(user == null) {
            throw new IllegalArgumentException("Utilisateur requis");
        }

        repo.findByIdAndUser(id, user).orElseThrow(() -> new IllegalArgumentException("Transaction introuvable"));

        repo.deleteById(id);
    }
}
