package com.schuduler.programschuduler.dto;

import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {
    private String id;
    private String userId;
    private String title;
    private String channel;
    private Instant startAt;
    private Integer durationMin; // ADDED
    private String recurrence;
    private String programUrl;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}