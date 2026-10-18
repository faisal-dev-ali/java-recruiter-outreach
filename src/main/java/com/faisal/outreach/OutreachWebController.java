package com.faisal.outreach;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OutreachWebController {

    private final OutreachService outreachService;
    private final OutreachConfig config;

    public OutreachWebController(
            OutreachService outreachService,
            OutreachConfig config) {
        this.outreachService = outreachService;
        this.config = config;
    }

    @GetMapping("/ui")
    public String showPage(Model model) {

        model.addAttribute("dryRun", config.isDryRun());

        return "outreach";
    }

    @PostMapping("/ui/outreach")
    public String sendOutreach(
            @RequestParam String recruiterName,
            @RequestParam String recruiterEmail,
            @RequestParam String companyName,
            Model model) {

        OutreachResult result =
                outreachService.process(
                        recruiterName,
                        recruiterEmail,
                        companyName
                );

        model.addAttribute("dryRun", config.isDryRun());
        model.addAttribute("result", result);

        return "outreach";
    }
}