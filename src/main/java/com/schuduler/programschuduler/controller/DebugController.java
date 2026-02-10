package com.schuduler.programschuduler.controller;

import com.schuduler.programschuduler.dto.PrePlaybackEvent;
import com.schuduler.programschuduler.messaging.PrePlaybackEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final PrePlaybackEventPublisher publisher;

    @PostMapping("/publish-test")
    public ResponseEntity<String> publishTest(@RequestParam(defaultValue = "manual1") String id,
                                              @RequestParam(defaultValue = "u1") String userId) {

        PrePlaybackEvent event = PrePlaybackEvent.builder()
                .scheduleId(id)
                .userId(userId)
                .channel("debug-channel")
                .programUrl("http://example.com/video.mp4")
                .startAt(Instant.now().plusSeconds(5 * 60))
                .durationMin(30)
                .build();

        publisher.publish(event);
        return ResponseEntity.ok("Published test PrePlaybackEvent: " + id);
    }
}
