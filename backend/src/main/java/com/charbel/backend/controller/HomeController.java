package com.charbel.backend.controller;

import org.springframework.security.core.Authentication;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.charbel.backend.DTO.ChildPercentView;
import com.charbel.backend.DTO.ParentSpendView;
import com.charbel.backend.model.Users;
import com.charbel.backend.service.TransactionService;
import com.charbel.backend.service.UserService;

@RestController
@RequestMapping("/home")
public class HomeController {
    private final UserService userService;
    private final TransactionService transactionService;

    public HomeController(UserService userService, TransactionService transactionService) {
        this.userService = userService;
        this.transactionService = transactionService;
    }

    @GetMapping("/percent")
    public ResponseEntity<Integer> getCurrentMonthPercent(Authentication auth) {
        Users user = userService.currentUser(auth);

        Integer percent = transactionService.getCurrentMonthPercent(user);

        return ResponseEntity.ok(percent);
    }

    @GetMapping("/PSV")
    public ResponseEntity<List<ParentSpendView>> getPSV(Authentication auth) {
        Users user = userService.currentUser(auth);

        List<ParentSpendView> out = transactionService.getParentSpendView(user);

        return ResponseEntity.ok(out);
    }

    @GetMapping("/CPV/{parentId}")
    public ResponseEntity<List<ChildPercentView>> getCPV(Authentication auth, @PathVariable Long parentId) {
        Users user = userService.currentUser(auth);

        List<ChildPercentView> out = transactionService.getChildPercentView(user, parentId);

        return ResponseEntity.ok(out);
    }

    @GetMapping("/Epargne")
    public ResponseEntity<Integer> getEpargne(Authentication auth) {
        Users user = userService.currentUser(auth);

        Integer epargne = transactionService.getEpargneForMonth(user);

        return ResponseEntity.ok(epargne);
    }

    @GetMapping("/AVG")
    public ResponseEntity<Integer> getAverage(Authentication auth) {
        Users user = userService.currentUser(auth);

        Integer average = transactionService.getAverageDaily(user);

        return ResponseEntity.ok(average);
    }

    @GetMapping("/SumRev")
    public ResponseEntity<Integer> getSumRev(Authentication auth) {
        Users user = userService.currentUser(auth);

        Integer average = transactionService.getSumRevenues(user);

        return ResponseEntity.ok(average);
    }

    @GetMapping("/SumDep")
    public ResponseEntity<Integer> getSumDep(Authentication auth) {
        Users user = userService.currentUser(auth);

        Integer average = transactionService.getSumDepenses(user);

        return ResponseEntity.ok(average);
    }

    @GetMapping("/SumDepF")
    public ResponseEntity<Integer> getSumDepF(Authentication auth) {
        Users user = userService.currentUser(auth);

        Integer average = transactionService.getSumDepFuture(user);

        return ResponseEntity.ok(average);
    }
}
