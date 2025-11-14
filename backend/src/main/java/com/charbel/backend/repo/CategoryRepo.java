package com.charbel.backend.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.charbel.backend.DTO.TopExpensesByMonth;
import com.charbel.backend.model.Category;
import com.charbel.backend.model.CategoryType;
import com.charbel.backend.model.Users;

import java.util.List;
import java.util.Optional;


@Repository
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
        FROM WISE_MONEY.CATEGORIES C
        INNER JOIN WISE_MONEY.TRANSACTIONS T ON T.CATEGORY_ID = C.ID
        WHERE C.USER_ID = :userId AND C.TYPE = 'DEPENSE' AND EXTRACT(MONTH FROM TRANSACTION_DATE) = EXTRACT(MONTH FROM SYSDATE())
        AND EXTRACT(YEAR FROM TRANSACTION_DATE) = EXTRACT(YEAR FROM SYSDATE())
        GROUP BY C.NAME
        ORDER BY TOTAL DESC LIMIT 5;
        """, nativeQuery = true)
        List<TopExpensesByMonth> getTopExpensesByMonths(@Param("userId") Long userId);
}

