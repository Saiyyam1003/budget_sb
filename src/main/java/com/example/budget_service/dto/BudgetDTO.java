package com.example.budget_service.dto;

import java.time.LocalDate;

public class BudgetDTO {
    private double budgetAmount;
    private LocalDate startDate;

    public double getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(double budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
}