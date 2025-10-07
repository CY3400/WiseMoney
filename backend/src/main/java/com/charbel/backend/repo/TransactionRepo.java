package com.charbel.backend.repo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
