package com.sam.insurance.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

import com.sam.insurance.dto.PolicyRequest;
import com.sam.insurance.dto.PolicyResponse;
import com.sam.insurance.exception.GlobalExceptionHandler;
import com.sam.insurance.model.PolicyStatus;
import com.sam.insurance.security.SecurityConfig;
import com.sam.insurance.service.PolicyService;

@WebMvcTest(PolicyController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class
})
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private PolicyService policyService;

    @Test
    void createPolicyShouldReturnCreated() throws Exception {

        PolicyRequest request = createValidRequest();

        PolicyResponse response = createResponse(
                1L,
                "POL-100",
                new BigDecimal("2500.00")
        );

        when(
                policyService.createPolicy(
                        any(PolicyRequest.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                post("/api/policies")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                jsonMapper.writeValueAsString(
                                        request
                                )
                        )
        )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.policyNumber")
                                .value("POL-100")
                )
                .andExpect(
                        jsonPath("$.premium")
                                .value(2500.00)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );
    }

    @Test
    void getAllPoliciesShouldReturnPolicies() throws Exception {

        PolicyResponse response1 =
                createResponse(
                        1L,
                        "POL-100",
                        new BigDecimal("2500.00")
                );

        PolicyResponse response2 =
                createResponse(
                        2L,
                        "POL-200",
                        new BigDecimal("4000.00")
                );

        when(
                policyService.getAllPolicies()
        ).thenReturn(
                List.of(
                        response1,
                        response2
                )
        );

        mockMvc.perform(
                get("/api/policies")
                        .with(jwt())
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$[0].policyNumber"
                        ).value(
                                "POL-100"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$[1].policyNumber"
                        ).value(
                                "POL-200"
                        )
                );
    }

    @Test
    void getAllPoliciesShouldReturnUnauthorizedWithoutJwt()
            throws Exception {

        mockMvc.perform(
                get("/api/policies")
        )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void getPolicyByIdShouldReturnPolicyWhenFound()
            throws Exception {

        PolicyResponse response =
                createResponse(
                        1L,
                        "POL-100",
                        new BigDecimal("2500.00")
                );

        when(
                policyService.getPolicyById(
                        1L
                )
        ).thenReturn(
                Optional.of(response)
        );

        mockMvc.perform(
                get("/api/policies/1")
                        .with(jwt())
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.policyNumber")
                                .value("POL-100")
                );
    }

    @Test
    void getPolicyByIdShouldReturnNotFoundWhenMissing()
            throws Exception {

        when(
                policyService.getPolicyById(
                        999L
                )
        ).thenReturn(
                Optional.empty()
        );

        mockMvc.perform(
                get("/api/policies/999")
                        .with(jwt())
        )
                .andExpect(
                        status().isNotFound()
                );
    }

    @Test
    void updatePolicyShouldReturnUpdatedPolicy()
            throws Exception {

        PolicyRequest request =
                createValidRequest();

        request.setPremium(
                new BigDecimal("3000.00")
        );

        PolicyResponse response =
                createResponse(
                        1L,
                        "POL-100",
                        new BigDecimal("3000.00")
                );

        when(
                policyService.updatePolicy(
                        eq(1L),
                        any(PolicyRequest.class)
                )
        ).thenReturn(
                Optional.of(response)
        );

        mockMvc.perform(
                put("/api/policies/1")
                        .with(jwt())
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                jsonMapper.writeValueAsString(
                                        request
                                )
                        )
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.premium")
                                .value(3000.00)
                );
    }

    @Test
    void updatePolicyShouldReturnNotFoundWhenMissing()
            throws Exception {

        PolicyRequest request =
                createValidRequest();

        when(
                policyService.updatePolicy(
                        eq(999L),
                        any(PolicyRequest.class)
                )
        ).thenReturn(
                Optional.empty()
        );

        mockMvc.perform(
                put("/api/policies/999")
                        .with(jwt())
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                jsonMapper.writeValueAsString(
                                        request
                                )
                        )
        )
                .andExpect(
                        status().isNotFound()
                );
    }

    @Test
    void deletePolicyShouldReturnNoContentWhenDeleted()
            throws Exception {

        when(
                policyService.deletePolicy(
                        1L
                )
        ).thenReturn(true);

        mockMvc.perform(
                delete("/api/policies/1")
                        .with(jwt())
        )
                .andExpect(
                        status().isNoContent()
                )
                .andExpect(
                        content().string("")
                );
    }

    @Test
    void deletePolicyShouldReturnNotFoundWhenMissing()
            throws Exception {

        when(
                policyService.deletePolicy(
                        999L
                )
        ).thenReturn(false);

        mockMvc.perform(
                delete("/api/policies/999")
                        .with(jwt())
        )
                .andExpect(
                        status().isNotFound()
                );
    }

    @Test
    void createPolicyShouldReturnBadRequestForInvalidInput()
            throws Exception {

        PolicyRequest request =
                createValidRequest();

        request.setCustomerName("");
        request.setCustomerEmail(
                "wrong-email"
        );

        request.setPremium(
                new BigDecimal("-50")
        );

        mockMvc.perform(
                post("/api/policies")
                        .with(jwt())
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                jsonMapper.writeValueAsString(
                                        request
                                )
                        )
        )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.customerName")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.customerEmail")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.premium")
                                .exists()
                );
    }

    private PolicyRequest createValidRequest() {

        PolicyRequest request =
                new PolicyRequest();

        request.setPolicyNumber(
                "POL-100"
        );

        request.setCustomerName(
                "John Doe"
        );

        request.setCustomerEmail(
                "john@example.com"
        );

        request.setPremium(
                new BigDecimal("2500.00")
        );

        request.setStatus(
                PolicyStatus.ACTIVE
        );

        request.setStartDate(
                LocalDate.of(
                        2026,
                        9,
                        22
                )
        );

        request.setEndDate(
                LocalDate.of(
                        2027,
                        9,
                        21
                )
        );

        return request;
    }

    private PolicyResponse createResponse(
            Long id,
            String policyNumber,
            BigDecimal premium
    ) {

        return new PolicyResponse(
                id,
                policyNumber,
                "John Doe",
                "john@example.com",
                premium,
                PolicyStatus.ACTIVE,
                LocalDate.of(
                        2026,
                        9,
                        22
                ),
                LocalDate.of(
                        2027,
                        9,
                        21
                )
        );
    }
}