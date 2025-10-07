package com.charbel.backend.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

