package com.example.budget_service.service;

import com.example.budget_service.dto.TransactionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Component
public class TransactionProcessor {
    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);
    private static final String TRANSACTION_SERVICE_URL = "http://localhost:8081/api/transactions";

    private final RestTemplate restTemplate;

    @Autowired
    public TransactionProcessor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public double fetchTransactions(LocalDate start, LocalDate end) {
        String url = TRANSACTION_SERVICE_URL + "?start=" + start + "&end=" + end;
        try {
            TransactionDTO[] transactions = restTemplate.getForObject(url, TransactionDTO[].class);
            double total = 0.0;
            if (transactions != null) {
                for (TransactionDTO transaction : transactions) {
                    if (transaction.getDate() != null &&
                            !transaction.getDate().isBefore(start) &&
                            !transaction.getDate().isAfter(end)) {
                        total += transaction.getAmount();
                        logger.debug("Processed transaction: amount={}, date={}, category={}",
                                transaction.getAmount(), transaction.getDate(), transaction.getCategory());
                    }
                }
            }
            logger.debug("Fetched transactions total: {} for period {} to {}", total, start, end);
            return total;
        } catch (RestClientException e) {
            logger.warn("Transaction service unavailable at {}: {}. Assuming no transactions.", url, e.getMessage());
            return 0.0;
        }
    }
}