package com.charbel.backend.DTO;

import java.math.BigDecimal;

public class MLSnapshotDTO {
    private int year;
    private int month;
    private int day;
    private int daysInMonth;

    private BigDecimal expensesSoFar;
    private BigDecimal revenuesSoFar;
    private BigDecimal netSoFar;

    private BigDecimal avgDailyExpense;
    private BigDecimal maxExpenseSoFar;

    private int nExpenseTx;
    private int daysActive;

    private BigDecimal topCategoryShare;

    private BigDecimal finalMonthExpenses;

    public MLSnapshotDTO() {}

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public int getDaysInMonth() { return daysInMonth; }
    public void setDaysInMonth(int daysInMonth) { this.daysInMonth = daysInMonth; }

    public BigDecimal getExpensesSoFar() { return expensesSoFar; }
    public void setExpensesSoFar(BigDecimal expensesSoFar) { this.expensesSoFar = expensesSoFar; }

    public BigDecimal getRevenuesSoFar() { return revenuesSoFar; }
    public void setRevenuesSoFar(BigDecimal revenuesSoFar) { this.revenuesSoFar = revenuesSoFar; }

    public BigDecimal getNetSoFar() { return netSoFar; }
    public void setNetSoFar(BigDecimal netSoFar) { this.netSoFar = netSoFar; }

    public BigDecimal getAvgDailyExpense() { return avgDailyExpense; }
    public void setAvgDailyExpense(BigDecimal avgDailyExpense) { this.avgDailyExpense = avgDailyExpense; }

    public BigDecimal getMaxExpenseSoFar() { return maxExpenseSoFar; }
    public void setMaxExpenseSoFar(BigDecimal maxExpenseSoFar) { this.maxExpenseSoFar = maxExpenseSoFar; }

    public int getNExpenseTx() { return nExpenseTx; }
    public void setNExpenseTx(int nExpenseTx) { this.nExpenseTx = nExpenseTx; }

    public int getDaysActive() { return daysActive; }
    public void setDaysActive(int daysActive) { this.daysActive = daysActive; }

    public BigDecimal getTopCategoryShare() { return topCategoryShare; }
    public void setTopCategoryShare(BigDecimal topCategoryShare) { this.topCategoryShare = topCategoryShare; }

    public BigDecimal getFinalMonthExpenses() { return finalMonthExpenses; }
    public void setFinalMonthExpenses(BigDecimal finalMonthExpenses) { this.finalMonthExpenses = finalMonthExpenses; }
}
