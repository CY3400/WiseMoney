package com.charbel.backend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.charbel.backend.DTO.StatisticsViews;
import com.charbel.backend.model.Budget;
import com.charbel.backend.model.Users;

public interface BudgetRepo extends JpaRepository<Budget, Long> {
    boolean existsByUserAndMonthAndYear(Users user, int month, int year);

    Optional<Budget> findByUserAndYearAndMonth(Users user, int year, int month);

    Optional<Budget> findTopByUserOrderByYearDescMonthDesc(Users user);

    @Query(value="""
    SELECT SUB2.MONTH, SUB2.YEAR, EXPENSES, REVENUES, B.AMOUNT - EXPENSES + REVENUES AS SAVINGS
    FROM (SELECT MONTH, YEAR, USER_ID, SUM(CASE WHEN TYPE = 'DEPENSE' THEN AMOUNT ELSE 0 END) AS EXPENSES,
    SUM(CASE WHEN TYPE = 'REVENU' THEN AMOUNT ELSE 0 END) AS REVENUES
    FROM (SELECT EXTRACT(MONTH FROM T.TRANSACTION_DATE) AS MONTH, EXTRACT(YEAR FROM T.TRANSACTION_DATE) AS YEAR, T.AMOUNT, C.TYPE, T.USER_ID
    FROM TRANSACTIONS T
    INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID) SUB
    WHERE USER_ID = :userId
    GROUP BY MONTH, YEAR, USER_ID) SUB2
    INNER JOIN BUDGETS B ON B.USER_ID = SUB2.USER_ID AND B.MONTH = SUB2.MONTH AND B.YEAR = SUB2.YEAR
    ORDER BY SUB2.YEAR DESC, SUB2.MONTH DESC LIMIT 6;
    """, nativeQuery = true)
    List<StatisticsViews> getLastSixMonthsStatistics(@Param("userId") Long userId);

    Optional<Budget> findByUserIdAndMonthAndYear(Long userId, int month, int year);
}
