package com.charbel.backend.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.charbel.backend.DTO.CreateBudgetRequest;
import com.charbel.backend.DTO.UpdateBudgetRequest;
import com.charbel.backend.model.Objectives;
import com.charbel.backend.model.Users;
import com.charbel.backend.service.ObjectivesService;
import com.charbel.backend.service.UserService;

@RestController
@RequestMapping("/objectives")
public class ObjectivesController {
    private final UserService userService;
    private final ObjectivesService objectivesService;

    public ObjectivesController(UserService userService, ObjectivesService objectivesService) {
        this.userService = userService;
        this.objectivesService = objectivesService;
    }

    private Map<String, Object> toMap(Objectives o) {
        Map<String, Object> m = new HashMap<>();

        m.put("id", o.getId());
        m.put("objectif", o.getObjectif());
        m.put("month", o.getMonth());
        m.put("year", o.getYear());

        return m;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateBudgetRequest req, Authentication auth) {
        Users user = userService.currentUser(auth);

        Objectives created = objectivesService.createObjectif(user, req.getAmount());

        return ResponseEntity.status(201).body(toMap(created));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication auth) {
        Users user = userService.currentUser(auth);

        List<Map<String, Object>> out = objectivesService.getObjectives(user).stream().map(this::toMap).toList();

        return ResponseEntity.ok(out);
    }

    @GetMapping("/sum")
    public ResponseEntity<BigDecimal> sumObjectives(Authentication auth) {
        Users user = userService.currentUser(auth);

        BigDecimal out = objectivesService.getSumObjectives(user);

        return ResponseEntity.ok(out);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable long id, @RequestBody UpdateBudgetRequest req, Authentication auth) {
        userService.currentUser(auth);

        Objectives updated = objectivesService.updateObjectif(id, req.getAmount());
        return ResponseEntity.ok(toMap(updated));
    }
}
