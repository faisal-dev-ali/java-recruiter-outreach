package com.faisal.outreach;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
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

    public OutreachService(
            JavaMailSender mailSender,
            EmailTemplateService templateService,
            OutreachConfig config,
            OutreachLogService logService) {

        this.mailSender = mailSender;
        this.templateService = templateService;
        this.config = config;
        this.logService = logService;
    }

    public OutreachResult process(
            String recruiterName,
            String recruiterEmail,
            String companyName) {

        String normalizedEmail =
                normalizeEmail(recruiterEmail);

        EmailContent content = null;

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
             * Dry-run mode:
             * Generate the email but do not send it.
             */
            if (config.isDryRun()) {

                return logAndReturn(
                        recruiterName,
                        companyName,
                        normalizedEmail,
                        OutreachStatus.DRY_RUN,
                        "Preview only; no email sent",
                        content
                );
            }

            validateSmtpConfiguration();

            sendEmail(
                    normalizedEmail,
                    content,
                    resume
            );

            return logAndReturn(
                    recruiterName,
                    companyName,
                    normalizedEmail,
                    OutreachStatus.SENT,
                    "Email sent successfully",
                    content
            );

        } catch (Exception e) {

            String details =
                    conciseMessage(e);

            return logAndReturn(
                    recruiterName,
                    companyName,
                    normalizedEmail,
                    OutreachStatus.FAILED,
                    details,
                    content
            );
        }
    }

    /**
     * Creates and sends the email with the resume attachment.
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
     * Validates recruiter email address.
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
     * Loads the resume either from the classpath
     * or from the local filesystem.
     *
     * Supported examples:
     *
     * classpath:resume/Faisal_Ali_Software_Engineer_Resume.pdf
     *
     * /Users/faisalali/Downloads/resume.pdf
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

    /**
     * Validates Gmail SMTP configuration.
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
     * Writes the outreach result to the log
     * and creates the response returned to the caller.
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
     * Normalizes recruiter email input.
     */
    private String normalizeEmail(String email) {

        return email == null
                ? ""
                : email.trim();
    }

    /**
     * Converts an exception into a short,
     * single-line message suitable for logs/UI.
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

        /*
         * Keep errors short and single-line.
         * Never expose SMTP credentials.
         */
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