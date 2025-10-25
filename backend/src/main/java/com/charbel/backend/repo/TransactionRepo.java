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
                WITH SUM_LBP AS (
                SELECT BM.USER_ID, SUM(BM.AMOUNT) AS SM
                FROM WISE_MONEY.BUDGET_MANAGEMENT BM
                WHERE TYPE_ALLOCATION = 'LBP' AND BM.USER_ID = :userId
                GROUP BY BM.USER_ID
                )
                SELECT SUB.ID AS PARENTID, SUB.NAME, ROUND(SUM(T.AMOUNT)/(CASE WHEN SUB.TYPE_ALLOCATION = 'LBP' AND SUB.AMOUNT > 0 THEN SUB.AMOUNT WHEN SUB.TYPE_ALLOCATION = 'PERCENT' AND (AM - SUM) > 0 THEN (AM - SUM)*SUB.AMOUNT/100 END)*100,0) AS TOTAL
                FROM (SELECT C.ID, C.NAME, BM.TYPE_ALLOCATION, BM.AMOUNT, BM.CATEGORY_ID, COALESCE(SM, 0) AS SUM, COALESCE(B.AMOUNT, 0) AS AM
                FROM WISE_MONEY.BUDGET_MANAGEMENT BM
                INNER JOIN WISE_MONEY.CATEGORIES C ON C.ID = BM.CATEGORY_ID
                LEFT JOIN WISE_MONEY.BUDGETS B ON B.USER_ID = BM.USER_ID
                LEFT JOIN SUM_LBP SL ON SL.USER_ID = BM.USER_ID
                WHERE BM.USER_ID = :userId  AND EXTRACT(MONTH FROM SYSDATE()) = MONTH AND EXTRACT(YEAR FROM SYSDATE()) = YEAR) SUB
                INNER JOIN WISE_MONEY.CATEGORIES C ON C.PARENT_ID = SUB.CATEGORY_ID
                INNER JOIN WISE_MONEY.TRANSACTIONS T ON T.CATEGORY_ID = C.ID
                WHERE EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE()) AND EXTRACT(YEAR FROM T.TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
                GROUP BY SUB.ID, SUB.NAME, SUB.TYPE_ALLOCATION, SUB.AMOUNT, SUB.AM
                    """, nativeQuery = true)
    List<ParentSpendView> findParentSpendViewByUserId(@Param("userId") Long userId);

    @Query(value="""
                WITH SPEND_CHILD AS (
                SELECT C.PARENT_ID, C.ID AS CHILD_ID, C.NAME AS NAME, COALESCE(SUM(T.AMOUNT), 0) AS AMOUNT
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId AND C.TYPE = 'DEPENSE'
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR  FROM T.TRANSACTION_DATE) = EXTRACT(YEAR  FROM SYSDATE())
                AND C.PARENT_ID = :parentId
                GROUP BY C.PARENT_ID, C.ID, C.NAME
                ),
                SPEND_PARENT AS (
                SELECT C.PARENT_ID, COALESCE(SUM(T.AMOUNT), 0) AS TOTAL
                FROM TRANSACTIONS T
                INNER JOIN CATEGORIES C ON C.ID = T.CATEGORY_ID
                WHERE T.USER_ID = :userId AND C.TYPE = 'DEPENSE'
                AND EXTRACT(MONTH FROM T.TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
                AND EXTRACT(YEAR  FROM T.TRANSACTION_DATE) = EXTRACT(YEAR  FROM SYSDATE())
                AND C.PARENT_ID = :parentId
                GROUP BY C.PARENT_ID
                )
                SELECT SC.NAME, SC.AMOUNT, SP.TOTAL, ROUND(100 * SC.AMOUNT / NULLIF(SP.TOTAL, 0), 0) AS PERCENT
                FROM SPEND_CHILD SC
                INNER JOIN SPEND_PARENT SP ON SP.PARENT_ID = SC.PARENT_ID
                ORDER BY PERCENT DESC;
                """, nativeQuery = true)
    List<ChildPercentView> findChildrenSpendViewByUserId(@Param("userId") Long userId, @Param("parentId") Long parentId);
}
