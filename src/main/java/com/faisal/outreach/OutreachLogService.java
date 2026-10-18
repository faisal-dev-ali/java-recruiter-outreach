package com.faisal.outreach;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class OutreachLogService {

    private static final String HEADER =
            "timestamp,recruiter_name,company_name,recipient,status,details";

    private final OutreachConfig config;

    public OutreachLogService(OutreachConfig config) {
        this.config = config;
    }

    public synchronized void append(
            String recruiter,
            String company,
            String recipient,
            String status,
            String details) {

        try {

            Path path = getLogPath();

            createParentDirectory(path);

            boolean needsHeader =
                    !Files.exists(path)
                            || Files.size(path) == 0;

            String row =
                    buildRow(
                            recruiter,
                            company,
                            recipient,
                            status,
                            details
                    );

            if (needsHeader) {

                row =
                        HEADER
                                + System.lineSeparator()
                                + row;
            }

            Files.writeString(
                    path,
                    row,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (Exception e) {

            /*
             * Logging must never cause the actual
             * outreach operation to fail.
             */
            System.err.println(
                    "WARNING: Could not write outreach log: "
                            + e.getMessage()
            );
        }
    }

    private Path getLogPath() {

        String configuredPath =
                config.getLogPath();

        if (configuredPath == null
                || configuredPath.isBlank()) {

            throw new IllegalStateException(
                    "Outreach log path is not configured. "
                            + "Set 'outreach.log-path' "
                            + "in application.properties."
            );
        }

        return Path.of(
                        configuredPath.trim()
                )
                .toAbsolutePath()
                .normalize();
    }

    private void createParentDirectory(
            Path path)
            throws IOException {

        Path parent =
                path.getParent();

        if (parent != null) {

            Files.createDirectories(parent);
        }
    }

    private String buildRow(
            String recruiter,
            String company,
            String recipient,
            String status,
            String details) {

        return String.join(
                ",",
                csv(
                        OffsetDateTime
                                .now(ZoneOffset.UTC)
                                .toString()
                ),
                csv(recruiter),
                csv(company),
                csv(recipient),
                csv(status),
                csv(details)
        )
                + System.lineSeparator();
    }

    private String csv(String value) {

        if (value == null) {
            return "\"\"";
        }

        String safe =
                value
                        .replace("\r", " ")
                        .replace("\n", " ");

        /*
         * CSV escaping.
         *
         * Faisal "Ali"
         *
         * becomes:
         *
         * "Faisal ""Ali"""
         */
        safe =
                safe.replace(
                        "\"",
                        "\"\""
                );

        /*
         * Prevent CSV / Excel formula injection.
         *
         * Examples:
         *
         * =HYPERLINK(...)
         * +SUM(...)
         * -1+1
         * @SUM(...)
         */
        if (!safe.isEmpty()
                && "=+-@".indexOf(
                safe.charAt(0)
        ) >= 0) {

            safe = "'" + safe;
        }

        return "\"" + safe + "\"";
    }
}