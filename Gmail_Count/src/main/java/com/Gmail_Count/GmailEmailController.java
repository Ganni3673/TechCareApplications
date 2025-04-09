package com.Gmail_Count;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RestController
public class GmailEmailController {

    @Autowired
    private GmailEmailService gmailEmailService;

    @GetMapping("/email/count")
    public String getEmailCount() {
        try {
            int count = gmailEmailService.fetchTotalEmailCount();
            return "Total emails: " + count;
        } catch (IOException | GeneralSecurityException e) {
            return "Error fetching email count: " + e.getMessage();
        }
    }
}
