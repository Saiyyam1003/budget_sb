// package com.example.budget_service.service;

// import com.example.budget_service.dto.BudgetDTO;
// import com.example.budget_service.dto.BillDTO;
// import com.example.budget_service.model.Budget;
// import com.example.budget_service.repository.BudgetRepository;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestClientException;
// import org.springframework.web.client.RestTemplate;

// import java.time.LocalDate;
// import java.util.List;
// import java.util.Objects;

// @Service
// public class BudgetService {
//     private static final Logger logger = LoggerFactory.getLogger(BudgetService.class);
//     private static final String BILL_SERVICE_URL = "http://localhost:8083/api/bills";

//     private final BudgetRepository repository;
//     private final RestTemplate restTemplate;

//     @Autowired
//     public BudgetService(BudgetRepository repository, RestTemplate restTemplate) {
//         this.repository = repository;
//         this.restTemplate = restTemplate;
//     }

//     public Budget createBudget(BudgetDTO dto) {
//         logger.debug("Creating budget for period: {} to {}", dto.getPeriodStart(), dto.getPeriodEnd());
//         validateBudgetDTO(dto);

//         Budget budget = new Budget();
//         budget.setBudgetAmount(dto.getBudgetAmount());
//         budget.setPeriodStart(dto.getPeriodStart());
//         budget.setPeriodEnd(dto.getPeriodEnd());
//         budget.setRemainingFunds(dto.getBudgetAmount());
//         budget.setOverflowFund(0.0);
//         budget.setPreviousNegativeBalance(0.0);

//         try {
//             resetBudgetForPeriod(budget);
//             Budget saved = repository.save(budget);
//             logger.info("Budget created with ID: {}", saved.getId());
//             return saved;
//         } catch (Exception e) {
//             logger.error("Failed to create budget: {}", e.getMessage(), e);
//             throw new RuntimeException("Unable to create budget", e);
//         }
//     }

//     public Budget updateBudget(Long id, BudgetDTO dto) {
//         logger.debug("Updating budget ID: {}", id);
//         validateBudgetDTO(dto);

//         Budget budget = repository.findById(id)
//                 .orElseThrow(() -> new IllegalArgumentException("Budget not found with ID: " + id));
//         budget.setBudgetAmount(dto.getBudgetAmount());
//         try {
//             resetBudgetForPeriod(budget);
//             Budget updated = repository.save(budget);
//             logger.info("Budget ID: {} updated", id);
//             return updated;
//         } catch (Exception e) {
//             logger.error("Failed to update budget ID: {}: {}", id, e.getMessage(), e);
//             throw new RuntimeException("Unable to update budget", e);
//         }
//     }

//     public Budget processTransaction(Object transaction) {
//         logger.debug("Processing transaction");
//         validateTransaction(transaction);

//         LocalDate transactionDate = getTransactionDate(transaction);
//         Budget budget = findCurrentBudget(transactionDate);
//         if (budget == null) {
//             throw new IllegalStateException("No budget found for date: " + transactionDate);
//         }

//         try {
//             double transactionAmount = getTransactionAmount(transaction);
//             double newRemaining = budget.getRemainingFunds() - transactionAmount;
//             if (newRemaining <= 0) {
//                 double deficit = -newRemaining;
//                 if (budget.getOverflowFund() >= deficit) {
//                     budget.setOverflowFund(budget.getOverflowFund() - deficit);
//                     newRemaining = 0;
//                 }
//             }
//             budget.setRemainingFunds(newRemaining);
//             Budget updated = repository.save(budget);
//             logger.info("Transaction processed, budget ID: {}, remainingFunds: {}", updated.getId(), updated.getRemainingFunds());
//             return updated;
//         } catch (Exception e) {
//             logger.error("Failed to process transaction: {}", e.getMessage(), e);
//             throw new RuntimeException("Unable to process transaction", e);
//         }
//     }

//     public Budget resetBudgetForPeriod(Budget budget) {
//         logger.debug("Resetting budget for period: {} to {}", budget.getPeriodStart(), budget.getPeriodEnd());
//         Objects.requireNonNull(budget, "Budget cannot be null");

