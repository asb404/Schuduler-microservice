package com.schuduler.programschuduler.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRequest {

    @NotBlank(message = "userId is required")
    @Size(max = 100)
    private String userId;

    @NotBlank(message = "title is required")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "channel is required")
    @Size(max = 100)
    private String channel;

    @NotBlank(message = "date is required (yyyy-MM-dd)")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "date must be yyyy-MM-dd")
    private String date;

    @NotBlank(message = "time is required (HH:mm)")
    @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "time must be HH:mm")
    private String time;

    // ADDED FIELD
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMin; 

    @NotBlank(message = "recurrence is required")
    @Pattern(regexp = "NONE|DAILY|WEEKLY|MONTHLY", message = "recurrence must be NONE/DAILY/WEEKLY/MONTHLY")
    private String recurrence;

    @Size(max = 1000)
    private String programUrl;

    @Size(max = 2000)
    private String notes;
}