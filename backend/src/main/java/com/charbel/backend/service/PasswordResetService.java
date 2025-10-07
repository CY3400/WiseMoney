package com.charbel.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charbel.backend.model.PasswordResetToken;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.PasswordResetTokenRepo;
import com.charbel.backend.repo.UserRepo;

@Service
public class PasswordResetService {

    private static final int EXP_MIN = 30;

    private final UserRepo userRepo;
    private final PasswordResetTokenRepo tokenRepo;

    public PasswordResetService(UserRepo userRepo, PasswordResetTokenRepo tokenRepo) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
    }
    
    @Transactional
    public String createResetToken(String email) {
        String normalized = email == null ? null : email.trim().toLowerCase();

        Users user = userRepo.findByEmailIgnoreCase(normalized).orElseThrow(() -> new IllegalArgumentException("Aucun utilisateur avec cet email."));
        tokenRepo.deleteByUser_Id(user.getId());

        String token = UUID.randomUUID().toString();

        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(token);
        prt.setUser(user);
        prt.setExpiresAt(LocalDateTime.now().plusMinutes(EXP_MIN));
        prt.setUsedAt(null);

        tokenRepo.save(prt);
        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPasswordHash) {
        PasswordResetToken prt = tokenRepo.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Token invalide."));

        if (prt.getUsedAt() != null || prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expiré ou déjà utilisé.");
        }

        Users user = prt.getUser();
        user.setPassword(newPasswordHash);
        userRepo.save(user);

        prt.setUsedAt(LocalDateTime.now());
        tokenRepo.save(prt);
    }
}