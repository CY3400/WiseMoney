package com.charbel.backend.DTO;

import java.util.List;
import java.util.Map;

public class InsightDTO {
    private InsightType type;
    private InsightSeverity severity;
    private int score;

    private String title;
    private String message;

    private String month;

    private Long categoryId;
    private String categoryName;

    private Map<String, Object> facts;
    private List<String> suggestions;

    public InsightDTO() {}

    public InsightDTO(InsightType type, InsightSeverity severity, int score, String title, String message, String month, Long categoryId, String categoryName, Map<String, Object> facts, List<String> suggestions) {
        this.type = type;
        this.severity = severity;
        this.score = score;
        this.title = title;
        this.message = message;
        this.month = month;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.facts = facts;
        this.suggestions = suggestions;
    }

    public InsightType getType() { return type; }
    public void setType(InsightType type) { this.type = type; }

    public InsightSeverity getSeverity() { return severity; }
    public void setSeverity(InsightSeverity severity) { this.severity = severity; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Map<String, Object> getFacts() { return facts; }
    public void setFacts(Map<String, Object> facts) { this.facts = facts; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}
