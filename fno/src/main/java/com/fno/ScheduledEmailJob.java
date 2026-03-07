package com.fno;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledEmailJob {


    // Runs every 15 minutes
    // 15 minutes = 900000 ms
    @Scheduled(fixedRate = 120000)
    public void sendScheduledEmail() {
    	NseFuturesAutomationParallel1.startTheScraper();
    }
}