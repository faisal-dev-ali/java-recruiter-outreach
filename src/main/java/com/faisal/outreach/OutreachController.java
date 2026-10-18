package com.faisal.outreach;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outreach")
public class OutreachController {

    private final OutreachService outreachService;

    public OutreachController(OutreachService outreachService) {
        this.outreachService = outreachService;
    }

    @PostMapping("/send")
    public ResponseEntity<OutreachResult> sendEmail(
            @RequestBody OutreachRequest request) {

        OutreachResult result = outreachService.process(
                request.getRecruiterName(),
                request.getRecruiterEmail(),
                request.getCompanyName()
        );

        return ResponseEntity.ok(result);
    }
}
