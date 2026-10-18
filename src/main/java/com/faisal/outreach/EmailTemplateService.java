package com.faisal.outreach;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailTemplateService {

    private static final int MAX_FIELD_LENGTH = 100;
    private static final Pattern UNSAFE_WHITESPACE = Pattern.compile("[\\s\\p{Cntrl}\\p{Zl}\\p{Zp}]+");

    private static final String INITIAL_SUBJECT =
            "Java Backend Engineer | 3.5 Yrs | Payments & Microservices – %s";

    private static final String INITIAL_BODY = """
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
            """;

    private static final String FOLLOW_UP_SUBJECT =
            "Following up | Java Backend Engineer – %s";

    private static final String FOLLOW_UP_BODY = """
            Hi %s,

            I wanted to follow up on my previous email regarding Java Backend Engineer opportunities at %s.

            I'm still interested in exploring suitable backend opportunities with your team. I have 3.5 years of experience working with Java, Spring Boot, microservices, Kafka, MySQL, Redis, and AWS, with experience building payment and booking systems.

            If there is a relevant opening, I'd really appreciate it if you could consider my profile.

            My resume is attached again for convenience.

            Thank you for your time, and I look forward to hearing from you.

            Best regards,
            %s
            Java Backend Engineer
            """;

    private final OutreachConfig config;

    public EmailTemplateService(OutreachConfig config) {
        this.config = config;
    }

    public EmailContent create(
            String recruiterName,
            String companyName) {

        Parties parties = parties(recruiterName, companyName);

        return new EmailContent(
                INITIAL_SUBJECT.formatted(parties.company()),
                INITIAL_BODY.formatted(parties.recruiter(), parties.company(), senderName())
        );
    }

    public EmailContent createFollowUp(
            String recruiterName,
            String companyName) {

        Parties parties = parties(recruiterName, companyName);

        return new EmailContent(
                FOLLOW_UP_SUBJECT.formatted(parties.company()),
                FOLLOW_UP_BODY.formatted(parties.recruiter(), parties.company(), senderName())
        );
    }

    private Parties parties(
            String recruiterName,
            String companyName) {

        String recruiter = clean(recruiterName);
        String company = clean(companyName);

        require(recruiter, "Recruiter name");
        require(company, "Company name");

        return new Parties(recruiter, company);
    }

    private void require(
            String value,
            String label) {

        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(label + " is required");
        }

        if (value.length() > MAX_FIELD_LENGTH) {
            throw new IllegalArgumentException(
                    label + " must be " + MAX_FIELD_LENGTH + " characters or fewer"
            );
        }
    }

    private String senderName() {

        String name = clean(config.getSenderName());

        if (!StringUtils.hasText(name)) {
            throw new IllegalStateException("Sender name is not configured");
        }

        return name;
    }

    private static String clean(String value) {

        if (value == null) {
            return "";
        }

        return UNSAFE_WHITESPACE
                .matcher(value)
                .replaceAll(" ")
                .trim();
    }

    private record Parties(String recruiter, String company) {
    }
}