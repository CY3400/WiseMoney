package com.charbel.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.charbel.backend.DTO.UpdateProfilRequest;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.UserRepo;
import com.charbel.backend.service.BudgetService;
import com.charbel.backend.service.JWTService;
import com.charbel.backend.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RestController
public class UserController {

    private static final int COOKIE_AGE_SECONDS = 86400;

    private final UserService userService;
    private final UserRepo repo;
    private final JWTService jwtService;
    private final BudgetService budgetService;

    public UserController(UserService userService, UserRepo repo, JWTService jwtService, BudgetService budgetService){
        this.userService = userService;
        this.repo = repo;
        this.jwtService = jwtService;
        this.budgetService = budgetService;
    }

    private Map<String, Object> userToMap(Users user) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", user.getFirstName());
        body.put("lastName", user.getLastName());
        body.put("email", user.getEmail());

        return body;
    }

    private void setCookies(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie Cookie = new Cookie(name, value);
        Cookie.setHttpOnly(true);
        Cookie.setSecure(false);
        Cookie.setPath("/");
        Cookie.setMaxAge(maxAgeSeconds);

        response.addCookie(Cookie);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Users user, HttpServletResponse response) {
        Users saved = userService.register(user);

        String jwt = jwtService.generateToken(saved.getEmail());

        setCookies(response, "jwt_token", jwt, COOKIE_AGE_SECONDS);

        return ResponseEntity.ok(userToMap(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Users payload, HttpServletResponse response){
        try{
            String jwt = userService.authenticate(payload.getEmail(), payload.getPassword());
            setCookies(response, "jwt_token", jwt, COOKIE_AGE_SECONDS );

            String email = payload.getEmail() == null ? "" : payload.getEmail().trim();
            Users logged = repo.findByEmailIgnoreCase(email).orElseThrow();
            budgetService.ensureCurrentMonthBudget(logged);

            return ResponseEntity.ok(userToMap(logged));
        }
        catch(BadCredentialsException ex) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).build();
        }
    }

    @GetMapping("/verify")
    public boolean verify(@RequestParam String email) {
        String e = email == null ? "" : email.trim().toLowerCase();
        return repo.findByEmailIgnoreCase(e).isEmpty();
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        setCookies(response, "jwt_token", null, 0);

        response.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).body("Non authentifié");
        }

        Users user = repo.findByEmailIgnoreCase(authentication.getName()).orElseThrow();

        return ResponseEntity.ok(userToMap(user));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody UpdateProfilRequest req, Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).body("Non authentifié");
        }

        String first = req.getFirstName() == null ? "" : req.getFirstName().trim();
        String last = req.getLastName() == null ? "" : req.getLastName().trim();

        if(first.length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le prénom doit contenir au moins 2 caractères"));
        }

        if(last.length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le nom doit contenir au moins 2 caractères"));
        }

        Users user = repo.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        user.setFirstName(first);
        user.setLastName(last);
        repo.save(user);

        return ResponseEntity.ok(userToMap(user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> request, Authentication authentication){
        String actualPassword = request.get("actualPassword");
        String newPassword = request.get("newPassword");

        if(actualPassword == null || actualPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le mot de passe actuel ne peut pas être vide"));
        }

        if(newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le nouveau mot de passe ne peut pas être vide"));
        }

        String email = authentication.getName();
        boolean success = userService.changePassword(email, actualPassword, newPassword);

        if(!success){
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(Map.of("message","Mot de passe incorrect"));
        }

        return ResponseEntity.ok(Map.of("message","Mot de passe correct"));
    }
}