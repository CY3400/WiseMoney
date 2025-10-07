package com.charbel.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.charbel.backend.service.EmailService;
import com.charbel.backend.service.PasswordResetService;
import com.charbel.backend.config.AppProps;

@RestController
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class PasswordResetController {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    private final PasswordResetService service;
    private final PasswordEncoder encoder;
    private final EmailService emailService;
    private final AppProps props;

    public PasswordResetController(
            PasswordResetService service,
            PasswordEncoder encoder,
            EmailService emailService,
            AppProps props
    ) {
        this.service = service;
        this.encoder = encoder;
        this.emailService = emailService;
        this.props = props;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgot(@RequestBody Map<String, String> body) {
        String rawEmail = body.get("email");
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email requis."));
        }
        String email = rawEmail.trim().toLowerCase();

        try {
            String token = service.createResetToken(email);
            if(token != null){
                String enc = URLEncoder.encode(token, StandardCharsets.UTF_8);
                String resetUrl = props.getFrontend().getBaseUrl() + "/Reset_MDP?token=" + enc;

                emailService.sendPasswordResetEmail(email, resetUrl);
            }
        }
        catch (IllegalArgumentException ex) {
            log.debug("Demande reset pour email inconnu: {}", email);
        }
        catch (Exception ex) {
            log.warn("Erreur envoi email reset: {}", ex.getMessage());
        }

        return ResponseEntity.ok(Map.of("message", "Si l'email existe, un lien a été envoyé."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> reset(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPwd = body.get("newPassword");

        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Token requis."));
        }
        if (newPwd == null || newPwd.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le nouveau mot de passe est requis."));
        }

        if(!newPwd.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+=?.,:;{}\\\\[\\\\]<>\\\\-]).{8,20}$")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le mot de passe doit avoir entre 8 et 20 caractères, avec majuscule, minuscule et un caractère spécial"));
        }

        try {
            String hash = encoder.encode(newPwd);
            service.resetPassword(token.trim(), hash);
            return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé."));
        }
        catch (IllegalArgumentException ex) {
            log.debug("Reset refusé: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
        catch (Exception ex) {
            log.warn("Erreur reset: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Lien invalide ou expiré."));
        }
    }
}