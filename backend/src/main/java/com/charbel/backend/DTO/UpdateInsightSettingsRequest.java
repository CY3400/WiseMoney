package com.charbel.backend.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class UpdateInsightSettingsRequest {
    @Min(0) @Max(100)
    private int midmonthDay10TresholdPercent;
    @Min(0) @Max(100)
    private int midmonthDay15TresholdPercent;
    @Min(0) @Max(100)
    private int runrateWarningPercent;
    @Min(0) @Max(100)
    private int runrateCriticalPercent;

    public int getMidmonthDay10TresholdPercent() {
        return midmonthDay10TresholdPercent;
    }

    public void setMidmonthDay10TresholdPercent(int v) {
        this.midmonthDay10TresholdPercent = v;
    }

    public int getMidmonthDay15TresholdPercent() {
        return midmonthDay15TresholdPercent;
    }

    public void setMidmonthDay15TresholdPercent(int v) {
        this.midmonthDay15TresholdPercent = v;
    }

    public int getRunrateWarningPercent() {
        return runrateWarningPercent;
    }

    public void setRunrateWarningPercent(int v) {
        this.runrateWarningPercent = v;
    }

    public int getRunrateCriticalPercent() {
        return runrateCriticalPercent;
    }

    public void setRunrateCriticalPercent(int v) {
        this.runrateCriticalPercent = v;
    }
}
