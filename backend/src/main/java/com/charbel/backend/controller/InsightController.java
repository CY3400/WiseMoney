package com.charbel.backend.controller;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.charbel.backend.DTO.InsightDTO;
import com.charbel.backend.DTO.InsightSettingsDTO;
import com.charbel.backend.DTO.UpdateInsightSettingsRequest;
import com.charbel.backend.model.Users;
import com.charbel.backend.service.InsightService;
import com.charbel.backend.service.InsightSettingsService;
import com.charbel.backend.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/insights")
@Validated
public class InsightController {

    private final UserService userService;
    private final InsightService insightService;
    private final InsightSettingsService insightSettingsService;

    public InsightController(
            UserService userService,
            InsightService insightService,
            InsightSettingsService insightSettingsService
    ) {
        this.userService = userService;
        this.insightService = insightService;
        this.insightSettingsService = insightSettingsService;
    }

    @GetMapping
    public ResponseEntity<?> getInsights(
            Authentication auth,
            @RequestParam String month,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        Users user = userService.currentUser(auth);

        YearMonth ym;
        try {
            ym = YearMonth.parse(month); // format: "YYYY-MM"
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Format month invalide. Exemple: 2025-12"));
        }

        List<InsightDTO> out = insightService.getInsights(user.getId(), ym, force);
        return ResponseEntity.ok(out);
    }

    @GetMapping("/settings")
    public ResponseEntity<InsightSettingsDTO> getSettings(Authentication auth) {
        Users user = userService.currentUser(auth);
        return ResponseEntity.ok(insightSettingsService.getSettings(user));
    }

    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(
            Authentication auth,
            @Valid @RequestBody UpdateInsightSettingsRequest req
    ) {
        Users user = userService.currentUser(auth);
        try {
            return ResponseEntity.ok(insightSettingsService.updateSettings(user, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}