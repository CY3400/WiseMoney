package com.charbel.backend.repo;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.charbel.backend.model.Objectives;
import com.charbel.backend.model.Users;

public interface ObjectivesRepo extends JpaRepository<Objectives, Long> {
    boolean existsByUserAndMonthAndYear(Users user, int month, int year);

    List<Objectives> findByUserOrderByYearDescMonthDesc(Users user);

    @Query(value = """
    SELECT SUM(OBJECTIF) AS OBJECTIF
    FROM OBJECTIVES
    WHERE USER_ID = :userId;
    """, nativeQuery = true)
    BigDecimal getSumObjectives(@Param("userId") Long userId);
}
