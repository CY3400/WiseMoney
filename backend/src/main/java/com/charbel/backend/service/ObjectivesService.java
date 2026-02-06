package com.charbel.backend.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charbel.backend.model.Objectives;
import com.charbel.backend.model.Users;
import com.charbel.backend.repo.ObjectivesRepo;

@Service
@Transactional
public class ObjectivesService {
    private final ObjectivesRepo repo;

    public ObjectivesService(ObjectivesRepo repo) {
        this.repo = repo;
    }

    public Objectives createObjectif(Users user, BigDecimal objectif) {
        if(user == null){
            throw new IllegalArgumentException("Utilisateur requis");
        }

        if(objectif == null){
            throw new IllegalArgumentException("Montant invalide");
        }

        var now = YearMonth.now();
        int m = now.getMonthValue();
        int y = now.getYear();

        if(repo.existsByUserAndMonthAndYear(user, m, y)) {
            throw new IllegalArgumentException("Objectif déjà existant pour ce mois");
        }

        Objectives o = new Objectives();
        o.setUser(user);
        o.setObjectif(objectif);

        return repo.save(o);
    }

    public Objectives updateObjectif(Long id, BigDecimal objectif) {
        if(id == null) {
            throw new IllegalArgumentException("Identifiant du budget requis");
        }

        if(objectif == null) {
            throw new IllegalArgumentException("Montant invalide");
        }

        Objectives existing = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Objectif introuvable"));

        existing.setObjectif(objectif);
        return repo.save(existing);
    }

    @Transactional(readOnly = true)
    public List<Objectives> getObjectives(Users user){
        Objects.requireNonNull(user, "Utilisateur requis");

        return repo.findByUserOrderByYearDescMonthDesc(user);
    }

    @Transactional(readOnly = true)
    public Integer getSumObjectives(Users user){
        Objects.requireNonNull(user, "Utilisateur requis");

        return repo.getSumObjectives(user.getId());
    }
}
