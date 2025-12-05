package com.taskmgmt.config;



import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "reminder")
public class ReminderProperties {

      //cron expression for scheduler default every 5 minutes

    private String cron = "0 0/5 * * * *";


     //number of days ahead to send "upcoming" reminders

    private int upcomingDays = 1;

    // getters & setters
    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }
    public int getUpcomingDays() { return upcomingDays; }
    public void setUpcomingDays(int upcomingDays) { this.upcomingDays = upcomingDays; }
}

