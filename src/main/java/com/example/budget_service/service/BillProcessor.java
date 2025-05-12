package com.example.budget_service.service;

import com.example.budget_service.dto.BillDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Component
public class BillProcessor {
    private static final Logger logger = LoggerFactory.getLogger(BillProcessor.class);
    @Value("${bill.service.url}")
    private String BILL_SERVICE_URL;

    private final RestTemplate restTemplate;

    @Autowired
    public BillProcessor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public double fetchUpcomingBills(LocalDate start, LocalDate end) {
        String url = BILL_SERVICE_URL + "?start=" + start + "&end=" + end;
        BillDTO[] bills = restTemplate.getForObject(url, BillDTO[].class);
        double total = 0.0;
        if (bills != null) {
            for (Object bill : bills) {
                if (!isBillPaid(bill)) {
                    total += getBillAmount(bill);
                }
            }
        }
        logger.debug("Fetched bills total: {} for period {} to {}", total, start, end);
        return total;
    }

    private double getBillAmount(Object bill) {
        try {
            return ((Number) bill.getClass().getMethod("getAmount").invoke(bill)).doubleValue();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to retrieve bill amount", e);
        }
    }

    private boolean isBillPaid(Object bill) {
        try {
            return (Boolean) bill.getClass().getMethod("isPaid").invoke(bill);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to retrieve bill paid status", e);
        }
    }
}