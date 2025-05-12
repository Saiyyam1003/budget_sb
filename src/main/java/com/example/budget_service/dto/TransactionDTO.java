package com.example.budget_service.dto;

import java.time.LocalDate;

public class TransactionDTO {
private double amount;
private LocalDate date;
private String category;

public double getAmount() {
return amount;
}

public void setAmount(double amount) {
this.amount = amount;
}

public LocalDate getDate() {
return date;
}

public void setDate(LocalDate date) {
this.date = date;
}

public String getCategory() {
return category;
}

public void setCategory(String category) {
this.category = category;
}
}