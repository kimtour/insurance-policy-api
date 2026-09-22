package com.sam.insurance.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sam.insurance.model.Policy;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    boolean existsByPolicyNumber(String policyNumber);

    Optional<Policy> findByPolicyNumber(String policyNumber);
}