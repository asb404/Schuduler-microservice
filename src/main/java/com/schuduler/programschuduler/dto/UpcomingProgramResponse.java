package com.schuduler.programschuduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpcomingProgramResponse {
    private String scheduleId;
    private String userId;
    private String title;
    private String channel;
    private ZonedDateTime nextOccurrence;
    private String recurrence;
    private String programUrl;
    private String notes;
}
