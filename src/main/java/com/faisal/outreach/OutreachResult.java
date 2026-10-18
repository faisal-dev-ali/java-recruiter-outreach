package com.faisal.outreach;

public record OutreachResult(
        String recruiterName,
        String companyName,
        String recruiterEmail,
        OutreachStatus status,
        String details,
        String subject,
        String body
) {
}