package com.faisal.outreach;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ApplicationStore {

    private final ConcurrentMap<String, JobApplication> applications =
            new ConcurrentHashMap<>();

    /**
     * Creates and stores a new job application.
     */
    public JobApplication create(
            String recruiterName,
            String recruiterEmail,
            String companyName) {

        String id = UUID.randomUUID().toString();

        JobApplication application =
                JobApplication.initial(
                        id,
                        recruiterName,
                        recruiterEmail,
                        companyName
                );

        applications.put(id, application);

        return application;
    }

    /**
     * Updates an existing application.
     * The application ID must already exist.
     */
    public void update(
            JobApplication application) {

        if (application == null) {
            throw new IllegalArgumentException(
                    "Application cannot be null."
            );
        }

        if (application.id() == null
                || application.id().isBlank()) {

            throw new IllegalArgumentException(
                    "Application ID is required."
            );
        }

        applications.computeIfPresent(
                application.id(),
                (id, existing) -> application
        );
    }

    /**
     * Finds an application by ID.
     */
    public Optional<JobApplication> findById(
            String id) {

        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                applications.get(id)
        );
    }

    /**
     * Returns all applications ordered
     * from newest to oldest.
     */
    public List<JobApplication> findAll() {

        return applications.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                JobApplication::createdAt,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .toList();
    }
}