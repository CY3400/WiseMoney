package com.charbel.backend.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.charbel.backend.model.Budget;
import com.charbel.backend.model.Users;

@Repository
public interface BudgetRepo extends JpaRepository<Budget, Long> {
    boolean existsByUserAndMonthAndYear(Users user, int month, int year);

    Optional<Budget> findByUserAndYearAndMonth(Users user, int year, int month);

    Optional<Budget> findTopByUserOrderByYearDescMonthDesc(Users user);
}
