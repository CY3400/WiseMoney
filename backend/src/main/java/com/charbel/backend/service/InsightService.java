package com.charbel.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.charbel.backend.DTO.DeltaTransactions;
import com.charbel.backend.DTO.InsightDTO;
import com.charbel.backend.DTO.InsightSeverity;
import com.charbel.backend.DTO.InsightType;
import com.charbel.backend.model.CategoryType;
import com.charbel.backend.model.InsightHistory;
import com.charbel.backend.model.Transaction;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.BudgetRepo;
import com.charbel.backend.repo.InsightHistoryRepo;
import com.charbel.backend.repo.TransactionRepo;
import com.charbel.backend.repo.UserRepo;

@Service
public class InsightService {

    private final TransactionRepo transactionRepo;
    private final BudgetRepo budgetRepo;
    private final UserRepo userRepo;
    private final InsightHistoryRepo insightHistoryRepo;

    public InsightService(TransactionRepo transactionRepo, BudgetRepo budgetRepo, UserRepo userRepo, InsightHistoryRepo insightHistoryRepo) {
        this.transactionRepo = transactionRepo;
        this.budgetRepo = budgetRepo;
        this.userRepo = userRepo;
        this.insightHistoryRepo = insightHistoryRepo;
    }

    public List<InsightDTO> getInsights(Long userId, YearMonth month, boolean force) {
        Users user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);

        int m = month.getMonthValue();
        int y = month.getYear();

        YearMonth currentMonth = YearMonth.now();
        LocalDate today = LocalDate.now();

        List<Transaction> tx = transactionRepo.findByUserAndDateRange(userId, start, end);

        YearMonth prevMonth = month.minusMonths(1);
        LocalDate prevStart = prevMonth.atDay(1);
        LocalDate prevEnd = prevMonth.plusMonths(1).atDay(1);

        List<Transaction> prevTx = transactionRepo.findByUserAndDateRange(userId, prevStart, prevEnd);

        BigDecimal prevRevenues = sumRevenues(prevTx);
        BigDecimal prevExpenses = sumExpenses(prevTx);

        BigDecimal curRevenues = sumRevenues(tx);
        BigDecimal curExpenses = sumExpenses(tx);

        BigDecimal monthlyBudget = budgetRepo.findByUserIdAndMonthAndYear(userId, m, y)
                .map(b -> b.getAmount())
                .orElse(BigDecimal.ZERO);

        BigDecimal revenuesSoFar = sumRevenues(tx);
        BigDecimal capacity = monthlyBudget.add(revenuesSoFar);

        List<InsightDTO> insights = new ArrayList<>();
        List<DeltaTransactions> delta = transactionRepo.findDeltaTransactions(userId);

        insights.add(buildRunRateInsight(month, monthlyBudget, capacity, tx, user, revenuesSoFar));
        insights.add(buildLastSprintInsight(month, monthlyBudget, capacity, tx, revenuesSoFar));
        if (!prevTx.isEmpty()) {
            insights.add(buildMonthOverMonthGlobalInsight(month, curExpenses, curRevenues, prevExpenses, prevRevenues, delta));
        }
        insights.add(buildDailySpendingCapInsight(month, monthlyBudget, capacity, tx, revenuesSoFar));

        if (month.equals(currentMonth)) {
            int day = today.getDayOfMonth();
            if (day == 10 || day == 15) {
                insights.add(buildMidMonthBudgetCheckInsight(month, monthlyBudget, capacity, tx, day, user, revenuesSoFar));
            }
        }

        insights.addAll(buildTopCategoriesInsight(month, tx));

        insights = insights.stream()
                .filter(Objects::nonNull)
                .filter(dto -> force || shouldEmit(userId, dto))
                .sorted(Comparator.comparingInt(InsightDTO::getScore).reversed())
                .collect(Collectors.toList());

        if (!force) {
            for (InsightDTO dto : insights) {
                logEmitted(user, dto);
            }
        }

