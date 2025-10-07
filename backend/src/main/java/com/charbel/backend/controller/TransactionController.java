package com.charbel.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.charbel.backend.DTO.CreateTransactionRequest;
import com.charbel.backend.DTO.UpdateTransactionRequest;
import com.charbel.backend.model.Category;
import com.charbel.backend.model.Transaction;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.CategoryRepo;
import com.charbel.backend.service.TransactionService;
import com.charbel.backend.service.UserService;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final UserService userService;
    private final TransactionService transactionService;
    private final CategoryRepo catRepo;

    public TransactionController(UserService userService, TransactionService transactionService, CategoryRepo catRepo) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.catRepo = catRepo;
    }

    private Map<String, Object> toMap(Transaction t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());

        var c = t.getCategory();
        Map<String, Object> cat = new HashMap<>();
        cat.put("id", c.getId());
        cat.put("name", c.getName());
        cat.put("type", c.getType());

        var p = c.getParent();
        if(p != null){
            Map<String, Object> parent = new HashMap<>();
            parent.put("id", p.getId());
            parent.put("name", p.getName());
            cat.put("parent", parent);
        }

        m.put("category", cat);
        m.put("amount", t.getAmount());
        m.put("transactionDate", t.getTransactionDate());
        m.put("notes", t.getNotes());

        return m;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateTransactionRequest req, Authentication auth) {
        Users user = userService.currentUser(auth);

        Category cat = catRepo.findById(req.getCategory()).orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable"));

        Transaction created = transactionService.createTransaction(user, cat, req.getAmount(), req.getTransactionDate(), req.getNotes());

        return ResponseEntity.status(201).body(toMap(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody UpdateTransactionRequest req, Authentication auth) {
        Users user = userService.currentUser(auth);

        Category cat = catRepo.findById(req.getCategory()).orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable"));

        Transaction updated = transactionService.updateTransaction(id, user, cat, req.getAmount(), req.getTransactionDate(), req.getNotes());

        return ResponseEntity.ok(toMap(updated));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication auth) {
        Users user = userService.currentUser(auth);

        List<Map<String, Object>> out = transactionService.getTransactionsForUser(user).stream().map(this::toMap).toList();

        return ResponseEntity.ok(out);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        Users user = userService.currentUser(auth);

        transactionService.deleteTransaction(id, user);

        return ResponseEntity.noContent().build();
    }
}
