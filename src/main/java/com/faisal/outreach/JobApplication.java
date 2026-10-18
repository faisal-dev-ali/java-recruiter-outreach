package com.faisal.outreach;

import java.time.OffsetDateTime;

public record JobApplication(
        String id,
        String recruiterName,
        String recruiterEmail,
        String companyName,

        OutreachStatus status,
        OutreachType lastAction,

        String subject,
        String body,
        String details,

        int attempts,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * Creates the initial application state.
     */
    public static JobApplication initial(
            String id,
            String recruiterName,
            String recruiterEmail,
            String companyName) {

        OffsetDateTime now =
                OffsetDateTime.now();

        return new JobApplication(
                id,
                recruiterName,
                recruiterEmail,
                companyName,
                OutreachStatus.FAILED,
                OutreachType.INITIAL,
                null,
                null,
                "Application created",
                0,
                now,
                now
        );
    }

    /**
     * Updates an existing application after an action.
     * IMPORTANT:
     * The application ID, recruiter details and createdAt never change.
     * Only the outcome of the action is taken from the result.
     */
    public JobApplication update(
            OutreachResult result,
            OutreachType action) {

        return new JobApplication(
                id,
                recruiterName,
                recruiterEmail,
                companyName,
                result.status(),
                action,
                result.subject(),
                result.body(),
                result.details(),
                attempts + 1,
                createdAt,
                OffsetDateTime.now()
        );
    }
}