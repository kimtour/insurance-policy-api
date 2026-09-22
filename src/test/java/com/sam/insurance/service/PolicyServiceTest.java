package com.sam.insurance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sam.insurance.dto.PolicyRequest;
import com.sam.insurance.dto.PolicyResponse;
import com.sam.insurance.model.Policy;
import com.sam.insurance.model.PolicyStatus;
import com.sam.insurance.repository.PolicyRepository;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyService policyService;

    @Test
    void createPolicyShouldCreatePolicySuccessfully() {

        PolicyRequest request = createValidRequest();

        when(
                policyRepository.existsByPolicyNumber("POL-100")
        ).thenReturn(false);

        when(
                policyRepository.save(any(Policy.class))
        ).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyResponse response =
                policyService.createPolicy(request);

        assertEquals(
                "POL-100",
                response.getPolicyNumber()
        );

        assertEquals(
                new BigDecimal("2500.00"),
                response.getPremium()
        );

        assertEquals(
                PolicyStatus.ACTIVE,
                response.getStatus()
        );

        verify(policyRepository)
                .existsByPolicyNumber("POL-100");

        verify(policyRepository)
                .save(any(Policy.class));
    }

    @Test
    void createPolicyShouldRejectDuplicatePolicyNumber() {

        PolicyRequest request = createValidRequest();

        when(
                policyRepository.existsByPolicyNumber("POL-100")
        ).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> policyService.createPolicy(request)
                );

        assertEquals(
                "policyNumber already exists",
                exception.getMessage()
        );

        verify(policyRepository, never())
                .save(any(Policy.class));
    }

    @Test
    void createPolicyShouldRejectInvalidDates() {

        PolicyRequest request = createValidRequest();

        request.setStartDate(
                LocalDate.of(2027, 9, 22)
        );

        request.setEndDate(
                LocalDate.of(2026, 9, 21)
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> policyService.createPolicy(request)
                );

        assertEquals(
                "endDate must be after startDate",
                exception.getMessage()
        );

        verify(policyRepository, never())
                .save(any(Policy.class));
    }

    @Test
    void getPolicyByIdShouldReturnPolicyWhenItExists() {

        Policy policy = createPolicyEntity(
                1L,
                "POL-100",
                new BigDecimal("2500.00")
        );

        when(
                policyRepository.findById(1L)
        ).thenReturn(Optional.of(policy));

        Optional<PolicyResponse> response =
                policyService.getPolicyById(1L);

        assertTrue(response.isPresent());

        assertEquals(
                "POL-100",
                response.get().getPolicyNumber()
        );

        verify(policyRepository)
                .findById(1L);
    }

    @Test
    void updatePolicyShouldUpdateSuccessfully() {

        Policy existingPolicy = createPolicyEntity(
                1L,
                "POL-100",
                new BigDecimal("2500.00")
        );

        PolicyRequest request = createValidRequest();

        request.setPremium(
                new BigDecimal("3000.00")
        );

        when(
                policyRepository.findById(1L)
        ).thenReturn(Optional.of(existingPolicy));

        when(
                policyRepository.findByPolicyNumber("POL-100")
        ).thenReturn(Optional.of(existingPolicy));

        when(
                policyRepository.save(any(Policy.class))
        ).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<PolicyResponse> response =
                policyService.updatePolicy(1L, request);

        assertTrue(response.isPresent());

        assertEquals(
                new BigDecimal("3000.00"),
                response.get().getPremium()
        );

        verify(policyRepository)
                .save(existingPolicy);
    }

    @Test
    void updatePolicyShouldReturnEmptyWhenPolicyDoesNotExist() {

        PolicyRequest request = createValidRequest();

        when(
                policyRepository.findById(999L)
        ).thenReturn(Optional.empty());

        Optional<PolicyResponse> response =
                policyService.updatePolicy(999L, request);

        assertTrue(response.isEmpty());

        verify(policyRepository, never())
                .save(any(Policy.class));
    }

    @Test
    void updatePolicyShouldRejectDuplicatePolicyNumber() {

        Policy existingPolicy = createPolicyEntity(
                1L,
                "POL-100",
                new BigDecimal("2500.00")
        );

        Policy otherPolicy = createPolicyEntity(
                2L,
                "POL-200",
                new BigDecimal("4000.00")
        );

        PolicyRequest request = createValidRequest();

        request.setPolicyNumber("POL-200");

        when(
                policyRepository.findById(1L)
        ).thenReturn(Optional.of(existingPolicy));

        when(
                policyRepository.findByPolicyNumber("POL-200")
        ).thenReturn(Optional.of(otherPolicy));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> policyService.updatePolicy(1L, request)
                );

        assertEquals(
                "policyNumber already exists",
                exception.getMessage()
        );

        verify(policyRepository, never())
                .save(any(Policy.class));
    }

    @Test
    void deletePolicyShouldDeleteExistingPolicy() {

        when(
                policyRepository.existsById(1L)
        ).thenReturn(true);

        boolean deleted =
                policyService.deletePolicy(1L);

        assertTrue(deleted);

        verify(policyRepository)
                .deleteById(1L);
    }

    @Test
    void deletePolicyShouldReturnFalseWhenPolicyDoesNotExist() {

        when(
                policyRepository.existsById(999L)
        ).thenReturn(false);

        boolean deleted =
                policyService.deletePolicy(999L);

        assertFalse(deleted);

        verify(policyRepository, never())
                .deleteById(999L);
    }

    private PolicyRequest createValidRequest() {

        PolicyRequest request = new PolicyRequest();

        request.setPolicyNumber("POL-100");
        request.setCustomerName("John Doe");
        request.setCustomerEmail("john@example.com");

        request.setPremium(
                new BigDecimal("2500.00")
        );

        request.setStatus(
                PolicyStatus.ACTIVE
        );

        request.setStartDate(
                LocalDate.of(2026, 9, 22)
        );

        request.setEndDate(
                LocalDate.of(2027, 9, 21)
        );

        return request;
    }

    private Policy createPolicyEntity(
            Long id,
            String policyNumber,
            BigDecimal premium
    ) {

        Policy policy = new Policy(
                policyNumber,
                "John Doe",
                "john@example.com",
                premium,
                PolicyStatus.ACTIVE,
                LocalDate.of(2026, 9, 22),
                LocalDate.of(2027, 9, 21)
        );

        ReflectionTestUtils.setField(
                policy,
                "id",
                id
        );

        return policy;
    }
}