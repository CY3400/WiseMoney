package com.charbel.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.charbel.backend.DTO.CreateBudgetManagementRequest;
import com.charbel.backend.DTO.UpdateBudgetManagementRequest;
import com.charbel.backend.model.BudgetManagement;
import com.charbel.backend.model.Category;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.CategoryRepo;
import com.charbel.backend.service.BudgetManagementService;
import com.charbel.backend.service.UserService;

@RestController
@RequestMapping("/management")
public class BudgetManagementController {
    private final UserService userService;
    private final CategoryRepo catRepo;
    private final BudgetManagementService bMService;

    public BudgetManagementController(UserService userService, BudgetManagementService bMService, CategoryRepo catRepo) {
        this.userService = userService;
        this.bMService = bMService;
        this.catRepo = catRepo;
    }

    private Map<String, Object> toMap(BudgetManagement b) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", b.getId());

        var c = b.getCategory();
        Map<String, Object> cat = new HashMap<>();
        cat.put("id", c.getId());
        cat.put("name", c.getName());
        cat.put("type", c.getType());

        m.put("category", cat);
        m.put("amount", b.getAmount());
        m.put("typeAllocation", b.getType().name());

        return m;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateBudgetManagementRequest req, Authentication auth) {
        Users user = userService.currentUser(auth);

        Category cat = catRepo.findById(req.getCategory()).orElseThrow(() -> new IllegalArgumentException("Gestion introuvable"));

        BudgetManagement created = bMService.createBudgetManagement(user, cat, req.getAmount(), req.getType());

        return ResponseEntity.status(201).body(toMap(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody UpdateBudgetManagementRequest req, Authentication auth) {
        Users user = userService.currentUser(auth);

        Category cat = catRepo.findById(req.getCategory()).orElseThrow(() -> new IllegalArgumentException("Gestion introuvable"));

        BudgetManagement updated = bMService.updateBudgetManagement(id, user, cat, req.getAmount(), req.getType());

        return ResponseEntity.ok(toMap(updated));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication auth) {
        Users user = userService.currentUser(auth);

        List<Map<String, Object>> out = bMService.getBudgetManagementForUser(user).stream().map(this::toMap).toList();

        return ResponseEntity.ok(out);
    }
}
