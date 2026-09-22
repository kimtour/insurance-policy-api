package com.sam.insurance.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sam.insurance.dto.PolicyRequest;
import com.sam.insurance.dto.PolicyResponse;
import com.sam.insurance.model.Policy;
import com.sam.insurance.repository.PolicyRepository;

@Service
public class PolicyService {

    private static final Logger logger =
            LoggerFactory.getLogger(PolicyService.class);

    private final PolicyRepository policyRepository;

    public PolicyService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public PolicyResponse createPolicy(PolicyRequest request) {

        logger.info(
                "Creating policy with policyNumber={}",
                request.getPolicyNumber()
        );

        validateDates(request);

        if (policyRepository.existsByPolicyNumber(request.getPolicyNumber())) {

            logger.warn(
                    "Policy creation rejected because policyNumber={} already exists",
                    request.getPolicyNumber()
            );

            throw new IllegalArgumentException(
                    "policyNumber already exists"
            );
        }

        Policy policy = new Policy(
                request.getPolicyNumber(),
                request.getCustomerName(),
                request.getCustomerEmail(),
                request.getPremium(),
                request.getStatus(),
                request.getStartDate(),
                request.getEndDate()
        );

        Policy savedPolicy = policyRepository.save(policy);

        logger.info(
                "Policy created successfully with id={} and policyNumber={}",
                savedPolicy.getId(),
                savedPolicy.getPolicyNumber()
        );

        return toResponse(savedPolicy);
    }

    public List<PolicyResponse> getAllPolicies() {

        logger.info("Fetching all policies");

        List<PolicyResponse> policies =
                policyRepository.findAll()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        logger.info(
                "Fetched {} policies",
                policies.size()
        );

        return policies;
    }

    public Optional<PolicyResponse> getPolicyById(Long id) {

        logger.info(
                "Fetching policy with id={}",
                id
        );

        Optional<PolicyResponse> policy =
                policyRepository.findById(id)
                        .map(this::toResponse);

        if (policy.isPresent()) {
            logger.info(
                    "Policy found with id={}",
                    id
            );
        } else {
            logger.warn(
                    "Policy not found with id={}",
                    id
            );
        }

        return policy;
    }

    public Optional<PolicyResponse> updatePolicy(
            Long id,
            PolicyRequest request
    ) {

        logger.info(
                "Updating policy with id={}",
                id
        );

        validateDates(request);

        Optional<Policy> existingPolicyOptional =
                policyRepository.findById(id);

        if (existingPolicyOptional.isEmpty()) {

            logger.warn(
                    "Policy update failed because id={} was not found",
                    id
            );

            return Optional.empty();
        }

        Optional<Policy> policyWithSameNumber =
                policyRepository.findByPolicyNumber(
                        request.getPolicyNumber()
                );

        if (
            policyWithSameNumber.isPresent()
            && !policyWithSameNumber.get().getId().equals(id)
        ) {

            logger.warn(
                    "Policy update rejected because policyNumber={} belongs to another policy",
                    request.getPolicyNumber()
            );

            throw new IllegalArgumentException(
                    "policyNumber already exists"
            );
        }

        Policy existingPolicy =
                existingPolicyOptional.get();

        existingPolicy.setPolicyNumber(
                request.getPolicyNumber()
        );

        existingPolicy.setCustomerName(
                request.getCustomerName()
        );

        existingPolicy.setCustomerEmail(
                request.getCustomerEmail()
        );

        existingPolicy.setPremium(
                request.getPremium()
        );

        existingPolicy.setStatus(
                request.getStatus()
        );

        existingPolicy.setStartDate(
                request.getStartDate()
        );

        existingPolicy.setEndDate(
                request.getEndDate()
        );

        Policy savedPolicy =
                policyRepository.save(existingPolicy);

        logger.info(
                "Policy updated successfully with id={} and policyNumber={}",
                savedPolicy.getId(),
                savedPolicy.getPolicyNumber()
        );

        return Optional.of(
                toResponse(savedPolicy)
        );
    }

    public boolean deletePolicy(Long id) {

        logger.info(
                "Deleting policy with id={}",
                id
        );

        if (!policyRepository.existsById(id)) {

            logger.warn(
                    "Policy deletion failed because id={} was not found",
                    id
            );

            return false;
        }

        policyRepository.deleteById(id);

        logger.info(
                "Policy deleted successfully with id={}",
                id
        );

        return true;
    }

    private void validateDates(
            PolicyRequest request
    ) {

        if (!request.getEndDate()
                .isAfter(request.getStartDate())) {

            logger.warn(
                    "Policy validation failed because endDate={} is not after startDate={}",
                    request.getEndDate(),
                    request.getStartDate()
            );

            throw new IllegalArgumentException(
                    "endDate must be after startDate"
            );
        }
    }

    private PolicyResponse toResponse(
            Policy policy
    ) {

        return new PolicyResponse(
                policy.getId(),
                policy.getPolicyNumber(),
                policy.getCustomerName(),
                policy.getCustomerEmail(),
                policy.getPremium(),
                policy.getStatus(),
                policy.getStartDate(),
                policy.getEndDate()
        );
    }
}