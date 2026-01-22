package com.charbel.backend.DTO;

public class InsightSettingsDTO {
    private int midmonthDay10TresholdPercent;
    private int midmonthDay15TresholdPercent;
    private int runrateWarningPercent;
    private int runrateCriticalPercent;

    public InsightSettingsDTO(int midmonthDay10TresholdPercent, int midmonthDay15TresholdPercent, int runrateWarningPercent, int runrateCriticalPercent) {
        this.midmonthDay10TresholdPercent = midmonthDay10TresholdPercent;
        this.midmonthDay15TresholdPercent = midmonthDay15TresholdPercent;
        this.runrateWarningPercent = runrateWarningPercent;
        this.runrateCriticalPercent = runrateCriticalPercent;
    }

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
