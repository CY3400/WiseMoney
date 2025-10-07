package com.charbel.backend.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.charbel.backend.model.Budget;
import com.charbel.backend.model.CategoryType;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.BudgetRepo;
import com.charbel.backend.repo.TransactionRepo;
import com.charbel.backend.repo.UserRepo;

import jakarta.transaction.Transactional;

@Service
public class BudgetRolloverJob {
    private static final Logger log = LoggerFactory.getLogger(BudgetRolloverJob.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Beirut");

    private final BudgetRepo budgetRepo;
    private final TransactionRepo transactionRepo;
    private final UserRepo userRepo;
    private final BudgetService budgetService;

    public BudgetRolloverJob(BudgetRepo budgetRepo, TransactionRepo transactionRepo, UserRepo userRepo, BudgetService budgetService) {
        this.budgetRepo = budgetRepo;
        this.transactionRepo = transactionRepo;
        this.userRepo = userRepo;
        this.budgetService = budgetService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartupCatchup() {
        try {
            runOnceForAllUsers();
        } catch (Exception ex) {
            log.warn("BudgetRolloverJob onStartupCatchup failed: {}", ex.getMessage());
        }
    }

    @Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Beirut")
    @Transactional
    public void monthlyRollover() {
        try {
            runOnceForAllUsers();
        } catch (Exception ex) {
            log.warn("BudgetRolloverJob monthlyRollover failed: {}", ex.getMessage());
        }
    }

    private void runOnceForAllUsers() {
        YearMonth now = YearMonth.now(ZONE);
        int targetMonth = now.getMonthValue();
        int targetYear = now.getYear();

        List<Users> users = userRepo.findAll();
        if (users.isEmpty()) {
            log.info("Aucun utilisateur - rien à faire");
            return;
        }

        for (Users user : users) {
            Optional<Budget> lastOpt = budgetRepo.findTopByUserOrderByYearDescMonthDesc(user);
            if (lastOpt.isEmpty()) continue;

            Budget last = lastOpt.get();

            boolean exists = budgetRepo.existsByUserAndMonthAndYear(user, targetMonth, targetYear);
            if (exists) {
                log.debug("Budget déjà existant pour user={}, {}/{}", user.getEmail(), targetMonth, targetYear);
                continue;
            }

            YearMonth ref = YearMonth.of(last.getYear(), last.getMonth());

            var start = ref.atDay(1).atStartOfDay(ZONE).toLocalDate();
            var end = ref.plusMonths(1).atDay(1).atStartOfDay(ZONE).toLocalDate();

            var gains = transactionRepo.sumByUserAndMonthYearAndType(user, start, end, CategoryType.REVENU);
            var depenses = transactionRepo.sumByUserAndMonthYearAndType(user, start, end, CategoryType.DEPENSE);

            if (gains == null) gains = BigDecimal.ZERO;
            if (depenses == null) depenses = BigDecimal.ZERO;

            BigDecimal newAmount = last.getAmount()
                    .add(gains)
                    .subtract(depenses);


            budgetService.createBudget(user, newAmount);

            log.info("Budget rollover OK user={}, base(last)={}, gains={}, depenses={}, new={} (pour {}/{})",
                    user.getEmail(), last.getAmount(), gains, depenses, newAmount, targetMonth, targetYear);
        }
    }
}
