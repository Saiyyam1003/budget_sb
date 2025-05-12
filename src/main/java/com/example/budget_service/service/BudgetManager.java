package com.example.budget_service.service;

import com.example.budget_service.dto.BudgetDTO;
import com.example.budget_service.model.Budget;
import com.example.budget_service.repository.BudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class BudgetManager {
    private static final Logger logger = LoggerFactory.getLogger(BudgetManager.class);

    private final BudgetRepository repository;
    private final BillProcessor billProcessor;
    private final TransactionProcessor transactionProcessor;

    @Autowired
    public BudgetManager(BudgetRepository repository, BillProcessor billProcessor, TransactionProcessor transactionProcessor) {
        this.repository = repository;
        this.billProcessor = billProcessor;
        this.transactionProcessor = transactionProcessor;
    }

    public Budget createBudget(BudgetDTO dto) {
        logger.debug("Creating budget for startDate: {}", dto.getStartDate());
        validateBudgetDTO(dto);

        LocalDate periodStart = dto.getStartDate().withDayOfMonth(1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());

        // Check for existing budget
        if (repository.findByPeriodStart(periodStart).isPresent()) {
            throw new IllegalArgumentException("Budget already exists for " + periodStart.getMonth() + " " + periodStart.getYear());
        }

        Budget budget = new Budget();
        budget.setBudgetAmount(dto.getBudgetAmount());
        budget.setPeriodStart(periodStart);
        budget.setPeriodEnd(periodEnd);
        budget.setRemainingFunds(dto.getBudgetAmount());
        budget.setOverflowFund(0.0);
        budget.setPreviousNegativeBalance(0.0);

        resetBudgetForPeriod(budget);
        Budget saved = repository.save(budget);
        logger.info("Budget created with ID: {}", saved.getId());
        return saved;
    }

    public Budget updateBudget(LocalDate startDate, BudgetDTO dto) {
        logger.debug("Updating budget for startDate: {}", startDate);
        validateBudgetDTO(dto);

        LocalDate periodStart = startDate.withDayOfMonth(1);
        Budget budget = repository.findByPeriodStart(periodStart)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found for " + periodStart));
        budget.setBudgetAmount(dto.getBudgetAmount());
        resetBudgetForPeriod(budget);
        Budget updated = repository.save(budget);
        logger.info("Budget updated for period starting on {}", periodStart);
        return updated;
    }

    public void deleteBudget(LocalDate startDate) {
        logger.debug("Deleting budget for startDate: {}", startDate);
        LocalDate periodStart = startDate.withDayOfMonth(1);
        Budget budget = repository.findByPeriodStart(periodStart)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found for " + periodStart));
        repository.delete(budget);
        logger.info("Budget deleted for period starting on {}", periodStart);
    }

    public Budget getBudget(LocalDate startDate) {
        LocalDate periodStart = startDate.withDayOfMonth(1);
        Budget budget = repository.findByPeriodStart(periodStart)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found for " + periodStart));
        resetBudgetForPeriod(budget);
        return budget;
    }

    public List<Budget> getAllBudgets() {
        logger.debug("Fetching all budgets");
        List<Budget> budgets = repository.findAll();
        budgets.forEach(this::resetBudgetForPeriod);
        return budgets;
    }

    public Budget resetBudgetForPeriod(Budget budget) {
        logger.debug("Resetting budget for period: {} to {}", budget.getPeriodStart(), budget.getPeriodEnd());
        Objects.requireNonNull(budget, "Budget cannot be null");

        LocalDate today = LocalDate.now();
        if (today.isAfter(budget.getPeriodEnd())) {
            if (budget.getRemainingFunds() > 0) {
                budget.setOverflowFund(budget.getOverflowFund() + budget.getRemainingFunds());
            } else if (budget.getRemainingFunds() < 0) {
                budget.setPreviousNegativeBalance(-budget.getRemainingFunds());
            }
            budget.setPeriodStart(budget.getPeriodStart().plusMonths(1));
            budget.setPeriodEnd(budget.getPeriodEnd().plusMonths(1));
        }

        double totalBills = billProcessor.fetchUpcomingBills(budget.getPeriodStart(), budget.getPeriodEnd());
        double totalTransactions = transactionProcessor.fetchTransactions(budget.getPeriodStart(), budget.getPeriodEnd());
        budget.setRemainingFunds(budget.getBudgetAmount() - totalBills - totalTransactions - budget.getPreviousNegativeBalance());
        budget.setPreviousNegativeBalance(0.0);

        // Notification for 80% completion
        if (budget.getRemainingFunds() < 0.2 * budget.getBudgetAmount()) {
            logger.info("Notification: Budget for period {} to {} is nearing completion. Remaining funds: {}",
                    budget.getPeriodStart(), budget.getPeriodEnd(), budget.getRemainingFunds());
        }

        logger.debug("Budget reset: bills={}, transactions={}, remainingFunds={}", totalBills, totalTransactions, budget.getRemainingFunds());
        return repository.save(budget);
    }

    public Budget findCurrentBudget(LocalDate date) {
        LocalDate periodStart = date.withDayOfMonth(1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        logger.debug("Searching for budget with period: {} to {}", periodStart, periodEnd);
        return repository.findByPeriodStartAndPeriodEnd(periodStart, periodEnd)
                .map(this::resetBudgetForPeriod)
                .orElse(null);
    }

    private void validateBudgetDTO(BudgetDTO dto) {
        if (dto.getBudgetAmount() <= 0) {
            throw new IllegalArgumentException("Budget amount must be positive");
        }
        if (dto.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
    }
}