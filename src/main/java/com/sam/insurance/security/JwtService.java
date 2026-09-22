package com.sam.insurance.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    public JwtService(
            JwtEncoder jwtEncoder
    ) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateToken(
            String username
    ) {

        Instant now = Instant.now();

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer("insurance-policy-api")
                        .issuedAt(now)
                        .expiresAt(
                                now.plus(
                                        1,
                                        ChronoUnit.HOURS
                                )
                        )
                        .subject(username)
                        .claim(
                                "scope",
                                "policies.read policies.write"
                        )
                        .build();

        JwsHeader header =
                JwsHeader.with(
                        MacAlgorithm.HS256
                ).build();

        JwtEncoderParameters parameters =
                JwtEncoderParameters.from(
                        header,
                        claims
                );

        return jwtEncoder
                .encode(parameters)
                .getTokenValue();
    }
}