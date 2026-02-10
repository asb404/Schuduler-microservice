package com.schuduler.programschuduler.controller;

import com.schuduler.programschuduler.dto.ScheduleRequest;
import com.schuduler.programschuduler.dto.ScheduleResponse;
import com.schuduler.programschuduler.dto.SchedulerNowResponse;
import com.schuduler.programschuduler.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/schedules")
@Validated
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ScheduleController {

    private final ScheduleService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ScheduleResponse> create(@Valid @RequestBody ScheduleRequest req) {
        log.info("ScheduleController.create called with: " + req);
        return service.createSchedule(req);
    }

    // --- NEW ENDPOINT FOR PLAYBACK SERVICE ---
    @GetMapping(value = "/now", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<SchedulerNowResponse> getNow(@RequestParam("userId") String userId) {
        return service.getNowPlaying(userId);
    }
    // -----------------------------------------

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ScheduleResponse> list(@RequestParam(value = "userId", required = false) String userId,
                                       @RequestParam(value = "channel", required = false) String channel) {
        if (userId != null && !userId.isBlank()) {
            return service.listByUserId(userId);
        }
        if (channel != null && !channel.isBlank()) {
            return service.listByChannel(channel);
        }
        return service.listAll();
    }

    @GetMapping(value = "/upcoming", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ScheduleResponse> upcoming(@RequestParam(value = "userId", required = false) String userId,
                                        @RequestParam(value = "channel", required = false) String channel,
                                        @RequestParam(value = "limit", required = false) Integer limit) {
        return service.upcoming(userId, channel, limit);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ScheduleResponse> get(@PathVariable String id) {
        return service.getById(id);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable String id) {
        return service.delete(id);
    }
}