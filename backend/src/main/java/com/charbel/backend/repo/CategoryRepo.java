package com.charbel.backend.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.charbel.backend.DTO.ExpDiffDTO;
import com.charbel.backend.DTO.TopExpensesByMonth;
import com.charbel.backend.model.Category;
import com.charbel.backend.model.CategoryType;
import com.charbel.backend.model.Users;

import java.util.List;
import java.util.Optional;

public interface CategoryRepo extends JpaRepository<Category, Long> {
  boolean existsByUserAndNameIgnoreCase(Users user, String name);

  List<Category> findByUserOrderByNameAsc(Users user);

  Optional<Category> findByIdAndUser(Long id, Users user);

  List<Category> findByUserAndParentIsNullOrderByNameAsc(Users user);
  List<Category> findByUserAndParentIsNullAndStatusOrderByNameAsc(Users user, int status);

  List<Category> findByUserAndParentIsNullAndTypeInOrderByNameAsc(Users user, List<CategoryType> types);
  List<Category> findByUserAndParentIsNullAndStatusAndTypeInOrderByNameAsc(Users user, int status, List<CategoryType> types);

  List<Category> findByUserAndParentIsNotNullOrderByNameAsc(Users user);
  List<Category> findByUserAndParentIsNotNullAndStatusOrderByNameAsc(Users user, int status);

  @Query(value="""
        SELECT C.NAME, SUM(T.AMOUNT) AS TOTAL
        FROM CATEGORIES C
        INNER JOIN TRANSACTIONS T ON T.CATEGORY_ID = C.ID
        WHERE C.USER_ID = :userId AND C.TYPE = 'DEPENSE' AND EXTRACT(MONTH FROM TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
        AND EXTRACT(YEAR FROM TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
        GROUP BY C.NAME
        ORDER BY TOTAL DESC LIMIT 5;
        """, nativeQuery = true)
        List<TopExpensesByMonth> getTopExpensesByMonths(@Param("userId") Long userId);

  @Query(value = """
  WITH OLD_MONTH AS (SELECT USER_ID, CATEGORY_ID AS OLD_CATEGORY, SUM(AMOUNT) AS OLD_AMOUNT
  FROM TRANSACTIONS
  WHERE USER_ID = :userId AND ((EXTRACT(MONTH FROM SYSDATE()) - 1 = EXTRACT(MONTH FROM TRANSACTION_DATE) AND EXTRACT(YEAR FROM SYSDATE()) = EXTRACT(YEAR FROM TRANSACTION_DATE) AND EXTRACT(MONTH FROM SYSDATE()) != 1)
  OR (EXTRACT(MONTH FROM SYSDATE()) = 1 AND EXTRACT(YEAR FROM SYSDATE()) - 1 = EXTRACT(YEAR FROM TRANSACTION_DATE)))
  GROUP BY USER_ID, CATEGORY_ID),
  CURRENT_MONTH AS (SELECT USER_ID, CATEGORY_ID AS CURRENT_CATEGORY, SUM(AMOUNT) AS CURRENT_AMOUNT
  FROM TRANSACTIONS
  WHERE USER_ID = :userId AND EXTRACT(MONTH FROM SYSDATE()) = EXTRACT(MONTH FROM TRANSACTION_DATE) AND EXTRACT(YEAR FROM SYSDATE()) = EXTRACT(YEAR FROM TRANSACTION_DATE)
  GROUP BY USER_ID, CATEGORY_ID)
  SELECT *
  FROM (SELECT C.NAME, COALESCE(CURRENT_AMOUNT,0) AS CURRENT,COALESCE(OLD_AMOUNT,1) AS OLD, CASE WHEN CURRENT_AMOUNT >= OLD_AMOUNT THEN ROUND(COALESCE(CURRENT_AMOUNT,0) * 100 / COALESCE(OLD_AMOUNT,1), 2) - 100
  ELSE (100 - ROUND(COALESCE(CURRENT_AMOUNT,0) * 100 / COALESCE(OLD_AMOUNT,1), 2)) * (-1) END AS TOTAL
  FROM CATEGORIES C
  LEFT JOIN OLD_MONTH OM ON OM.OLD_CATEGORY = C.ID
  LEFT JOIN CURRENT_MONTH CM ON CM.CURRENT_CATEGORY = C.ID
  WHERE C.USER_ID = :userId AND C.TYPE = 'DEPENSE' AND C.PARENT_ID IS NOT NULL AND (CURRENT_AMOUNT IS NOT NULL OR OLD_AMOUNT IS NOT NULL)) SUB
  WHERE (TOTAL < 0 AND :status = 0) OR (TOTAL > 0 AND :status = 1)
  ORDER BY TOTAL;
  """, nativeQuery = true)
  List<ExpDiffDTO> getExpensesDifference(@Param("userId") Long userId, @Param("status") Integer status);
}

