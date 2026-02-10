package com.schuduler.programschuduler.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SchedulerStartupProbe {

    @PostConstruct
    public void onStart() {
        log.info("SchedulerStartupProbe initialized — scheduling should be enabled.");
        log.info("Server zone: {}", java.time.ZoneId.systemDefault());
    }
}
