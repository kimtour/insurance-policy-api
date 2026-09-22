package com.sam.insurance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.sam.insurance.model.PolicyStatus;

public class PolicyResponse {

    private Long id;
    private String policyNumber;
    private String customerName;
    private String customerEmail;
    private BigDecimal premium;
    private PolicyStatus status;
    private LocalDate startDate;
    private LocalDate endDate;

    public PolicyResponse(
            Long id,
            String policyNumber,
            String customerName,
            String customerEmail,
            BigDecimal premium,
            PolicyStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.premium = premium;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}