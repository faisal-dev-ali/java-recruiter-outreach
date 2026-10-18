package com.faisal.outreach;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class OutreachService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;
    private final OutreachConfig config;
    private final OutreachLogService logService;
    private final ApplicationStore applicationStore;

    public OutreachService(
            JavaMailSender mailSender,
            EmailTemplateService templateService,
            OutreachConfig config,
            OutreachLogService logService,
            ApplicationStore applicationStore) {

        this.mailSender = mailSender;
        this.templateService = templateService;
        this.config = config;
        this.logService = logService;
        this.applicationStore = applicationStore;
    }

    /**
     * Creates and sends a NEW recruiter application.
     * A new JobApplication is created here.
     * Subsequent resend/follow-up operations reuse the same application ID.
     */
    public OutreachResult process(
            String recruiterName,
            String recruiterEmail,
            String companyName) {

        String normalizedEmail = normalizeEmail(recruiterEmail);

        EmailContent content = null;

        JobApplication application =
                applicationStore.create(
                        recruiterName,
                        normalizedEmail,
                        companyName
                );

        try {

            validateInput(
                    recruiterName,
                    normalizedEmail,
                    companyName
            );

            Resource resume = validateResume();

            content = templateService.create(
                    recruiterName.trim(),
                    companyName.trim()
            );

            /*
             * Preview mode.
             */
            if (config.isDryRun()) {

                OutreachResult result =
                        logAndReturn(
                                recruiterName,
                                companyName,
                                normalizedEmail,
                                OutreachStatus.DRY_RUN,
                                "Preview only; no email sent",
                                content
                        );

                applicationStore.update(
                        application.update(
                                result,
                                OutreachType.INITIAL
                        )
                );

                return result;
            }

            validateSmtpConfiguration();

            sendEmail(
                    normalizedEmail,
                    content,
                    resume
            );

            OutreachResult result =
                    logAndReturn(
                            recruiterName,
                            companyName,
                            normalizedEmail,
                            OutreachStatus.SENT,
                            "Email sent successfully",
                            content
                    );

            applicationStore.update(
                    application.update(
                            result,
                            OutreachType.INITIAL
                    )
            );

            return result;

        } catch (Exception e) {

            String details = conciseMessage(e);

            OutreachResult result =
                    logAndReturn(
                            recruiterName,
                            companyName,
                            normalizedEmail,
                            OutreachStatus.FAILED,
                            details,
                            content
                    );

            applicationStore.update(
                    application.update(
                            result,
                            OutreachType.INITIAL
                    )
            );

            return result;
        }
    }

    /**
     * Resends a FAILED application.
     * IMPORTANT:
     * The same application ID is retained.
     * A new application is NOT created.
     */
    public OutreachResult resend(String applicationId) {

        JobApplication application =
                applicationStore
                        .findById(applicationId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Application not found: "
                                                + applicationId
                                )
                        );

        if (application.status()
                != OutreachStatus.FAILED) {

            throw new IllegalStateException(
                    "Only failed applications can be resent."
            );
        }

        return sendForExistingApplication(
                application,
                OutreachType.RESEND
        );
    }

    /**
     * Sends a follow-up email for an existing SENT application.
     * The same application ID is retained.
     */
    public OutreachResult sendFollowUp(String applicationId) {

        JobApplication application =
                applicationStore
                        .findById(applicationId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Application not found: "
                                                + applicationId
                                )
                        );

        if (application.status()
                != OutreachStatus.SENT) {

            throw new IllegalStateException(
                    "Follow-up can only be sent for successfully sent applications."
            );
        }

        return sendForExistingApplication(
                application,
                OutreachType.FOLLOW_UP
        );
    }

    /**
     * Sends either a resend or a follow-up
     * for an existing application.
     * The existing application ID is always preserved.
     */
    private OutreachResult sendForExistingApplication(
            JobApplication application,
            OutreachType action) {

        String recruiterName =
                application.recruiterName();

        String recruiterEmail =
                application.recruiterEmail();

        String companyName =
                application.companyName();

        EmailContent content = null;

        try {

            validateInput(
                    recruiterName,
                    recruiterEmail,
                    companyName
            );

            /*
             * Follow-up and resend both use the
             * existing application's recruiter details.
             */
            Resource resume = validateResume();

            if (action == OutreachType.FOLLOW_UP) {

                content =
                        templateService.createFollowUp(
                                recruiterName,
                                companyName
                        );

            } else {

                content =
                        templateService.create(
                                recruiterName,
                                companyName
                        );
            }

            /*
             * Dry-run mode.
             */
            if (config.isDryRun()) {

                OutreachResult result =
                        logAndReturn(
                                recruiterName,
                                companyName,
                                recruiterEmail,
                                OutreachStatus.DRY_RUN,
                                getDryRunMessage(action),
                                content
                        );

                applicationStore.update(
                        application.update(
                                result,
                                action
                        )
                );

                return result;
            }

            validateSmtpConfiguration();

            sendEmail(
                    recruiterEmail,
                    content,
                    resume
            );

            OutreachResult result =
                    logAndReturn(
                            recruiterName,
                            companyName,
                            recruiterEmail,
                            OutreachStatus.SENT,
                            getSuccessMessage(action),
                            content
                    );

            applicationStore.update(
                    application.update(
                            result,
                            action
                    )
            );

            return result;

        } catch (Exception e) {

            String details =
                    conciseMessage(e);

            OutreachResult result =
                    logAndReturn(
                            recruiterName,
                            companyName,
                            recruiterEmail,
                            OutreachStatus.FAILED,
                            getFailureMessage(action, details),
                            content
                    );

            applicationStore.update(
                    application.update(
                            result,
                            action
                    )
            );

            return result;
        }
    }

    /**
     * Creates and sends an email with resume attachment.
     */
    private void sendEmail(
            String recruiterEmail,
            EmailContent content,
            Resource resume)
            throws Exception {

        MimeMessage message =
                mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(
                        message,
                        true,
                        "UTF-8"
                );

        helper.setFrom(
                config.getSmtpUsername(),
                config.getSenderName()
        );

        helper.setTo(recruiterEmail);

        helper.setSubject(
                content.subject()
        );

        helper.setText(
                content.body(),
                false
        );

        String filename =
                resume.getFilename();

        if (!StringUtils.hasText(filename)) {

            filename =
                    "Faisal_Ali_Software_Engineer_Resume.pdf";
        }

        helper.addAttachment(
                filename,
                resume
        );

        mailSender.send(message);
    }

    /**
     * Validates recruiter input.
     */
    private void validateInput(
            String recruiterName,
            String recruiterEmail,
            String companyName)
            throws AddressException {

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

        validateEmail(recruiterEmail);
    }

    /**
     * Validates recruiter email.
     */
    private void validateEmail(String email)
            throws AddressException {

        if (!StringUtils.hasText(email)) {

            throw new IllegalArgumentException(
                    "Recruiter email is required"
            );
        }

        InternetAddress address =
                new InternetAddress(email.trim());

        address.validate();
    }

    /**
     * Validates and resolves the resume resource.
     * Supports:
     * classpath:
     * classpath:resume/Faisal_Ali_Software_Engineer_Resume.pdf
     * or a filesystem path.
     */
    private Resource validateResume() {

        String resumePath =
                config.getResumePath();

        if (!StringUtils.hasText(resumePath)) {

            throw new IllegalStateException(
                    "Resume path is not configured. " +
                            "Set 'outreach.resume-path' " +
                            "in application.properties."
            );
        }

        Resource resource = getResource(resumePath);

        String filename =
                resource.getFilename();

        if (!StringUtils.hasText(filename)) {

            throw new IllegalStateException(
                    "Resume filename could not be determined."
            );
        }

        String normalizedFilename =
                filename.toLowerCase(Locale.ROOT);

        if (!normalizedFilename.endsWith(".pdf")) {

            throw new IllegalStateException(
                    "Resume must be a PDF file: "
                            + filename
            );
        }

        return resource;
    }

    private static @NonNull Resource getResource(String resumePath) {
        Resource resource;

        String configuredPath =
                resumePath.trim();

        if (configuredPath.startsWith("classpath:")) {

            String classpathLocation =
                    configuredPath.substring(
                            "classpath:".length()
                    );

            resource =
                    new ClassPathResource(
                            classpathLocation
                    );

        } else {

            resource =
                    new FileSystemResource(
                            configuredPath
                    );
        }

        if (!resource.exists()) {

            throw new IllegalStateException(
                    "Resume file does not exist: "
                            + configuredPath
            );
        }

        if (!resource.isReadable()) {

            throw new IllegalStateException(
                    "Resume file is not readable: "
                            + configuredPath
            );
        }
        return resource;
    }

    /**
     * Validates SMTP configuration before
     * attempting to send a real email.
     */
    private void validateSmtpConfiguration() {

        if (!StringUtils.hasText(
                config.getSmtpUsername())) {

            throw new IllegalStateException(
                    "SMTP username is not configured."
            );
        }

        if (!StringUtils.hasText(
                config.getSmtpPassword())) {

            throw new IllegalStateException(
                    "SMTP app password is not configured."
            );
        }
    }

    /**
     * Creates the result and writes the operation
     * to the outreach CSV log.
     */
    private OutreachResult logAndReturn(
            String recruiterName,
            String companyName,
            String recruiterEmail,
            OutreachStatus status,
            String details,
            EmailContent content) {

        logService.append(
                recruiterName,
                companyName,
                recruiterEmail,
                status.name(),
                details
        );

        String subject =
                content != null
                        ? content.subject()
                        : null;

        String body =
                content != null
                        ? content.body()
                        : null;

        return new OutreachResult(
                recruiterName,
                recruiterEmail,
                companyName,
                status,
                details,
                subject,
                body
        );
    }

    /**
     * Returns a normalized email address.
     */
    private String normalizeEmail(String email) {

        return email == null
                ? ""
                : email.trim();
    }

    /**
     * Returns a user-friendly dry-run message
     * depending on the operation.
     */
    private String getDryRunMessage(
            OutreachType action) {

        return switch (action) {

            case INITIAL ->
                    "Preview only; no email sent";

            case RESEND ->
                    "Resend preview only; no email sent";

            case FOLLOW_UP ->
                    "Follow-up preview only; no email sent";
        };
    }

    /**
     * Returns a success message depending
     * on the operation.
     */
    private String getSuccessMessage(
            OutreachType action) {

        return switch (action) {

            case INITIAL ->
                    "Email sent successfully";

            case RESEND ->
                    "Email resent successfully";

            case FOLLOW_UP ->
                    "Follow-up email sent successfully";
        };
    }

    /**
     * Returns a failure message depending
     * on the operation.
     */
    private String getFailureMessage(
            OutreachType action,
            String details) {

        return switch (action) {

            case INITIAL ->
                    details;

            case RESEND ->
                    "Resend failed: " + details;

            case FOLLOW_UP ->
                    "Follow-up failed: " + details;
        };
    }

    /**
     * Extracts the root exception and returns
     * a concise, readable error message.
     */
    private String conciseMessage(Exception e) {

        Throwable rootCause = e;

        while (rootCause.getCause() != null) {

            rootCause =
                    rootCause.getCause();
        }

        String message =
                rootCause.getMessage();

        if (!StringUtils.hasText(message)) {

            return rootCause
                    .getClass()
                    .getSimpleName();
        }

        return rootCause
                .getClass()
                .getSimpleName()
                + ": "
                + message
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }
}