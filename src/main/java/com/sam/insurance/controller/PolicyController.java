package com.sam.insurance.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sam.insurance.dto.PolicyRequest;
import com.sam.insurance.dto.PolicyResponse;
import com.sam.insurance.service.PolicyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> createPolicy(
            @Valid @RequestBody PolicyRequest request
    ) {
        PolicyResponse savedPolicy =
                policyService.createPolicy(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedPolicy);
    }

    @GetMapping
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {

        List<PolicyResponse> policies =
                policyService.getAllPolicies();

        return ResponseEntity.ok(policies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyResponse> getPolicyById(
            @PathVariable Long id
    ) {

        Optional<PolicyResponse> policy =
                policyService.getPolicyById(id);

        if (policy.isPresent()) {
            return ResponseEntity.ok(policy.get());
        }

        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<PolicyResponse> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody PolicyRequest request
    ) {

        Optional<PolicyResponse> policy =
                policyService.updatePolicy(id, request);

        if (policy.isPresent()) {
            return ResponseEntity.ok(policy.get());
        }

        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(
            @PathVariable Long id
    ) {

        boolean deleted = policyService.deletePolicy(id);

        if (deleted) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}