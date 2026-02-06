package com.charbel.backend.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.charbel.backend.model.Objectives;
import com.charbel.backend.model.Users;

@Repository
public interface ObjectivesRepo extends JpaRepository<Objectives, Long> {
    boolean existsByUserAndMonthAndYear(Users user, int month, int year);

    List<Objectives> findByUserOrderByYearDescMonthDesc(Users user);

    @Query(value = """
    SELECT SUM(OBJECTIF) AS OBJECTIF
    FROM OBJECTIVES
    WHERE USER_ID = :userId;
    """, nativeQuery = true)
    Integer getSumObjectives(@Param("userId") Long userId);
}
