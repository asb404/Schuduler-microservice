package com.schuduler.programschuduler.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record SchedulerNowResponse(
        String status,                 // PLAY, NONE, OVERLAP
        ScheduleEntry entry,
        ScheduleEntry nextEntry,
        Integer delayMin,
        List<String> options
) {
    public record ScheduleEntry(
            String id,
            String videoUrl,
            OffsetDateTime startTime,
            Integer durationMin,
            Integer skipStartMin
    ) {}
}