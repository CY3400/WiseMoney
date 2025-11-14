package com.charbel.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.charbel.backend.DTO.PercentGap;
import com.charbel.backend.DTO.StatisticsViews;
import com.charbel.backend.DTO.TopExpensesByMonth;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.BudgetRepo;
import com.charbel.backend.service.BudgetService;
import com.charbel.backend.service.CategoryService;
import com.charbel.backend.service.TransactionService;
import com.charbel.backend.service.UserService;

@RestController
@RequestMapping("/stats")
public class StatisticsController {
    private final BudgetService budgetService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final TransactionService transactionService;

    public StatisticsController(BudgetService budgetService, UserService userService, BudgetRepo budgetRepo, CategoryService categoryService, TransactionService transactionService) {
        this.budgetService = budgetService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
    }

    @GetMapping("/sixMonths")
    public ResponseEntity<List<StatisticsViews>> getSixMonthStats(Authentication auth) {
        Users user = userService.currentUser(auth);

        List<StatisticsViews> stats = budgetService.getLastSixMonthsStatistics(user);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/topByMonth")
    public ResponseEntity<List<TopExpensesByMonth>> getTopExpensesByMonth(Authentication auth) {
        Users user = userService.currentUser(auth);

        List<TopExpensesByMonth> top = categoryService.getTopExpensesByMonths(user);

        return ResponseEntity.ok(top);
    }

    @GetMapping("/gap")
    public ResponseEntity<List<PercentGap>> getPercentGap(Authentication auth) {
        Users user = userService.currentUser(auth);

        List<PercentGap> gap = transactionService.getGapForMonth(user);

        return ResponseEntity.ok(gap);
    }

    @GetMapping("/ExpRevDiff")
    public ResponseEntity<List<StatisticsViews>> getExpensesRevenuesDifference(Authentication auth) {
        Users user = userService.currentUser(auth);

        List<StatisticsViews> diff = transactionService.getExpensesRevenuesDifference(user);

        return ResponseEntity.ok(diff);
    }
}
