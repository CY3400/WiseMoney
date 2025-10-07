package com.charbel.backend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.charbel.backend.model.BudgetManagement;
import com.charbel.backend.model.Users;

@Repository
public interface BudgetManagementRepo extends JpaRepository<BudgetManagement, Long> {
    Optional<BudgetManagement> findByIdAndUser(Long id, Users user);

    List<BudgetManagement> findByUser(Users user);
}