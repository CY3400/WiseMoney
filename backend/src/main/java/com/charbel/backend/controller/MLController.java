package com.charbel.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.charbel.backend.DTO.MLSnapshotDTO;
import com.charbel.backend.model.Users;
import com.charbel.backend.service.MLDatasetService;
import com.charbel.backend.service.UserService;

@RestController
@RequestMapping("/ml")
public class MLController {

    private final UserService userService;
    private final MLDatasetService mlDatasetService;

    public MLController(UserService userService, MLDatasetService mlDatasetService) {
        this.userService = userService;
        this.mlDatasetService = mlDatasetService;
    }

    @GetMapping("/dataset")
    public ResponseEntity<List<MLSnapshotDTO>> dataset(Authentication auth, @RequestParam(defaultValue = "2") int monthsBack, @RequestParam(defaultValue = "5") int stepDays) {
        Users user = userService.currentUser(auth);

        List<MLSnapshotDTO> out = mlDatasetService.buildDataset(user, monthsBack, stepDays);

        return ResponseEntity.ok(out);
    }
}
