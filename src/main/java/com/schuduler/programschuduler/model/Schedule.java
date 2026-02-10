package com.schuduler.programschuduler.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {
    @Id
    private String id;

    private String userId;
    private String title;
    private String channel;

    private Instant startAt;

    private Integer durationMin;

    private Recurrence recurrence;
    private String programUrl;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    private Boolean preplayPublished;

    public enum Recurrence { NONE, DAILY, WEEKLY, MONTHLY }
}
