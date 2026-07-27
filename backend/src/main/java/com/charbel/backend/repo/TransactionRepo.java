package com.charbel.backend.repo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.charbel.backend.DTO.ChildPercentView;
import com.charbel.backend.DTO.DeltaTransactions;
import com.charbel.backend.DTO.ParentSpendView;
import com.charbel.backend.DTO.PercentGap;
import com.charbel.backend.DTO.StatisticsViews;
import com.charbel.backend.model.CategoryType;
import com.charbel.backend.model.Transaction;
import com.charbel.backend.model.Users;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdAndUser(Long id, Users user);

    @Query("""
            select t from Transaction t
            join fetch t.category c
            left join fetch c.parent p
            where t.user = :user
            order by t.id desc
            """)
    List<Transaction> findByUserOrderByIdDesc(@Param("user") Users user);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user = :user
            AND t.transactionDate >= :start
            AND t.transactionDate < :end
            AND t.category.type = :type
            """)
    BigDecimal sumByUserAndMonthYearAndType(
            @Param("user") Users user,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("type") CategoryType type
    );

    @Query(value = """
            WITH TOTALS AS (
                SELECT
                    T.USER_ID,
                    COALESCE(SUM(CASE WHEN C.TYPE = 'DEPENSE' THEN T.AMOUNT ELSE 0 END), 0) AS DEPENSE,
                    COALESCE(SUM(CASE WHEN C.TYPE = 'REVENU' THEN T.AMOUNT ELSE 0 END), 0) AS REVENU
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY T.USER_ID
            )
            SELECT
                CASE
                    WHEN COALESCE(T.REVENU, 0) + COALESCE(B.AMOUNT, 0) = 0 THEN 0
                    ELSE ROUND(COALESCE(T.DEPENSE, 0) / (COALESCE(T.REVENU, 0) + COALESCE(B.AMOUNT, 0)) * 100, 0)
                END AS PERCENT
            FROM BUDGETS B
            LEFT JOIN TOTALS T ON T.USER_ID = B.USER_ID
            WHERE B.USER_ID = :userId
            AND B.MONTH = EXTRACT(MONTH FROM SYSDATE())
            AND B.YEAR = EXTRACT(YEAR FROM SYSDATE())
            """, nativeQuery = true)
    Integer findCurrentMonthPercentByUserId(@Param("userId") Long userId);

    @Query(value = """
            WITH CURRENT_BUDGET AS (
                SELECT USER_ID, AMOUNT
                FROM BUDGETS
                WHERE USER_ID = :userId
                AND MONTH = EXTRACT(MONTH FROM SYSDATE())
                AND YEAR = EXTRACT(YEAR FROM SYSDATE())
            ),
            REVENU AS (
                SELECT T.USER_ID, COALESCE(SUM(T.AMOUNT), 0) AS TR
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND C.TYPE = 'REVENU'
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY T.USER_ID
            ),
            DEPENSES_ZERO AS (
                SELECT C.USER_ID, COALESCE(SUM(T.AMOUNT), 0) AS TD
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE C.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND C.AMOUNT = 0
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY C.USER_ID
            ),
            TOTAL_EXCES AS (
                SELECT USER_ID, COALESCE(SUM(GREATEST(T_AMOUNT - AMOUNT, 0)), 0) AS TOTAL
                FROM (
                    SELECT C.USER_ID, C.ID, C.AMOUNT, COALESCE(SUM(T.AMOUNT), 0) AS T_AMOUNT
                    FROM CATEGORIES C
                    INNER JOIN TRANSACTIONS T ON T.CATEGORY_ID = C.ID
                    WHERE C.USER_ID = :userId
                    AND C.TYPE = 'DEPENSE'
                    AND C.AMOUNT > 0
                    AND C.TYPE_ALLOCATION = 'LBP'
                    AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                    AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                    GROUP BY C.USER_ID, C.ID, C.AMOUNT
                ) SUB
                GROUP BY USER_ID
            ),
            TOTAL_BUDGET AS (
                SELECT
                    B.USER_ID,
                    COALESCE(B.AMOUNT, 0)
                    + COALESCE(R.TR, 0)
                    - COALESCE(DZ.TD, 0)
                    - COALESCE(TE.TOTAL, 0) AS TOTAL
                FROM CURRENT_BUDGET B
                LEFT JOIN REVENU R ON R.USER_ID = B.USER_ID
                LEFT JOIN DEPENSES_ZERO DZ ON DZ.USER_ID = B.USER_ID
                LEFT JOIN TOTAL_EXCES TE ON TE.USER_ID = B.USER_ID
            ),
            CURRENT_TX AS (
                SELECT T.USER_ID, T.CATEGORY_ID, COALESCE(SUM(T.AMOUNT), 0) AS TOTAL_TRANSACTIONS
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY T.USER_ID, T.CATEGORY_ID
            ),
            CHILD_BUDGET AS (
                SELECT
                    C2.ID AS PARENT_ID,
                    C2.NAME AS PARENT_NAME,
                    C.ID AS CHILD_ID,
                    C.NAME AS CHILD_NAME,
                    COALESCE(CT.TOTAL_TRANSACTIONS, 0) AS TOTAL_TRANSACTIONS,
                    CASE WHEN C.AMOUNT > 0 THEN 1 ELSE 0 END AS TOTAL_UP,
                    CASE
                        WHEN C.TYPE_ALLOCATION = 'LBP'
                            THEN GREATEST(C.AMOUNT, COALESCE(CT.TOTAL_TRANSACTIONS, 0))
                        ELSE GREATEST(COALESCE(CT.TOTAL_TRANSACTIONS, 0), ROUND(COALESCE(TB.TOTAL, 0) * C.AMOUNT / 100, 0))
                    END AS TOTAL_BM
                FROM CATEGORIES C
                INNER JOIN CATEGORIES C2 ON C2.ID = C.PARENT_ID
                LEFT JOIN CURRENT_TX CT ON CT.CATEGORY_ID = C.ID
                LEFT JOIN TOTAL_BUDGET TB ON TB.USER_ID = C.USER_ID
                WHERE C.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND C.PARENT_ID IS NOT NULL
                AND (C.AMOUNT > 0 OR COALESCE(CT.TOTAL_TRANSACTIONS, 0) > 0)
            )
            SELECT
                PARENT_ID AS parentId,
                PARENT_NAME AS name,
                CASE
                    WHEN COALESCE(SUM(TOTAL_BM), 0) = 0 THEN 0
                    ELSE ROUND(SUM(TOTAL_TRANSACTIONS) / SUM(TOTAL_BM) * 100, 0)
                END AS total,
                SUM(TOTAL_UP) AS count,
                SUM(TOTAL_TRANSACTIONS) AS transactions,
                SUM(TOTAL_BM) AS budget
            FROM CHILD_BUDGET
            GROUP BY PARENT_ID, PARENT_NAME
            HAVING total > 0
            """, nativeQuery = true)
    List<ParentSpendView> findParentSpendViewByUserId(@Param("userId") Long userId);

    @Query(value = """
            WITH CURRENT_BUDGET AS (
                SELECT USER_ID, AMOUNT
                FROM BUDGETS
                WHERE USER_ID = :userId
                AND MONTH = EXTRACT(MONTH FROM SYSDATE())
                AND YEAR = EXTRACT(YEAR FROM SYSDATE())
            ),
            REVENU AS (
                SELECT T.USER_ID, COALESCE(SUM(T.AMOUNT), 0) AS TR
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND C.TYPE = 'REVENU'
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY T.USER_ID
            ),
            DEPENSES_ZERO AS (
                SELECT C.USER_ID, COALESCE(SUM(T.AMOUNT), 0) AS TD
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE C.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND C.AMOUNT = 0
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY C.USER_ID
            ),
            TOTAL_EXCES AS (
                SELECT USER_ID, COALESCE(SUM(GREATEST(T_AMOUNT - AMOUNT, 0)), 0) AS TOTAL
                FROM (
                    SELECT C.USER_ID, C.ID, C.AMOUNT, COALESCE(SUM(T.AMOUNT), 0) AS T_AMOUNT
                    FROM CATEGORIES C
                    INNER JOIN TRANSACTIONS T ON T.CATEGORY_ID = C.ID
                    WHERE C.USER_ID = :userId
                    AND C.TYPE = 'DEPENSE'
                    AND C.AMOUNT > 0
                    AND C.TYPE_ALLOCATION = 'LBP'
                    AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                    AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                    GROUP BY C.USER_ID, C.ID, C.AMOUNT
                ) SUB
                GROUP BY USER_ID
            ),
            TOTAL_BUDGET AS (
                SELECT
                    B.USER_ID,
                    COALESCE(B.AMOUNT, 0)
                    + COALESCE(R.TR, 0)
                    - COALESCE(DZ.TD, 0)
                    - COALESCE(TE.TOTAL, 0) AS TOTAL
                FROM CURRENT_BUDGET B
                LEFT JOIN REVENU R ON R.USER_ID = B.USER_ID
                LEFT JOIN DEPENSES_ZERO DZ ON DZ.USER_ID = B.USER_ID
                LEFT JOIN TOTAL_EXCES TE ON TE.USER_ID = B.USER_ID
            ),
            CURRENT_TX AS (
                SELECT T.USER_ID, T.CATEGORY_ID, COALESCE(SUM(T.AMOUNT), 0) AS TOTAL_TRANSACTIONS
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY T.USER_ID, T.CATEGORY_ID
            )
            SELECT
                childId,
                name,
                total,
                budget,
                CASE
                    WHEN budget = 0 THEN 0
                    ELSE ROUND(total / budget * 100, 0)
                END AS percent,
                amount
            FROM (
                SELECT
                    C.ID AS childId,
                    C.NAME AS name,
                    COALESCE(CT.TOTAL_TRANSACTIONS, 0) AS total,
                    CASE
                        WHEN C.TYPE_ALLOCATION = 'LBP'
                            THEN GREATEST(C.AMOUNT, COALESCE(CT.TOTAL_TRANSACTIONS, 0))
                        ELSE GREATEST(COALESCE(CT.TOTAL_TRANSACTIONS, 0), ROUND(COALESCE(TB.TOTAL, 0) * C.AMOUNT / 100, 0))
                    END AS budget,
                    C.AMOUNT AS amount
                FROM CATEGORIES C
                LEFT JOIN CURRENT_TX CT ON CT.CATEGORY_ID = C.ID
                LEFT JOIN TOTAL_BUDGET TB ON TB.USER_ID = C.USER_ID
                WHERE C.USER_ID = :userId
                AND C.PARENT_ID = :parentId
                AND C.TYPE = 'DEPENSE'
                AND (C.AMOUNT > 0 OR COALESCE(CT.TOTAL_TRANSACTIONS, 0) > 0)
            ) SUB
            """, nativeQuery = true)
    List<ChildPercentView> findChildrenSpendViewByUserId(
            @Param("userId") Long userId,
            @Param("parentId") Long parentId
    );

    @Query(value = """
            WITH CURRENT_BUDGET AS (
                SELECT USER_ID, AMOUNT
                FROM BUDGETS
                WHERE USER_ID = :userId
                AND MONTH = EXTRACT(MONTH FROM SYSDATE())
                AND YEAR = EXTRACT(YEAR FROM SYSDATE())
            ),
            REVENU AS (
                SELECT T.USER_ID, COALESCE(SUM(T.AMOUNT), 0) AS TR
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND C.TYPE = 'REVENU'
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY T.USER_ID
            ),
            DEPENSES_ZERO AS (
                SELECT C.USER_ID, COALESCE(SUM(T.AMOUNT), 0) AS TD
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE C.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND C.AMOUNT = 0
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY C.USER_ID
            ),
            TOTAL_EXCES AS (
                SELECT USER_ID, COALESCE(SUM(GREATEST(T_AMOUNT - AMOUNT, 0)), 0) AS TOTAL
                FROM (
                    SELECT C.USER_ID, C.ID, C.AMOUNT, COALESCE(SUM(T.AMOUNT), 0) AS T_AMOUNT
                    FROM CATEGORIES C
                    INNER JOIN TRANSACTIONS T ON T.CATEGORY_ID = C.ID
                    WHERE C.USER_ID = :userId
                    AND C.TYPE = 'DEPENSE'
                    AND C.AMOUNT > 0
                    AND C.TYPE_ALLOCATION = 'LBP'
                    AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                    AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                    GROUP BY C.USER_ID, C.ID, C.AMOUNT
                ) SUB
                GROUP BY USER_ID
            ),
            TOTAL_BUDGET AS (
                SELECT
                    B.USER_ID,
                    COALESCE(B.AMOUNT, 0)
                    + COALESCE(R.TR, 0)
                    - COALESCE(DZ.TD, 0)
                    - COALESCE(TE.TOTAL, 0) AS TOTAL
                FROM CURRENT_BUDGET B
                LEFT JOIN REVENU R ON R.USER_ID = B.USER_ID
                LEFT JOIN DEPENSES_ZERO DZ ON DZ.USER_ID = B.USER_ID
                LEFT JOIN TOTAL_EXCES TE ON TE.USER_ID = B.USER_ID
            ),
            CURRENT_TX AS (
                SELECT T.USER_ID, T.CATEGORY_ID, COALESCE(SUM(T.AMOUNT), 0) AS TOTAL_TRANSACTIONS
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY T.USER_ID, T.CATEGORY_ID
            ),
            TOTAL_LBP AS (
                SELECT
                    C.USER_ID,
                    COALESCE(SUM(GREATEST(C.AMOUNT, COALESCE(CT.TOTAL_TRANSACTIONS, 0))), 0) AS TOTAL
                FROM CATEGORIES C
                LEFT JOIN CURRENT_TX CT ON CT.CATEGORY_ID = C.ID
                WHERE C.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND C.TYPE_ALLOCATION = 'LBP'
                AND (C.AMOUNT > 0 OR COALESCE(CT.TOTAL_TRANSACTIONS, 0) > 0)
                GROUP BY C.USER_ID
            ),
            TOTAL_PERCENT AS (
                SELECT
                    C.USER_ID,
                    COALESCE(SUM(GREATEST(COALESCE(CT.TOTAL_TRANSACTIONS, 0), ROUND(COALESCE(TB.TOTAL, 0) * C.AMOUNT / 100, 0))), 0) AS TOTAL
                FROM CATEGORIES C
                LEFT JOIN CURRENT_TX CT ON CT.CATEGORY_ID = C.ID
                LEFT JOIN TOTAL_BUDGET TB ON TB.USER_ID = C.USER_ID
                WHERE C.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND C.TYPE_ALLOCATION = 'PERCENT'
                AND (C.AMOUNT > 0 OR COALESCE(CT.TOTAL_TRANSACTIONS, 0) > 0)
                GROUP BY C.USER_ID
            )
            SELECT
                CAST(
                    COALESCE(B.AMOUNT, 0)
                    + COALESCE(R.TR, 0)
                    - COALESCE(TL.TOTAL, 0)
                    - COALESCE(TP.TOTAL, 0)
                AS SIGNED) AS EPARGNE
            FROM CURRENT_BUDGET B
            LEFT JOIN REVENU R ON R.USER_ID = B.USER_ID
            LEFT JOIN TOTAL_LBP TL ON TL.USER_ID = B.USER_ID
            LEFT JOIN TOTAL_PERCENT TP ON TP.USER_ID = B.USER_ID
            """, nativeQuery = true)
    Integer getEpargneOfMonth(@Param("userId") Long userId);

    @Query(value = """
            WITH CURRENT_MONTH AS (
                SELECT T.USER_ID, COALESCE(SUM(T.AMOUNT), 0) AS CURRENT_AMOUNT
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY T.USER_ID
            ),
            PAST_MONTH AS (
                SELECT T.USER_ID, COALESCE(SUM(T.AMOUNT), 1) AS PAST_AMOUNT
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND (
                    (
                        EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE()) - 1
                        AND EXTRACT(MONTH FROM SYSDATE()) != 1
                        AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                    )
                    OR
                    (
                        EXTRACT(MONTH FROM SYSDATE()) = 1
                        AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = 12
                        AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE()) - 1
                    )
                )
                GROUP BY T.USER_ID
            )
            SELECT
                CM.CURRENT_AMOUNT AS currentAmount,
                ROUND(CM.CURRENT_AMOUNT * 100 / PM.PAST_AMOUNT - 100, 0) AS gap
            FROM CURRENT_MONTH CM
            INNER JOIN PAST_MONTH PM ON PM.USER_ID = CM.USER_ID
            """, nativeQuery = true)
    List<PercentGap> getGapOfMonth(@Param("userId") Long userId);

    @Query(value = """
            SELECT
                MONTH,
                YEAR,
                SUM(CASE WHEN TYPE = 'DEPENSE' THEN AMOUNT ELSE 0 END) AS EXPENSES,
                SUM(CASE WHEN TYPE = 'REVENU' THEN AMOUNT ELSE 0 END) AS REVENUES
            FROM (
                SELECT
                    EXTRACT(MONTH FROM T.TRANSACTION_DATE) AS MONTH,
                    EXTRACT(YEAR FROM T.TRANSACTION_DATE) AS YEAR,
                    T.AMOUNT AS AMOUNT,
                    C.TYPE AS TYPE
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
            ) SUB
            GROUP BY MONTH, YEAR
            """, nativeQuery = true)
    List<StatisticsViews> getExpensesRevenuesDifference(@Param("userId") Long userId);

    @Query(value = """
            SELECT *
            FROM TRANSACTIONS
            WHERE USER_ID = :userId
            AND TRANSACTION_DATE >= :startDate
            AND TRANSACTION_DATE < :endDate
            """, nativeQuery = true)
    List<Transaction> findByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
            WITH CURRENT_MONTH AS (
                SELECT T.CATEGORY_ID, C.NAME, COALESCE(SUM(T.AMOUNT), 0) AS AMOUNT
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM NOW())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM NOW())
                GROUP BY T.CATEGORY_ID, C.NAME
            ),
            PRIOR_MONTH AS (
                SELECT T.CATEGORY_ID, C.NAME, COALESCE(SUM(T.AMOUNT), 0) AS AMOUNT
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND (
                    (
                        EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM NOW()) - 1
                        AND EXTRACT(MONTH FROM NOW()) != 1
                        AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM NOW())
                    )
                    OR
                    (
                        EXTRACT(MONTH FROM T.TRANSACTION_DATE) = 12
                        AND EXTRACT(MONTH FROM NOW()) = 1
                        AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM NOW()) - 1
                    )
                )
                GROUP BY T.CATEGORY_ID, C.NAME
            )
            SELECT
                CM.NAME AS name,
                CM.AMOUNT - PM.AMOUNT AS delta
            FROM CURRENT_MONTH CM
            INNER JOIN PRIOR_MONTH PM ON PM.CATEGORY_ID = CM.CATEGORY_ID
            WHERE CM.AMOUNT - PM.AMOUNT > 0
            ORDER BY delta DESC
            LIMIT 3
            """, nativeQuery = true)
    List<DeltaTransactions> findDeltaTransactions(@Param("userId") Long userId);

    @Query(value = """
            SELECT CAST(
                ROUND(
                    COALESCE(SUM(AMOUNT), 0) / (DATEDIFF(LAST_DAY(SYSDATE()), SYSDATE()) + 1),
                    0
                )
            AS SIGNED) AS TOTAL
            FROM (
                SELECT B.AMOUNT AS AMOUNT
                FROM BUDGETS B
                WHERE B.USER_ID = :userId
                AND B.MONTH = EXTRACT(MONTH FROM SYSDATE())
                AND B.YEAR = EXTRACT(YEAR FROM SYSDATE())

                UNION ALL

                SELECT COALESCE(SUM(T.AMOUNT), 0) AS AMOUNT
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE C.TYPE = 'REVENU'
                AND C.USER_ID = :userId
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())

                UNION ALL

                SELECT COALESCE(SUM(C.AMOUNT), 0) * (-1) AS AMOUNT
                FROM CATEGORIES C
                WHERE C.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND C.FREQUENCY = 1

                UNION ALL

                SELECT COALESCE(SUM(T.AMOUNT), 0) * (-1) AS AMOUNT
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE C.FREQUENCY = 2
                AND C.TYPE = 'DEPENSE'
                AND C.USER_ID = :userId
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                AND T.TRANSACTION_DATE <= CURRENT_DATE()
            ) SUB
            """, nativeQuery = true)
    Integer getAverageDaily(@Param("userId") Long userId);

    @Query(value = """
            SELECT COALESCE(SUM(T.AMOUNT), 0) AS TOTAL
            FROM TRANSACTIONS T
            INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
            WHERE C.TYPE = 'REVENU'
            AND C.USER_ID = :userId
            AND EXTRACT(MONTH FROM SYSDATE()) = EXTRACT(MONTH FROM T.TRANSACTION_DATE)
            AND EXTRACT(YEAR FROM SYSDATE()) = EXTRACT(YEAR FROM T.TRANSACTION_DATE)
            """, nativeQuery = true)
    Integer getSumRevenues(@Param("userId") Long userId);

    @Query(value = """
            SELECT COALESCE(SUM(T.AMOUNT), 0) AS TOTAL
            FROM TRANSACTIONS T
            INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
            WHERE C.TYPE = 'DEPENSE'
            AND C.USER_ID = :userId
            AND EXTRACT(MONTH FROM SYSDATE()) = EXTRACT(MONTH FROM T.TRANSACTION_DATE)
            AND EXTRACT(YEAR FROM SYSDATE()) = EXTRACT(YEAR FROM T.TRANSACTION_DATE)
            """, nativeQuery = true)
    Integer getSumDepenses(@Param("userId") Long userId);

    @Query(value = """
            WITH CURRENT_BUDGET AS (
                SELECT USER_ID, AMOUNT
                FROM BUDGETS
                WHERE USER_ID = :userId
                AND MONTH = EXTRACT(MONTH FROM SYSDATE())
                AND YEAR = EXTRACT(YEAR FROM SYSDATE())
            ),
            REVENU AS (
                SELECT T.USER_ID, COALESCE(SUM(T.AMOUNT), 0) AS TR
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND C.TYPE = 'REVENU'
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY T.USER_ID
            ),
            DEPENSES_ZERO AS (
                SELECT C.USER_ID, COALESCE(SUM(T.AMOUNT), 0) AS TD
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE C.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND C.AMOUNT = 0
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY C.USER_ID
            ),
            TOTAL_EXCES AS (
                SELECT USER_ID, COALESCE(SUM(GREATEST(T_AMOUNT - AMOUNT, 0)), 0) AS TOTAL
                FROM (
                    SELECT C.USER_ID, C.ID, C.AMOUNT, COALESCE(SUM(T.AMOUNT), 0) AS T_AMOUNT
                    FROM CATEGORIES C
                    INNER JOIN TRANSACTIONS T ON T.CATEGORY_ID = C.ID
                    WHERE C.USER_ID = :userId
                    AND C.TYPE = 'DEPENSE'
                    AND C.AMOUNT > 0
                    AND C.TYPE_ALLOCATION = 'LBP'
                    AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                    AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                    GROUP BY C.USER_ID, C.ID, C.AMOUNT
                ) SUB
                GROUP BY USER_ID
            ),
            TOTAL_BUDGET AS (
                SELECT
                    B.USER_ID,
                    COALESCE(B.AMOUNT, 0)
                    + COALESCE(R.TR, 0)
                    - COALESCE(DZ.TD, 0)
                    - COALESCE(TE.TOTAL, 0) AS TOTAL
                FROM CURRENT_BUDGET B
                LEFT JOIN REVENU R ON R.USER_ID = B.USER_ID
                LEFT JOIN DEPENSES_ZERO DZ ON DZ.USER_ID = B.USER_ID
                LEFT JOIN TOTAL_EXCES TE ON TE.USER_ID = B.USER_ID
            ),
            CURRENT_TX AS (
                SELECT T.USER_ID, T.CATEGORY_ID, COALESCE(SUM(T.AMOUNT), 0) AS TOTAL_TRANSACTIONS
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY T.USER_ID, T.CATEGORY_ID
            )
            SELECT CAST(COALESCE(SUM(TOTAL_BM - TOTAL_TRANSACTIONS), 0) AS SIGNED) AS TOTAL
            FROM (
                SELECT
                    C2.ID AS PARENT_ID,
                    C2.NAME AS PARENT_NAME,
                    C.ID AS CHILD_ID,
                    C.NAME AS CHILD_NAME,
                    COALESCE(CT.TOTAL_TRANSACTIONS, 0) AS TOTAL_TRANSACTIONS,
                    CASE
                        WHEN C.TYPE_ALLOCATION = 'LBP'
                            THEN GREATEST(C.AMOUNT, COALESCE(CT.TOTAL_TRANSACTIONS, 0))
                        ELSE GREATEST(COALESCE(CT.TOTAL_TRANSACTIONS, 0), ROUND(COALESCE(TB.TOTAL, 0) * C.AMOUNT / 100, 0))
                    END AS TOTAL_BM
                FROM CATEGORIES C
                INNER JOIN CATEGORIES C2 ON C2.ID = C.PARENT_ID
                LEFT JOIN CURRENT_TX CT ON CT.CATEGORY_ID = C.ID
                LEFT JOIN TOTAL_BUDGET TB ON TB.USER_ID = C.USER_ID
                WHERE C.USER_ID = :userId
                AND C.TYPE = 'DEPENSE'
                AND C.PARENT_ID IS NOT NULL
                AND (C.AMOUNT > 0 OR COALESCE(CT.TOTAL_TRANSACTIONS, 0) > 0)
            ) SUB
            """, nativeQuery = true)
    Integer getSumDepFuture(@Param("userId") Long userId);
}