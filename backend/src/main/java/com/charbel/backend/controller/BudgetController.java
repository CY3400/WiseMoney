package com.charbel.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.charbel.backend.DTO.CreateBudgetRequest;
import com.charbel.backend.DTO.UpdateBudgetRequest;
import com.charbel.backend.model.Budget;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.BudgetRepo;
import com.charbel.backend.service.BudgetService;
import com.charbel.backend.service.UserService;

@RestController
@RequestMapping("/budgets")
public class BudgetController {
    private final BudgetService budgetService;
    private final UserService userService;
    private final BudgetRepo budgetRepo;

    public BudgetController(BudgetService budgetService, UserService userService, BudgetRepo budgetRepo) {
        this.budgetService = budgetService;
        this.userService = userService;
        this.budgetRepo = budgetRepo;
    }

    private Map<String, Object> toMap(Budget b) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", b.getId());
        m.put("amount", b.getAmount());
        m.put("month", b.getMonth());
        m.put("year", b.getYear());
        return m;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateBudgetRequest req, Authentication auth) {
        Users user = userService.currentUser(auth);

        Budget created = budgetService.createBudget(user, req.getAmount());

        return ResponseEntity.status(201).body(toMap(created));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@RequestParam int year, @RequestParam int month, Authentication auth) {
        Users user = userService.currentUser(auth);

        List<Map<String, Object>> out = budgetService.getBudgetForMonth(user, year, month).stream().map(this::toMap).toList();

        return ResponseEntity.ok(out);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable long id, @RequestBody UpdateBudgetRequest req, Authentication auth) {
        userService.currentUser(auth);

        Budget updated = budgetService.updateBudget(id, req.getAmount());
        return ResponseEntity.ok(toMap(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        userService.currentUser(auth);

        budgetRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
