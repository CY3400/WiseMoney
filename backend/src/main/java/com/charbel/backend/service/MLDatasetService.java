package com.charbel.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.charbel.backend.DTO.MLSnapshotDTO;
import com.charbel.backend.model.CategoryType;
import com.charbel.backend.model.Transaction;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.TransactionRepo;

@Service
public class MLDatasetService {
    private final TransactionRepo transactionRepo;

    public MLDatasetService(TransactionRepo transactionRepo) {
        this.transactionRepo = transactionRepo;
    }

    public List<MLSnapshotDTO> buildDataset(Users user, int monthsBack, int stepDays) {
        monthsBack = Math.max(1, Math.min(monthsBack, 24));
        stepDays = Math.max(1, Math.min(stepDays, 15));

        YearMonth now = YearMonth.now();
        List<YearMonth> months = new ArrayList<>();

        for(int i = monthsBack; i>=0; i--) {
            months.add(now.minusMonths(i));
        }

        List<MLSnapshotDTO> out = new ArrayList<>();

        for (YearMonth ym : months) {
            LocalDate start = ym.atDay(1);
            LocalDate endExclusive = ym.plusMonths(1).atDay(1);

            List<Transaction> tx = transactionRepo.findByUserAndDateRange(user.getId(), start, endExclusive);

            boolean isFinished = ym.isBefore(now);
            BigDecimal finalMonthExpenses = isFinished ? sumExpensesUntil(tx, ym.atEndOfMonth()) : null;

            int daysInMonth = ym.lengthOfMonth();

            for (int day = 1; day <= daysInMonth; day += stepDays){
                LocalDate snapDate = ym.atDay(day);

                MLSnapshotDTO dto = buildSnapshot(ym, day, daysInMonth, tx, snapDate, finalMonthExpenses);
                out.add(dto);
            }

            if (ym.equals(now)) {
                int today = LocalDate.now().getDayOfMonth();
                int lastGenerated = ((today - 1)/stepDays) * stepDays + 1;
                if(today != lastGenerated && today <= daysInMonth) {
                    LocalDate snapDate = ym.atDay(today);
                    out.add(buildSnapshot(ym, today, daysInMonth, tx, snapDate, null));
                }
            }
        }

        out.sort(Comparator.comparingInt((MLSnapshotDTO dto) -> dto.getYear()).thenComparingInt((MLSnapshotDTO dto) -> dto.getMonth()).thenComparingInt((MLSnapshotDTO dto) -> dto.getDay()));

        return out;
    }

    private MLSnapshotDTO buildSnapshot(YearMonth ym, int day, int daysInMonth, List<Transaction> tx, LocalDate snapDate, BigDecimal finalMonthExpenses) {
        BigDecimal expenses = BigDecimal.ZERO;
        BigDecimal revenues = BigDecimal.ZERO;

        BigDecimal maxExpense = BigDecimal.ZERO;
        int nExpenseTx = 0;

        Set<LocalDate> activeDays = new HashSet<>();

        Map<Long, BigDecimal> expByCat = new HashMap<>();

        for (Transaction t: tx) {
            if(t==null || t.getTransactionDate() == null || t.getAmount() == null || t.getCategory() == null) continue;
            if(t.getTransactionDate().isAfter(snapDate)) continue;

            BigDecimal amt = nz(t.getAmount());
            CategoryType type = t.getCategory().getType();

            if(type == CategoryType.DEPENSE) {
                expenses = expenses.add(amt);
                nExpenseTx++;
                activeDays.add(t.getTransactionDate());

                if(amt.compareTo(maxExpense) > 0) maxExpense = amt;

                Long catId = t.getCategory().getId();
                if(catId != null){
                    expByCat.merge(catId, amt, (oldAmount, newAmount) -> nz(oldAmount).add(nz(newAmount)));
                }
            }
            else if (type == CategoryType.REVENU) {
                revenues = revenues.add(amt);
            }
        }

        BigDecimal net = revenues.subtract(expenses);

        BigDecimal avgDailyExpense = BigDecimal.ZERO;
        if(day > 0) {
            avgDailyExpense = expenses.divide(BigDecimal.valueOf(day), 2, RoundingMode.HALF_UP);
        }

        BigDecimal topCategoryShare = BigDecimal.ZERO;

        if(expenses.compareTo(BigDecimal.ZERO) > 0 && !expByCat.isEmpty()) {
            BigDecimal top = expByCat.values().stream().max((a, b) -> nz(a).compareTo(nz(b))).orElse(BigDecimal.ZERO);
            topCategoryShare = top.multiply(BigDecimal.valueOf(100)).divide(expenses, 2, RoundingMode.HALF_UP);
        }

        MLSnapshotDTO dto = new MLSnapshotDTO();
        dto.setYear(ym.getYear());
        dto.setMonth(ym.getMonthValue());
        dto.setDay(day);
        dto.setDaysInMonth(daysInMonth);
        dto.setExpensesSoFar(expenses);
        dto.setRevenuesSoFar(revenues);
        dto.setNetSoFar(net);
        dto.setAvgDailyExpense(avgDailyExpense);
        dto.setMaxExpenseSoFar(maxExpense);
        dto.setNExpenseTx(nExpenseTx);
        dto.setDaysActive(activeDays.size());
        dto.setTopCategoryShare(topCategoryShare);
        dto.setFinalMonthExpenses(finalMonthExpenses);

        return dto;
    }

    private BigDecimal sumExpensesUntil(List<Transaction> tx, LocalDate dateInclusive) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction t : tx) {
            if(t == null || t.getTransactionDate() == null || t.getAmount() == null || t.getCategory() == null) continue;
            if(t.getCategory().getType() != CategoryType.DEPENSE) continue;
            if(t.getTransactionDate().isAfter(dateInclusive)) continue;

            sum = sum.add(nz(t.getAmount()));
        }
        return sum;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
