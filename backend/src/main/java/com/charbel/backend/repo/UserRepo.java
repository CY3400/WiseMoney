package com.charbel.backend.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.charbel.backend.model.Users;

@Repository
public interface UserRepo extends JpaRepository<Users, Long>{
    Optional<Users> findByEmailIgnoreCase(String email);
}