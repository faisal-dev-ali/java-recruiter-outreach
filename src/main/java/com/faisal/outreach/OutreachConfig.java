package com.faisal.outreach;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class OutreachConfig {

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @Value("${outreach.sender-name:Faisal Ali}")
    private String senderName;

    @Value("${outreach.resume-path:}")
    private String resumePath;

    @Value("${outreach.log-path:outreach-log.csv}")
    private String logPath;

    @Value("${outreach.dry-run:true}")
    private boolean dryRun;

}