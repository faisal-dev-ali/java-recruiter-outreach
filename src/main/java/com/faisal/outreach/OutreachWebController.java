package com.faisal.outreach;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OutreachWebController {

    private static final Logger log = LoggerFactory.getLogger(OutreachWebController.class);

    private static final String REDIRECT_HOME = "redirect:/ui";
    private static final String GENERIC_ERROR = "Something went wrong. Please try again.";
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private final OutreachService outreachService;
    private final OutreachConfig config;
    private final ApplicationStore applicationStore;

    public OutreachWebController(
            OutreachService outreachService,
            OutreachConfig config,
            ApplicationStore applicationStore) {

        this.outreachService = outreachService;
        this.config = config;
        this.applicationStore = applicationStore;
    }

    @GetMapping("/ui")
    public String showPage(Model model) {

        populateCommonModel(model);

        return "outreach";
    }

    @PostMapping("/ui/outreach")
    public String sendOutreach(
            @RequestParam(defaultValue = "") String recruiterName,
            @RequestParam(defaultValue = "") String recruiterEmail,
            @RequestParam(defaultValue = "") String companyName,
            RedirectAttributes redirectAttributes) {

        OutreachRequest form = new OutreachRequest(
                recruiterName.trim(),
                recruiterEmail.trim(),
                companyName.trim()
        );

        String validationError = validate(form);

        if (validationError != null) {
            log.warn("event=outreach_rejected reason=\"{}\"", validationError);
            redirectAttributes.addFlashAttribute("form", form);
            redirectAttributes.addFlashAttribute("actionError", validationError);

            return REDIRECT_HOME;
        }

        try {
            OutreachResult result = outreachService.process(
                    form.getRecruiterName(),
                    form.getRecruiterEmail(),
                    form.getCompanyName()
            );

            log.info("event=outreach_processed status={}", result.status());
            redirectAttributes.addFlashAttribute("result", result);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("form", form);
            redirectAttributes.addFlashAttribute("actionError", errorMessage("outreach_failed", null, e));
        }

        return REDIRECT_HOME;
    }

    @GetMapping("/ui/application/{id}")
    public String viewApplication(
            @PathVariable String id,
            Model model,
            RedirectAttributes redirectAttributes) {

        return applicationStore
                .findById(id)
                .map(application -> {

                    populateCommonModel(model);
                    model.addAttribute("selectedApplication", application);

                    return "outreach";
                })
                .orElseGet(() -> {

                    log.warn("event=application_not_found applicationId={}", id);
                    redirectAttributes.addFlashAttribute(
                            "actionError",
                            "Application not found. It may have been removed or lost after a restart."
                    );

                    return REDIRECT_HOME;
                });
    }

    @PostMapping("/ui/application/{id}/resend")
    public String resendApplication(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        return runAction("application_resend", id, () -> outreachService.resend(id), redirectAttributes);
    }

    @PostMapping("/ui/application/{id}/follow-up")
    public String followUpApplication(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        return runAction("application_follow_up", id, () -> outreachService.sendFollowUp(id), redirectAttributes);
    }

    private String runAction(
            String event,
            String applicationId,
            Supplier<OutreachResult> action,
            RedirectAttributes redirectAttributes) {

        try {
            OutreachResult result = action.get();

            log.info("event={} applicationId={} status={}", event, applicationId, result.status());
            redirectAttributes.addFlashAttribute("result", result);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("actionError", errorMessage(event + "_failed", applicationId, e));
        }

        return REDIRECT_HOME;
    }

    private String errorMessage(String event, String applicationId, Exception e) {

        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            log.warn("event={} applicationId={} reason=\"{}\"", event, applicationId, e.getMessage());

            return StringUtils.hasText(e.getMessage()) ? e.getMessage() : GENERIC_ERROR;
        }

        log.error("event={} applicationId={}", event, applicationId, e);

        return GENERIC_ERROR;
    }

    private String validate(OutreachRequest form) {

        if (!StringUtils.hasText(form.getRecruiterName())) {
            return "Recruiter name is required";
        }

        if (form.getRecruiterName().length() > MAX_NAME_LENGTH) {
            return "Recruiter name must be " + MAX_NAME_LENGTH + " characters or fewer";
        }

        if (!StringUtils.hasText(form.getRecruiterEmail())
                || form.getRecruiterEmail().length() > MAX_EMAIL_LENGTH
                || !EMAIL.matcher(form.getRecruiterEmail()).matches()) {
            return "Enter a valid recruiter email address";
        }

        if (!StringUtils.hasText(form.getCompanyName())) {
            return "Company name is required";
        }

        if (form.getCompanyName().length() > MAX_NAME_LENGTH) {
            return "Company name must be " + MAX_NAME_LENGTH + " characters or fewer";
        }

        return null;
    }

    private void populateCommonModel(Model model) {

        model.addAttribute("dryRun", config.isDryRun());
        model.addAttribute("applications", applicationStore.findAll());
    }
}