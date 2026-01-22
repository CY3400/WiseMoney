package com.charbel.backend.service;

import org.springframework.stereotype.Service;

import com.charbel.backend.DTO.InsightSettingsDTO;
import com.charbel.backend.DTO.UpdateInsightSettingsRequest;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.UserRepo;

@Service
public class InsightSettingsService {

    private final UserRepo userRepo;

    public InsightSettingsService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public InsightSettingsDTO getSettings(Users user) {
        return toDto(user);
    }

    public InsightSettingsDTO updateSettings(Users user, UpdateInsightSettingsRequest req) {
        if (req.getRunrateCriticalPercent() < req.getRunrateWarningPercent()) {
            throw new IllegalArgumentException("runrateCriticalPercent doit être >= runrateWarningPercent");
        }

        user.setMidmonthDay10ThresholdPercent(req.getMidmonthDay10TresholdPercent());
        user.setMidmonthDay15ThresholdPercent(req.getMidmonthDay15TresholdPercent());
        user.setRunrateWarningPercent(req.getRunrateWarningPercent());
        user.setRunrateCriticalPercent(req.getRunrateCriticalPercent());

        userRepo.save(user);
        return toDto(user);
    }

    private InsightSettingsDTO toDto(Users u) {
        return new InsightSettingsDTO(
                u.getMidmonthDay10ThresholdPercent(),
                u.getMidmonthDay15ThresholdPercent(),
                u.getRunrateWarningPercent(),
                u.getRunrateCriticalPercent()
        );
    }
}