        return insights;
    }

    private InsightDTO buildMonthOverMonthGlobalInsight(YearMonth month, BigDecimal curExpenses, BigDecimal curRevenues, BigDecimal prevExpenses, BigDecimal prevRevenues, List<DeltaTransactions> delta) {
        curExpenses = nz(curExpenses);
        curRevenues = nz(curRevenues);
        prevExpenses = nz(prevExpenses);
        prevRevenues = nz(prevRevenues);

        if (curExpenses.compareTo(BigDecimal.ZERO) == 0 && curRevenues.compareTo(BigDecimal.ZERO) == 0 && prevExpenses.compareTo(BigDecimal.ZERO) == 0 && prevRevenues.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal curNet = curRevenues.subtract(curExpenses);
        BigDecimal prevNet = prevRevenues.subtract(prevExpenses);

        BigDecimal expPct = percentChange(prevExpenses, curExpenses);
        BigDecimal revPct = percentChange(prevRevenues, curRevenues);
        BigDecimal netDelta = curNet.subtract(prevNet);

        int score = 30;
        InsightSeverity severity = InsightSeverity.INFO;

        if (expPct != null) {
            if (expPct.compareTo(BigDecimal.valueOf(30)) >= 0) {
                score = 80;
                severity = InsightSeverity.WARNING;
            }
            if (expPct.compareTo(BigDecimal.valueOf(60)) >= 0) {
                score = 90;
                severity = InsightSeverity.CRITICAL;
            }
        }

        if (prevRevenues.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal netDropPctOfPrevRev = netDelta
                    .divide(prevRevenues, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (netDropPctOfPrevRev.compareTo(BigDecimal.valueOf(-25)) <= 0) {
                score = Math.max(score, 75);
                severity = maxSeverity(severity, InsightSeverity.WARNING);
            }
            if (netDropPctOfPrevRev.compareTo(BigDecimal.valueOf(-50)) <= 0) {
                score = Math.max(score, 90);
                severity = maxSeverity(severity, InsightSeverity.CRITICAL);
            }
        }

        String title = "Comparaison avec le mois précédent";

        String message =
        "Dépenses : " + money(curExpenses) + " (" + fmtPct(expPct) + ")\n" +
        "Revenus : " + money(curRevenues) + " (" + fmtPct(revPct) + ")\n" +
        "Net : " + money(curNet) + " (" + signedMoney(netDelta) + " vs mois dernier)";

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("curExpenses", curExpenses);
        facts.put("curRevenues", curRevenues);
        facts.put("curNet", curNet);
        facts.put("prevExpenses", prevExpenses);
        facts.put("prevRevenues", prevRevenues);
        facts.put("prevNet", prevNet);
        facts.put("expensesPercentChange", expPct);
        facts.put("revenuesPercentChange", revPct);
        facts.put("netDelta", netDelta);
        if(delta != null && !delta.isEmpty()) {
            facts.put("delta", delta);

            String top1 = delta.get(0).getName();
            String top2 = delta.size() > 1 ? delta.get(1).getName() : null;
            String top3 = delta.size() > 2 ? delta.get(2).getName() : null;

            message += "\n\nPrincipales hausses : " +
            top1 +
            (top2 != null ? ", " + top2 : "") +
            (top3 != null ? ", " + top3 : "");
        }

        List<String> suggestions = new ArrayList<>();

        if (expPct != null && expPct.compareTo(BigDecimal.valueOf(20)) >= 0) {
            suggestions.add("Tes dépenses montent vs le mois dernier : regarde où ça a gonflé (top catégories).");
            suggestions.add("Fixe un plafond quotidien plus strict 3–5 jours.");
        }
        if (revPct != null && revPct.compareTo(BigDecimal.valueOf(-10)) <= 0) {
            suggestions.add("Tes revenus sont plus bas que le mois dernier : anticipe en réduisant les dépenses variables.");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Ton mois est assez stable vs le mois dernier. Continue comme ça.");
        }
        if(delta != null && !delta.isEmpty()) {
            suggestions.add("La hausse vient surtout de: " + delta.get(0).getName() + ". Regarde tes transactions dans cette catégorie");
        }

        return new InsightDTO(
                InsightType.MONTH_OVER_MONTH_GLOBAL,
                severity,
                score,
                title,
                message,
                month.toString(),
                null,
                null,
                facts,
                suggestions
        );
    }

    private InsightDTO buildRunRateInsight(YearMonth month,
                                          BigDecimal monthlyBudget,
                                          BigDecimal capacity,
                                          List<Transaction> tx,
                                          Users user,
                                          BigDecimal revenuesSoFar) {

        if (monthlyBudget == null || monthlyBudget.compareTo(BigDecimal.ZERO) <= 0) {
            return new InsightDTO(
                    InsightType.RUN_RATE_OVER_BUDGET,
                    InsightSeverity.INFO,
                    20,
                    "Budget mensuel non défini",
                    "Ajoute ton budget du mois pour recevoir des alertes intelligentes.",
                    month.toString(),
                    null,
                    null,
                    Map.of(),
                    List.of("Va dans Budget et ajoute un montant mensuel.")
            );
        }

        if (capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0) {
            capacity = monthlyBudget.max(BigDecimal.ONE);
        }

        BigDecimal spentSoFar = sumExpenses(tx);

        int today = LocalDate.now().getDayOfMonth();
        int daysInMonth = month.lengthOfMonth();
        int dayIndex = Math.max(1, Math.min(today, daysInMonth));

        int warningPct = user.getRunrateWarningPercent();
        int criticalPct = user.getRunrateCriticalPercent();

        if (criticalPct < warningPct) {
            int tmp = criticalPct;
            criticalPct = warningPct;
            warningPct = tmp;
        }

        BigDecimal warning = BigDecimal.valueOf(warningPct);
        BigDecimal critical = BigDecimal.valueOf(criticalPct);

        BigDecimal runRate = spentSoFar
                .divide(BigDecimal.valueOf(dayIndex), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(daysInMonth));

        boolean willOver = runRate.compareTo(capacity) > 0;

        BigDecimal delta = runRate.subtract(capacity);
        BigDecimal deltaPercent = delta
                .divide(capacity, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        int score;
        InsightSeverity severity;

        if (!willOver) {
            score = 30;
            severity = InsightSeverity.INFO;
        } else {
            if (deltaPercent.compareTo(critical) >= 0) {
                score = 90;
                severity = InsightSeverity.CRITICAL;
            } else if (deltaPercent.compareTo(warning) >= 0) {
                score = 70;
                severity = InsightSeverity.WARNING;
            } else {
                score = 55;
                severity = InsightSeverity.INFO;
            }
        }

        String title = willOver ? "Risque de dépasser ta capacité" : "Rythme de dépense OK";

        String message = willOver
                ? "À ce rythme, tu finirais à " + money(runRate) + " pour une capacité de " + money(capacity)
                + " (+" + percent(deltaPercent) + ")."
                : "Tu as dépensé " + money(spentSoFar) + " jusqu'ici pour une capacité de " + money(capacity) + ".";

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("spentSoFar", spentSoFar);
        facts.put("monthlyBudget", monthlyBudget);
        facts.put("revenuesSoFar", revenuesSoFar);
        facts.put("capacity", capacity);
        facts.put("runRate", runRate);
        facts.put("dayOfMonth", dayIndex);
        facts.put("daysInMonth", daysInMonth);
        facts.put("deltaPercent", deltaPercent);

        List<String> suggestions = willOver
                ? List.of(
                        "Réduis 1-2 dépenses non essentielles cette semaine.",
                        "Fixe un plafond de dépense hebdo jusqu'à la fin du mois."
                )
                : List.of("Continue comme ça.");

        return new InsightDTO(
                InsightType.RUN_RATE_OVER_BUDGET,
                severity,
                score,
                title,
                message,
                month.toString(),
                null,
                null,
                facts,
                suggestions
        );
    }

    private InsightDTO buildLastSprintInsight(YearMonth month,
                                              BigDecimal monthlyBudget,
                                              BigDecimal capacity,
                                              List<Transaction> tx,
                                              BigDecimal revenuesSoFar) {

        if (monthlyBudget == null || monthlyBudget.compareTo(BigDecimal.ZERO) <= 0 || !month.equals(YearMonth.now())) {
            return null;
        }

        if (capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0) {
            capacity = monthlyBudget.max(BigDecimal.ONE);
        }

        LocalDate todayDate = LocalDate.now();
        int today = todayDate.getDayOfMonth();
        int daysInMonth = month.lengthOfMonth();

        int remainingDays = Math.max(1, daysInMonth - today + 1);
        boolean isLastSprint = (today >= 25) || (remainingDays <= 5);

        if (!isLastSprint) {
            return null;
        }

        BigDecimal spentSoFar = sumExpenses(tx);
        BigDecimal remaining = capacity.subtract(spentSoFar);

        BigDecimal dailyCap = remaining.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP);

        BigDecimal avgDaily = capacity.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        int score;
        InsightSeverity severity;

        if (remaining.compareTo(BigDecimal.ZERO) < 0 || dailyCap.compareTo(BigDecimal.ZERO) < 0) {
            score = 90;
            severity = InsightSeverity.CRITICAL;
        } else if (dailyCap.compareTo(avgDaily.multiply(BigDecimal.valueOf(0.3))) < 0) {
            score = 65;
            severity = InsightSeverity.WARNING;
        } else {
            score = 35;
            severity = InsightSeverity.INFO;
        }

        String title = "Dernier sprint du mois";

        String message;
        if (severity == InsightSeverity.CRITICAL) {
            message = "Fin de mois difficile : tu es au-dessus de ta capacité du mois. Objectif : limiter les dépenses variables jusqu’à la fin du mois.";
        } else {
            message = "Il te reste " + money(remaining) + " pour " + remainingDays + " jours (en comptant aujourd’hui). "
                    + "Vise ~" + money(dailyCap) + "/jour pour finir dans ta capacité.";
        }

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("spentSoFar", spentSoFar);
        facts.put("monthlyBudget", monthlyBudget);
        facts.put("revenuesSoFar", revenuesSoFar);
        facts.put("capacity", capacity);
        facts.put("remaining", remaining);
        facts.put("remainingDays", remainingDays);
        facts.put("dailyCap", dailyCap);
        facts.put("avgDaily", avgDaily);

        List<String> suggestions = (severity == InsightSeverity.CRITICAL)
                ? List.of(
                        "Fais une pause sur les achats non essentiels 2–3 jours.",
                        "Si besoin, ajuste ton budget (réaliste) pour éviter la frustration."
                )
                : List.of(
                        "Prévois tes dépenses variables (sorties, snacks, livraison) à l’avance.",
                        "Évite les achats impulsifs jusqu’à la fin du mois."
                );

        return new InsightDTO(
                InsightType.LAST_SPRINT,
                severity,
                score,
                title,
                message,
                month.toString(),
                null,
                null,
                facts,
                suggestions
        );
    }

    private InsightDTO buildDailySpendingCapInsight(YearMonth month,
                                                    BigDecimal monthlyBudget,
                                                    BigDecimal capacity,
                                                    List<Transaction> tx,
                                                    BigDecimal revenuesSoFar) {

        if (monthlyBudget == null || monthlyBudget.compareTo(BigDecimal.ZERO) <= 0 || !month.equals(YearMonth.now())) {
            return null;
        }

        if (capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0) {
            capacity = monthlyBudget.max(BigDecimal.ONE);
        }

        BigDecimal spentSoFar = sumExpenses(tx);
        BigDecimal remaining = capacity.subtract(spentSoFar);

        int daysInMonth = month.lengthOfMonth();
        int today = LocalDate.now().getDayOfMonth();
        int remainingDays = Math.max(1, daysInMonth - today + 1);

        BigDecimal dailyCap = (remainingDays == 0)
                ? remaining
                : remaining.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP);

        boolean overCapacity = remaining.compareTo(BigDecimal.ZERO) < 0;

        BigDecimal avgDaily = capacity.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        int score;
        InsightSeverity severity;

        if (overCapacity || dailyCap.compareTo(BigDecimal.ZERO) < 0) {
            score = 85;
            severity = InsightSeverity.CRITICAL;
        } else if (remainingDays > 0 && dailyCap.compareTo(avgDaily) < 0) {
            score = 60;
            severity = InsightSeverity.WARNING;
        } else {
            score = 30;
            severity = InsightSeverity.INFO;
        }

        String title = "Plafond quotidien conseillé";
        String message;

        if (overCapacity) {
            message = "Tu as dépassé ta capacité de " + money(remaining.abs()) + ". Objectif : réduire les dépenses jusqu’à la fin du mois.";
        } else {
            message = "Il te reste " + money(remaining) + " pour ce mois. Pour tenir, vise environ "
                    + money(dailyCap) + " par jour (en comptant aujourd'hui, sur " + remainingDays + " jours).";
        }

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("spentSoFar", spentSoFar);
        facts.put("monthlyBudget", monthlyBudget);
        facts.put("revenuesSoFar", revenuesSoFar);
        facts.put("capacity", capacity);
        facts.put("remaining", remaining);
        facts.put("remainingDays", remainingDays);
        facts.put("dailyCap", dailyCap);

        List<String> suggestions = overCapacity
                ? List.of(
                        "Fais une pause sur les dépenses variables 2–3 jours.",
                        "Réévalue ton budget du mois si nécessaire."
                )
                : List.of("Utilise ce plafond quotidien comme repère pour tes dépenses variables.");

        return new InsightDTO(
                InsightType.DAILY_SPENDING_CAP,
                severity,
                score,
                title,
                message,
                month.toString(),
                null,
                null,
                facts,
                suggestions
        );
    }

    private InsightDTO buildMidMonthBudgetCheckInsight(YearMonth month,
                                                       BigDecimal monthlyBudget,
                                                       BigDecimal capacity,
                                                       List<Transaction> tx,
                                                       int day,
                                                       Users user,
                                                       BigDecimal revenuesSoFar) {

        if (monthlyBudget == null || monthlyBudget.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        if (capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0) {
            capacity = monthlyBudget.max(BigDecimal.ONE);
        }

        BigDecimal spentSoFar = sumExpenses(tx);

        BigDecimal usedPercent = spentSoFar
                .divide(capacity, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        BigDecimal threshold = (day == 10)
                ? BigDecimal.valueOf(user.getMidmonthDay10ThresholdPercent())
                : BigDecimal.valueOf(user.getMidmonthDay15ThresholdPercent());

        boolean over = usedPercent.compareTo(threshold) >= 0;

        BigDecimal overBy = usedPercent.subtract(threshold);

        int score;
        if (!over) {
            score = 25;
        } else if (overBy.compareTo(BigDecimal.valueOf(15)) >= 0) {
            score = 85;
        } else if (overBy.compareTo(BigDecimal.valueOf(5)) >= 0) {
            score = 70;
        } else {
            score = 60;
        }

        InsightSeverity severity = over ? severityFromScore(score) : InsightSeverity.INFO;

        String title = (day == 10) ? "Checkpoint du 10" : "Checkpoint du 15";

        String message = over
                ? "Tu as déjà utilisé " + percent(usedPercent) + " de ta capacité (" + money(spentSoFar) + " sur " + money(capacity) + "). "
                + "Objectif conseillé à J" + day + " : " + percent(threshold) + " max."
                : "Tu es dans le bon rythme : " + percent(usedPercent) + " de ta capacité utilisée à J" + day + ".";

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("day", day);
        facts.put("spentSoFar", spentSoFar);
        facts.put("monthlyBudget", monthlyBudget);
        facts.put("revenuesSoFar", revenuesSoFar);
        facts.put("capacity", capacity);
        facts.put("usedPercent", usedPercent);
        facts.put("thresholdPercent", threshold);

        List<String> suggestions = over
                ? List.of(
                        "Essaie de réduire les dépenses variables pendant 3–5 jours.",
                        "Fixe un plafond de dépense quotidien jusqu’au prochain checkpoint."
                )
                : List.of("Continue comme ça, ton rythme est bon.");

        return new InsightDTO(
                InsightType.MIDMONTH_BUDGET_CHECK,
                severity,
                score,
                title,
                message,
                month.toString(),
                null,
                null,
                facts,
                suggestions
        );
    }

    private List<InsightDTO> buildTopCategoriesInsight(YearMonth month, List<Transaction> tx) {
        Map<Long, BigDecimal> spentByCategory = new HashMap<>();
        Map<Long, String> categoryNames = new HashMap<>();

        for (Transaction t : tx) {
            if (t.getCategory() == null) continue;
            if (t.getCategory().getType() != CategoryType.DEPENSE) continue;
            if (t.getAmount() == null) continue;
            if (t.getCategory().getId() == null) continue;

            BigDecimal amt = t.getAmount();
            Long catId = t.getCategory().getId();

            spentByCategory.merge(catId, amt, BigDecimal::add);
            categoryNames.putIfAbsent(catId, t.getCategory().getName());
        }

        List<Map.Entry<Long, BigDecimal>> top = spentByCategory.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(3)
                .toList();

        List<InsightDTO> out = new ArrayList<>();

        int rank = 1;
        for (Map.Entry<Long, BigDecimal> e : top) {
            Long catId = e.getKey();
            BigDecimal spent = e.getValue();
            String catName = categoryNames.getOrDefault(catId, "Catégorie");

            out.add(new InsightDTO(
                    InsightType.SPIKE_SPENDING,
                    InsightSeverity.INFO,
                    35,
                    "Top dépense #" + rank + ": " + catName,
                    "Tu as dépensé " + money(spent) + " dans " + catName + " ce mois-ci.",
                    month.toString(),
                    catId,
                    catName,
                    Map.of("spent", spent),
                    List.of("Regarde si tu peux réduire un peu " + catName + " la semaine prochaine.")
            ));
            rank++;
        }

        return out;
    }

    private BigDecimal sumExpenses(List<Transaction> tx) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction t : tx) {
            if (t.getCategory() == null) continue;
            if (t.getCategory().getType() != CategoryType.DEPENSE) continue;
            if (t.getAmount() == null) continue;

            sum = sum.add(t.getAmount());
        }
        return sum;
    }

    private BigDecimal sumRevenues(List<Transaction> tx) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction t : tx) {
            if (t.getCategory() == null) continue;
            if (t.getCategory().getType() != CategoryType.REVENU) continue;
            if (t.getAmount() == null) continue;

            sum = sum.add(t.getAmount());
        }
        return sum;
    }

    private InsightSeverity severityFromScore(int score) {
        if (score >= 80) return InsightSeverity.CRITICAL;
        if (score >= 50) return InsightSeverity.WARNING;
        return InsightSeverity.INFO;
    }

    private String money(BigDecimal v) {
        if (v == null) return "0";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString() + " LBP";
    }

    private String percent(BigDecimal v) {
        if (v == null) return "0%";
        return v.setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private int cooldownHours(InsightSeverity severity) {
        return switch (severity) {
            case INFO -> 72;
            case WARNING -> 24;
            case CRITICAL -> 12;
        };
    }

    private String fingerprint(InsightDTO dto) {
        String base = dto.getType().name() + "|" + dto.getMonth();

        if (dto.getType() == InsightType.SPIKE_SPENDING) {
            return base;
        }

        if (dto.getCategoryId() != null) {
            base += "|cat=" + dto.getCategoryId();
        }

        if (dto.getType() == InsightType.MIDMONTH_BUDGET_CHECK && dto.getFacts() != null && dto.getFacts().get("day") != null) {
            base += "|day=" + dto.getFacts().get("day");
        }

        return base;
    }

    private boolean shouldEmit(Long userId, InsightDTO dto) {
        String fp = fingerprint(dto);
        int hours = cooldownHours(dto.getSeverity());

        return !insightHistoryRepo.existsByUserIdAndFingerprintAndCreatedAtAfter(
                userId,
                fp,
                java.time.LocalDateTime.now().minusHours(hours)
        );
    }

    private void logEmitted(Users user, InsightDTO dto) {
        InsightHistory h = new InsightHistory();
        h.setUser(user);
        h.setMonth(dto.getMonth());
        h.setType(dto.getType());
        h.setFingerprint(fingerprint(dto));
        h.setSeverity(dto.getSeverity());
        h.setScore(dto.getScore());

        try {
            insightHistoryRepo.save(h);
        } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
        }
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal percentChange(BigDecimal prev, BigDecimal cur) {
        prev = nz(prev);
        cur = nz(cur);

        if (prev.compareTo(BigDecimal.ZERO) == 0) return null;

        return cur.subtract(prev)
                .divide(prev, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private String fmtPct(BigDecimal pct) {
        if (pct == null) return "n/a";
        return (pct.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + percent(pct);
    }

    private String signedMoney(BigDecimal v) {
        if (v == null) return "0";
        return (v.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "-") + money(v.abs());
    }

    private InsightSeverity maxSeverity(InsightSeverity a, InsightSeverity b) {
        if (a == InsightSeverity.CRITICAL || b == InsightSeverity.CRITICAL) return InsightSeverity.CRITICAL;
        if (a == InsightSeverity.WARNING || b == InsightSeverity.WARNING) return InsightSeverity.WARNING;
        return InsightSeverity.INFO;
    }
}
