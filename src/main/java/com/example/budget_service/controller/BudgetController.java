package com.example.budget_service.controller;

import com.example.budget_service.dto.BudgetDTO;
import com.example.budget_service.model.Budget;
import com.example.budget_service.service.BudgetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private static final Logger logger = LoggerFactory.getLogger(BudgetController.class);

    private final BudgetManager budgetManager;

    @Autowired
    public BudgetController(BudgetManager budgetManager) {
        this.budgetManager = budgetManager;
    }

    @PostMapping
    public ResponseEntity<Budget> createBudget(@RequestBody BudgetDTO budgetDTO) {
        logger.debug("Received create budget request: {}", budgetDTO);
        Budget createdBudget = budgetManager.createBudget(budgetDTO);
        return new ResponseEntity<>(createdBudget, HttpStatus.CREATED);
    }

    @PutMapping("/{startDate}")
    public ResponseEntity<Budget> updateBudget(
            @PathVariable("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestBody BudgetDTO budgetDTO) {
        logger.debug("Received update budget request for startDate: {}", startDate);
        Budget updatedBudget = budgetManager.updateBudget(startDate, budgetDTO);
        return new ResponseEntity<>(updatedBudget, HttpStatus.OK);
    }

    @DeleteMapping("/{startDate}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        logger.debug("Received delete budget request for startDate: {}", startDate);
        budgetManager.deleteBudget(startDate);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping
    public ResponseEntity<List<Budget>> getAllBudgets() {
        logger.debug("Received get all budgets request");
        List<Budget> budgets = budgetManager.getAllBudgets();
        return new ResponseEntity<>(budgets, HttpStatus.OK);
    }

    @GetMapping("/{startDate}")
    public ResponseEntity<Budget> getBudget(
            @PathVariable("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        logger.debug("Received get budget request for startDate: {}", startDate);
        Budget budget = budgetManager.getBudget(startDate);
        return new ResponseEntity<>(budget, HttpStatus.OK);
    }
}