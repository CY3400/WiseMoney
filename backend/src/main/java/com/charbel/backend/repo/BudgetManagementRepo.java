package com.charbel.backend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.charbel.backend.model.BudgetManagement;
import com.charbel.backend.model.Users;

@Repository
public interface BudgetManagementRepo extends JpaRepository<BudgetManagement, Long> {
    Optional<BudgetManagement> findByIdAndUser(Long id, Users user);

    @Query(value="""
        SELECT BM.* FROM BUDGET_MANAGEMENT BM
        INNER JOIN CATEGORIES C ON C.ID = BM.CATEGORY_ID
        WHERE C.USER_ID = :userId AND C.STATUS = :status
        """, nativeQuery = true)
    List<BudgetManagement> findByUserAndStatus(@Param("userId") Long userId, @Param("status") int status);

    @Modifying
    @Query("update BudgetManagement b set b.amount = 0 where b.category.id = :categoryId")
    void resetAmountToZeroByCategoryId(@Param("categoryId") Long categoryId);
}