package com.faisal.outreach;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailTemplateService {

    private final OutreachConfig config;

    public EmailTemplateService(OutreachConfig config) {
        this.config = config;
    }

    public EmailContent create(
            String recruiterName,
            String companyName) {

        String safeRecruiter = clean(recruiterName);
        String safeCompany = clean(companyName);

        validate(safeRecruiter, safeCompany);

        String subject =
                "Java Backend Engineer | 3.5 Yrs | Payments & Microservices – "
                        + safeCompany;

        String body = """
                Hi %s,

                I'm a Java backend engineer with 3.5 years of experience building payment and booking systems for ICICI Bank and Kotak Mahindra Bank, reaching out about backend openings at %s.

                A few things I've delivered:
                - Owned a payment gateway integration end to end (async callbacks, reconciliation, retries, idempotency keys), eliminating duplicate transactions across services.
                - Shipped 5+ Spring Boot microservices handling 10K+ API requests/day; Redis caching cut product listing latency by ~30%%.
                - Hardened ClearTrip, MakeMyTrip and Yatra integrations with Resilience4j circuit breakers, retries and rate limiting.

                Stack: Java 17, Spring Boot, Kafka, MySQL, Redis, AWS.

                My resume is attached. If there is a relevant opening, I'd welcome a 15-minute conversation.

                Best regards,
                %s
                Java Backend Engineer
                """.formatted(
                safeRecruiter,
                safeCompany,
                config.getSenderName()
        );

        return new EmailContent(subject, body);
    }

    private void validate(
            String recruiterName,
            String companyName) {

        if (!StringUtils.hasText(recruiterName)) {
            throw new IllegalArgumentException(
                    "Recruiter name is required"
            );
        }

        if (!StringUtils.hasText(companyName)) {
            throw new IllegalArgumentException(
                    "Company name is required"
            );
        }
    }

    private String clean(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll("[\\r\\n]+", " ");
    }
}