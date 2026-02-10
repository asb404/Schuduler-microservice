package com.schuduler.programschuduler.dto;

import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrePlaybackEvent {
    private String scheduleId;
    private String userId;
    private String channel;
    private String programUrl;
    private Instant startAt;
    private Integer durationMin;
}
