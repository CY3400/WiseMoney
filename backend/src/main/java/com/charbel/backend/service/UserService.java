package com.charbel.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.charbel.backend.model.Users;
import com.charbel.backend.repo.UserRepo;

import jakarta.transaction.Transactional;

@Service
public class UserService {
    private final UserRepo repo;
    private final AuthenticationManager authManager;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepo repo, AuthenticationManager authManager, JWTService jwtService, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Users currentUser(Authentication auth) {
        if(auth == null || !auth.isAuthenticated()) {
            throw new IllegalArgumentException("Non authentifié");
        }

        return repo.findByEmailIgnoreCase(auth.getName()).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }

    @Transactional
    public Users register(Users user) {
        String email = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String rawPw = user.getPassword() == null ? "" : user.getPassword();

        if(email.isEmpty() || first.length() < 2 || last.length() < 2 || rawPw.isEmpty()) {
            throw new IllegalArgumentException("Données d'inscription invalides.");
        }

        if(repo.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("Email déjà utilisé.");
        }

        user.setEmail(email);
        user.setFirstName(first);
        user.setLastName(last);
        user.setPassword(passwordEncoder.encode(rawPw));

        return repo.save(user);
    }

    public String authenticate(String email, String password){
        String e = email == null ? "" : email.trim().toLowerCase();
        String p = password == null ? "" : password;

        Authentication auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(e, p));

        if(!auth.isAuthenticated()) {
            throw new BadCredentialsException("Identifiants invalides.");
        }

        return jwtService.generateToken(e);
    }

    @Transactional
    public boolean changePassword(String email, String actualPassword, String newPassword) {
        Users user = repo.findByEmailIgnoreCase(email).orElseThrow();
        if(user == null) return false;

        if(!passwordEncoder.matches(actualPassword, user.getPassword())){
            return false;
        }

        if(passwordEncoder.matches(newPassword, user.getPassword())){
            throw new IllegalArgumentException("Le nouveau mot de passe doit être différent de l'actuel.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        repo.save(user);
        return true;
    }

    public Users findByEmailOrThrow(String email) {
        return repo.findByEmailIgnoreCase(email).orElseThrow();
    }
}