//         LocalDate today = LocalDate.now();
//         if (today.isAfter(budget.getPeriodEnd())) {
//             if (budget.getRemainingFunds() > 0) {
//                 budget.setOverflowFund(budget.getOverflowFund() + budget.getRemainingFunds());
//             } else if (budget.getRemainingFunds() < 0) {
//                 budget.setPreviousNegativeBalance(-budget.getRemainingFunds());
//             }
//             budget.setPeriodStart(budget.getPeriodStart().plusMonths(1));
//             budget.setPeriodEnd(budget.getPeriodEnd().plusMonths(1));
//         }

//         double totalBills = fetchUpcomingBills(budget.getPeriodStart(), budget.getPeriodEnd());
//         budget.setRemainingFunds(budget.getBudgetAmount() - totalBills - budget.getPreviousNegativeBalance());
//         budget.setPreviousNegativeBalance(0.0);
//         return repository.save(budget);
//     }

//     private double fetchUpcomingBills(LocalDate start, LocalDate end) {
//         String url = BILL_SERVICE_URL + "?start=" + start + "&end=" + end;
//         try {
//             BillDTO[] bills = restTemplate.getForObject(url, BillDTO[].class);
//             if (bills == null || bills.length == 0) {
//                 logger.debug("No bills found for period {} to {}", start, end);
//                 return 0.0;
//             }
//             double total = 0.0;
//             for (BillDTO bill : bills) {
//                 if (!bill.isPaid()) {
//                     total += bill.getAmount();
//                 }
//             }
//             logger.debug("Fetched bills total: {} for period {} to {}", total, start, end);
//             return total;
//         } catch (RestClientException e) {
//             logger.warn("Bill service unavailable at {}: {}. Assuming no bills.", url, e.getMessage());
//             return 0.0;
//         } catch (Exception e) {
//             logger.error("Unexpected error fetching bills: {}", e.getMessage(), e);
//             return 0.0;
//         }
//     }

//     private Budget findCurrentBudget(LocalDate date) {
//         LocalDate periodStart = date.withDayOfMonth(1);
//         LocalDate periodEnd = date.withDayOfMonth(date.lengthOfMonth());
//         return repository.findByPeriodStartAndPeriodEnd(periodStart, periodEnd)
//                 .map(this::resetBudgetForPeriod)
//                 .orElse(null);
//     }

//     public List<Budget> getAllBudgets() {
//         logger.debug("Fetching all budgets");
//         try {
//             List<Budget> budgets = repository.findAll();
//             budgets.forEach(this::resetBudgetForPeriod);
//             return budgets;
//         } catch (Exception e) {
//             logger.error("Failed to fetch budgets: {}", e.getMessage(), e);
//             throw new RuntimeException("Unable to fetch budgets", e);
//         }
//     }

//     private void validateBudgetDTO(BudgetDTO dto) {
//         if (dto.getBudgetAmount() <= 0) {
//             throw new IllegalArgumentException("Budget amount must be positive");
//         }
//         if (dto.getPeriodStart() == null || dto.getPeriodEnd() == null) {
//             throw new IllegalArgumentException("Period start and end dates are required");
//         }
//         if (dto.getPeriodStart().isAfter(dto.getPeriodEnd())) {
//             throw new IllegalArgumentException("Period start must be before period end");
//         }
//     }

//     private void validateTransaction(Object transaction) {
//         if (getTransactionAmount(transaction) <= 0) {
//             throw new IllegalArgumentException("Transaction amount must be positive");
//         }
//         if (getTransactionDate(transaction) == null) {
//             throw new IllegalArgumentException("Transaction date is required");
//         }
//         if (getTransactionCategory(transaction) == null || getTransactionCategory(transaction).trim().isEmpty()) {
//             throw new IllegalArgumentException("Transaction category is required");
//         }
//     }

//     private double getTransactionAmount(Object transaction) {
//         try {
//             return ((Number) transaction.getClass().getMethod("getAmount").invoke(transaction)).doubleValue();
//         } catch (Exception e) {
//             throw new IllegalArgumentException("Unable to retrieve transaction amount", e);
//         }
//     }

//     private LocalDate getTransactionDate(Object transaction) {
//         try {
//             return (LocalDate) transaction.getClass().getMethod("getDate").invoke(transaction);
//         } catch (Exception e) {
//             throw new IllegalArgumentException("Unable to retrieve transaction date", e);
//         }
//     }

//     private String getTransactionCategory(Object transaction) {
//         try {
//             return (String) transaction.getClass().getMethod("getCategory").invoke(transaction);
//         } catch (Exception e) {
//             throw new IllegalArgumentException("Unable to retrieve transaction category", e);
//         }
//     }
// }