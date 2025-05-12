package com.example.budget_service.repository;

import com.example.budget_service.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByPeriodStartAndPeriodEnd(LocalDate periodStart, LocalDate periodEnd);
    Optional<Budget> findByPeriodStart(LocalDate periodStart);
}