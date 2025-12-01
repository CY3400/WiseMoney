package com.charbel.backend.repo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.charbel.backend.DTO.ChildPercentView;
import com.charbel.backend.DTO.ParentSpendView;
import com.charbel.backend.DTO.PercentGap;
import com.charbel.backend.DTO.StatisticsViews;
import com.charbel.backend.model.CategoryType;
import com.charbel.backend.model.Transaction;
import com.charbel.backend.model.Users;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdAndUser(Long id, Users user);

    @Query("""
            select t from Transaction t
            join fetch t.category c
            left join fetch c.parent p
            where t.user = :user
            order by t.transactionDate desc
            """)
    List<Transaction> findByUserOrderByTransactionDateDesc(@Param("user") Users user);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user = :user
            AND t.transactionDate >= :start
            AND t.transactionDate < :end
            AND t.category.type = :type
            """)
    BigDecimal sumByUserAndMonthYearAndType(Users user, LocalDate start, LocalDate end, CategoryType type);

    @Query(value="""
            SELECT CASE WHEN REVENU + B.AMOUNT = 0 THEN 0 ELSE ROUND(DEPENSE/(REVENU+B.AMOUNT)*100,0) END PERCENT
            FROM (SELECT COALESCE(SUM(CASE WHEN TYPE = 'DEPENSE' THEN T.AMOUNT ELSE 0 END), 0) AS DEPENSE, COALESCE(SUM(CASE WHEN TYPE = 'REVENU' THEN T.AMOUNT ELSE 0 END), 0) AS REVENU, T.USER_ID
            FROM WISE_MONEY.TRANSACTIONS T
            INNER JOIN WISE_MONEY.CATEGORIES C ON C.ID = T.CATEGORY_ID
            WHERE T.USER_ID = :userId AND EXTRACT(MONTH FROM SYSDATE()) = EXTRACT(MONTH FROM T.TRANSACTION_DATE)
            AND EXTRACT(YEAR FROM SYSDATE()) = EXTRACT(YEAR FROM T.TRANSACTION_DATE)
            GROUP BY T.USER_ID) SUB
            INNER JOIN WISE_MONEY.BUDGETS B ON B.USER_ID = SUB.USER_ID
            WHERE EXTRACT(MONTH FROM SYSDATE()) = MONTH
            AND EXTRACT(YEAR FROM SYSDATE()) = YEAR
            """, nativeQuery = true)
    Integer findCurrentMonthPercentByUserId(@Param("userId") Long userId);

    @Query(value="""
                WITH sum_lbp AS (SELECT bm.user_id, COALESCE(SUM(bm.amount),0) AS lbp_sum
                FROM wise_money.budget_management bm
                WHERE bm.type_allocation = 'LBP' AND bm.user_id = :userId
                GROUP BY bm.user_id),
                alloc_per_parent AS (SELECT bm.user_id, c_child.parent_id AS parent_id,
                SUM(CASE WHEN bm.type_allocation = 'LBP' THEN bm.amount
                WHEN bm.type_allocation = 'PERCENT' THEN GREATEST(COALESCE(b.amount,0) - COALESCE(sl.lbp_sum,0), 0) * bm.amount / 100 ELSE 0 END) AS parent_alloc
                FROM wise_money.budget_management bm
                JOIN wise_money.categories c_child ON c_child.id = bm.category_id
                LEFT JOIN wise_money.budgets b ON b.user_id = bm.user_id AND b.month = EXTRACT(MONTH FROM SYSDATE()) AND b.year = EXTRACT(YEAR FROM SYSDATE())
                LEFT JOIN sum_lbp sl ON sl.user_id = bm.user_id
                WHERE bm.user_id = :userId
                GROUP BY bm.user_id, c_child.parent_id),
                spend_by_parent AS (SELECT t.user_id, p.id AS parent_id, p.name AS parent_name, COALESCE(SUM(t.amount),0) AS spent
                FROM wise_money.transactions t
                JOIN wise_money.categories c_child ON c_child.id = t.category_id
                JOIN wise_money.categories p ON p.id = c_child.parent_id
                WHERE t.user_id = :userId AND c_child.type != 'REVENU' AND EXTRACT(MONTH FROM t.transaction_date) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM t.transaction_date) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY t.user_id, p.id, p.name)
                SELECT s.parent_id AS parentId, s.parent_name AS name, CASE WHEN ROUND(100 * s.spent / NULLIF(a.parent_alloc, 0), 0) > 100 THEN 100 ELSE ROUND(100 * s.spent / NULLIF(a.parent_alloc, 0), 0) END total
                FROM spend_by_parent s
                JOIN alloc_per_parent a ON a.user_id = s.user_id AND a.parent_id = s.parent_id
                ORDER BY total DESC;
                """, nativeQuery = true)
    List<ParentSpendView> findParentSpendViewByUserId(@Param("userId") Long userId);

    @Query(value="""
                WITH sum_lbp AS (SELECT bm.user_id, COALESCE(SUM(bm.amount),0) AS lbp_sum
                FROM wise_money.budget_management bm
                WHERE bm.type_allocation = 'LBP' AND bm.user_id = :userId
                GROUP BY bm.user_id),
                alloc_per_child AS (SELECT bm.user_id, bm.category_id AS child_id,
                SUM(CASE WHEN bm.type_allocation = 'LBP' THEN bm.amount
                WHEN bm.type_allocation = 'PERCENT' THEN GREATEST(COALESCE(b.amount,0) - COALESCE(sl.lbp_sum,0), 0) * bm.amount / 100 ELSE 0 END) AS child_alloc
                FROM wise_money.budget_management bm
                LEFT JOIN wise_money.budgets b ON b.user_id = bm.user_id AND b.month = EXTRACT(MONTH FROM SYSDATE()) AND b.year = EXTRACT(YEAR FROM SYSDATE())
                LEFT JOIN sum_lbp sl ON sl.user_id = bm.user_id
                WHERE bm.user_id = :userId
                GROUP BY bm.user_id, bm.category_id),
                spend_child AS (SELECT t.user_id, c.id AS child_id, c.parent_id AS parent_id, c.name AS name, COALESCE(SUM(t.amount),0) AS amount
                FROM wise_money.transactions t
                JOIN wise_money.categories c ON c.id = t.category_id
                WHERE t.user_id = :userId AND c.type = 'DEPENSE' AND EXTRACT(MONTH FROM t.transaction_date) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM t.transaction_date) = EXTRACT(YEAR FROM SYSDATE()) AND c.parent_id = :parentId
                GROUP BY t.user_id, child_id, parent_id, name)
                SELECT sc.child_id AS childId, sc.name AS name, sc.amount AS amount, case when apc.child_alloc = 0 then sc.amount else apc.child_alloc end total,
                ROUND(100 * sc.amount / case when apc.child_alloc = 0 then sc.amount else apc.child_alloc end, 0) AS percent
                FROM spend_child sc
                JOIN alloc_per_child apc ON apc.user_id = sc.user_id AND apc.child_id = sc.child_id
                ORDER BY percent DESC;
                """, nativeQuery = true)
    List<ChildPercentView> findChildrenSpendViewByUserId(@Param("userId") Long userId, @Param("parentId") Long parentId);

        @Query(value="""
        WITH sum_lbp AS (SELECT bm.user_id, COALESCE(SUM(bm.amount),0) AS lbp_sum
        FROM wise_money.budget_management bm
        WHERE bm.type_allocation = 'LBP'
        AND bm.user_id = :userId
        GROUP BY bm.user_id),
        alloc_total AS (SELECT bm.user_id,SUM(CASE WHEN bm.type_allocation = 'LBP' THEN bm.amount WHEN bm.type_allocation = 'PERCENT' THEN GREATEST(COALESCE(b.amount,0) - COALESCE(sl.lbp_sum,0), 0) * bm.amount / 100 ELSE 0 END) AS alloc_sum
        FROM wise_money.budget_management bm
        LEFT JOIN wise_money.budgets b ON b.user_id = bm.user_id AND b.month = EXTRACT(MONTH FROM SYSDATE()) AND b.year = EXTRACT(YEAR FROM SYSDATE())
        LEFT JOIN sum_lbp sl ON sl.user_id = bm.user_id
        WHERE bm.user_id = :userId
        GROUP BY bm.user_id),
        sum_dep AS (SELECT C.USER_ID, SUM(T.AMOUNT) AS DEPENSE
        FROM WISE_MONEY.TRANSACTIONS T
        INNER JOIN WISE_MONEY.CATEGORIES C ON C.ID = T.CATEGORY_ID
        INNER JOIN WISE_MONEY.BUDGET_MANAGEMENT B ON B.CATEGORY_ID = C.ID
        WHERE TYPE = 'DEPENSE' AND B.AMOUNT = 0 AND EXTRACT(MONTH FROM TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE()) AND EXTRACT(YEAR FROM TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
        GROUP BY USER_ID),
        sum_revenu AS (SELECT C.USER_ID, SUM(T.AMOUNT) AS REVENU
        FROM WISE_MONEY.TRANSACTIONS T
        INNER JOIN WISE_MONEY.CATEGORIES C ON C.ID = T.CATEGORY_ID
        WHERE TYPE = 'REVENU' AND EXTRACT(MONTH FROM TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE()) AND EXTRACT(YEAR FROM TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
        GROUP BY USER_ID)
        SELECT COALESCE(b.amount,0) - COALESCE(a.alloc_sum,0) - COALESCE(sd.depense,0) + COALESCE(sr.revenu,0) AS epargne
        FROM wise_money.budgets b
        LEFT JOIN alloc_total a ON a.user_id = b.user_id
        LEFT JOIN sum_revenu sr ON sr.user_id = b.user_id
        LEFT JOIN sum_dep sd ON sd.user_id = b.user_id
        WHERE b.user_id = :userId AND b.month = EXTRACT(MONTH FROM SYSDATE()) AND b.year = EXTRACT(YEAR FROM SYSDATE());
        """, nativeQuery = true)
        Integer getEpargneOfMonth(@Param("userId") Long userId);

        @Query(value="""
        WITH CURRENT_MONTH AS (SELECT T.USER_ID, COALESCE(SUM(AMOUNT),0) AS CURRENT_AMOUNT
        FROM WISE_MONEY.TRANSACTIONS T
        INNER JOIN WISE_MONEY.CATEGORIES C ON C.ID = T.CATEGORY_ID
        WHERE T.USER_ID = :userId AND EXTRACT(MONTH FROM TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
        AND EXTRACT(YEAR FROM TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE()) AND C.TYPE = 'DEPENSE'
        GROUP BY T.USER_ID),
        PAST_MONTH AS (SELECT T.USER_ID, COALESCE(SUM(AMOUNT), 1) AS PAST_AMOUNT
        FROM WISE_MONEY.TRANSACTIONS T
        INNER JOIN WISE_MONEY.CATEGORIES C ON C.ID = T.CATEGORY_ID
        WHERE T.USER_ID = :userId AND C.TYPE = 'DEPENSE' AND ((EXTRACT(MONTH FROM TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE()) - 1 AND EXTRACT(MONTH FROM SYSDATE()) != 1
        AND EXTRACT(YEAR FROM TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE()))
        OR (EXTRACT(MONTH FROM SYSDATE()) = 1 AND EXTRACT(MONTH FROM TRANSACTION_DATE) = 12 AND EXTRACT(YEAR FROM TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE()) - 1))
        GROUP BY T.USER_ID)
        SELECT CM.CURRENT_AMOUNT, ROUND(CM.CURRENT_AMOUNT * 100 / PM.PAST_AMOUNT - 100, 0) AS GAP
        FROM CURRENT_MONTH CM
        INNER JOIN PAST_MONTH PM ON PM.USER_ID = CM.USER_ID;
        """, nativeQuery = true)
        List<PercentGap> getGapOfMonth(@Param("userId") Long userId);

        @Query(value="""
        SELECT MONTH, YEAR, SUM(CASE WHEN TYPE = 'DEPENSE' THEN AMOUNT ELSE 0 END) AS EXPENSES, SUM(CASE WHEN TYPE = 'REVENU' THEN AMOUNT ELSE 0 END) AS REVENUES
        FROM (SELECT EXTRACT(MONTH FROM TRANSACTION_DATE) AS MONTH, EXTRACT(YEAR FROM TRANSACTION_DATE) AS YEAR, AMOUNT, TYPE
        FROM WISE_MONEY.TRANSACTIONS T
        INNER JOIN WISE_MONEY.CATEGORIES C ON C.ID = T.CATEGORY_ID
        WHERE T.USER_ID = :userId) SUB
        GROUP BY MONTH, YEAR;
        """, nativeQuery = true)
        List<StatisticsViews> getExpensesRevenuesDifference(@Param("userId") Long userId);
}
