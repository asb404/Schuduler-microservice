package com.schuduler.programschuduler.service;

import com.schuduler.programschuduler.dto.ScheduleRequest;
import com.schuduler.programschuduler.dto.ScheduleResponse;
import com.schuduler.programschuduler.dto.SchedulerNowResponse;
import com.schuduler.programschuduler.dto.PrePlaybackEvent;
import com.schuduler.programschuduler.messaging.PrePlaybackEventPublisher;
import com.schuduler.programschuduler.model.Schedule;
import com.schuduler.programschuduler.repository.ScheduleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    private final ScheduleRepository repository;
    private final PrePlaybackEventPublisher eventPublisher;
    private final ReactiveMongoTemplate mongo;

    @Value("${scheduler.poll.rate:60000}")
    private long pollRateMs;              

    private final AtomicReference<Instant> lastScan = new AtomicReference<>(null);
    private final AtomicInteger scanCount = new AtomicInteger(0);

    private static Instant parseStartAt(String dateIso, String timeHHmm) {
        LocalDate date = LocalDate.parse(dateIso);
        LocalTime time = LocalTime.parse(timeHHmm);
        LocalDateTime ldt = LocalDateTime.of(date, time);
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
        return zdt.toInstant();
    }

    public Mono<ScheduleResponse> createSchedule(ScheduleRequest req) {
        Instant startAt = parseStartAt(req.getDate(), req.getTime());
        int duration = (req.getDurationMin() != null) ? req.getDurationMin() : 30;

        Schedule schedule = Schedule.builder()
                .userId(req.getUserId())
                .title(req.getTitle())
                .channel(req.getChannel())
                .startAt(startAt)
                .durationMin(duration)
                .recurrence(Schedule.Recurrence.valueOf(req.getRecurrence()))
                .programUrl(req.getProgramUrl())
                .preplayPublished(false) 
                .notes(req.getNotes())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        log.info("[SCHEDULER-DEBUG] creating schedule userId={} title={} startAt={}", schedule.getUserId(), schedule.getTitle(), schedule.getStartAt());
        return repository.save(schedule).map(toResponse());
    }

    @Scheduled(fixedRateString = "${scheduler.poll.rate:60000}")
    public void emitPrePlaybackEvents() {
        Instant now = Instant.now();

        lastScan.set(now);
        int currentCount = scanCount.incrementAndGet();

        Instant fiveMinFromNow = now.plusSeconds(5 * 60);
        Instant sixMinFromNow = now.plusSeconds(6 * 60); 
        log.info("[SCHEDULER] heartbeat scan#{} at {}", currentCount, now);
        log.info("[SCHEDULER] scan#{} scanning for events between {} and {}", currentCount, fiveMinFromNow, sixMinFromNow);

        repository.findAll()
                .filter(s -> s.getStartAt() != null
                        && !s.getStartAt().isBefore(fiveMinFromNow)
                        && s.getStartAt().isBefore(sixMinFromNow))
                .collectList()
                .doOnNext(list -> log.info("[SCHEDULER-DEBUG] found {} candidate(s) in window", list.size()))
                .flatMapMany(Flux::fromIterable)
                .flatMap(schedule -> {
                    Query q = Query.query(Criteria.where("_id").is(schedule.getId())
                            .orOperator(Criteria.where("preplayPublished").is(false),
                                        Criteria.where("preplayPublished").exists(false)));
                    Update u = new Update()
                            .set("preplayPublished", true)
                            .set("updatedAt", Instant.now());

                    return mongo.updateFirst(q, u, Schedule.class)
                            .flatMap((UpdateResult result) -> {
                                long modified = result.getModifiedCount();
                                if (modified > 0) {
                                    // We claimed it — safe to publish
                                    log.info("[SCHEDULER] claimed schedule id={} startAt={} — publishing", schedule.getId(), schedule.getStartAt());
                                    PrePlaybackEvent event = PrePlaybackEvent.builder()
                                            .scheduleId(schedule.getId())
                                            .userId(schedule.getUserId())
                                            .channel(schedule.getChannel())
                                            .programUrl(schedule.getProgramUrl())
                                            .startAt(schedule.getStartAt())
                                            .durationMin(schedule.getDurationMin())
                                            .build();
                                    try {
                                        eventPublisher.publish(event);
                                    } catch (Exception ex) {
                                        log.error("[SCHEDULER] publish failed for schedule id={}", schedule.getId(), ex);
                                    }
                                } else {
                                    log.debug("[SCHEDULER] schedule id={} already published by another worker", schedule.getId());
                                }
                                return Mono.empty();
                            });
                })
                .doOnError(t -> log.error("[SCHEDULER] scan error", t))
                .subscribe();
    }

    public Instant getLastScan() {
        return lastScan.get();
    }

    public int getScanCount() {
        return scanCount.get();
    }

    public Mono<SchedulerNowResponse> getNowPlaying(String userId) {
        Instant now = Instant.now();
        return repository.findAllByUserId(userId)
                .collectList()
                .map(schedules -> calculateNowStatus(schedules, now));
    }

    private SchedulerNowResponse calculateNowStatus(List<Schedule> schedules, Instant now) {
        if (schedules == null || schedules.isEmpty()) {
            return new SchedulerNowResponse("NONE", null, null, null, null);
        }

        Schedule active = null;
        Schedule next = null;

        schedules.sort(Comparator.comparing(Schedule::getStartAt));

        for (Schedule s : schedules) {
            int duration = s.getDurationMin() != null ? s.getDurationMin() : 30;
            Instant end = s.getStartAt().plusSeconds(duration * 60L);

            if (!s.getStartAt().isAfter(now) && end.isAfter(now)) {
                active = s;
            } else if (s.getStartAt().isAfter(now)) {
                if (next == null || s.getStartAt().isBefore(next.getStartAt())) {
                    next = s;
                }
            }
        }

        SchedulerNowResponse.ScheduleEntry activeEntry = null;
        if (active != null) {
            long secondsPassed = Duration.between(active.getStartAt(), now).getSeconds();
            int skipMin = (int) (secondsPassed / 60);
            int duration = active.getDurationMin() != null ? active.getDurationMin() : 30;

            activeEntry = new SchedulerNowResponse.ScheduleEntry(
                    active.getId(),
                    active.getProgramUrl(),
                    active.getStartAt().atOffset(ZoneOffset.UTC),
                    duration,
                    skipMin
            );
        }

        SchedulerNowResponse.ScheduleEntry nextEntry = null;
        if (next != null) {
            int duration = next.getDurationMin() != null ? next.getDurationMin() : 30;
            nextEntry = new SchedulerNowResponse.ScheduleEntry(
                    next.getId(),
                    next.getProgramUrl(),
                    next.getStartAt().atOffset(ZoneOffset.UTC),
                    duration,
                    0
            );
        }

        if (activeEntry != null) {
            return new SchedulerNowResponse("PLAY", activeEntry, nextEntry, null, null);
        } else {
            return new SchedulerNowResponse("NONE", null, nextEntry, null, null);
        }
    }

    public Mono<ScheduleResponse> getById(String id) {
        return repository.findById(id).map(toResponse());
    }

    public Flux<ScheduleResponse> listAll() {
        return repository.findAll().map(toResponse());
    }

    public Flux<ScheduleResponse> listByChannel(String channel) {
        return repository.findAllByChannel(channel).map(toResponse());
    }

    public Flux<ScheduleResponse> listByUserId(String userId) {
        return repository.findAllByUserId(userId).map(toResponse());
    }

    public Flux<ScheduleResponse> upcoming(String userId, String channel, Integer limit) {
        int l = (limit == null || limit <= 0) ? 10 : limit;
        Instant now = Instant.now();

        Flux<Schedule> source;
        if (userId != null && !userId.isBlank()) {
            source = repository.findAllByUserIdAndStartAtGreaterThanEqual(userId, now);
        } else if (channel != null && !channel.isBlank()) {
            source = repository.findAllByChannelAndStartAtGreaterThanEqual(channel, now);
        } else {
            source = repository.findAll()
                    .filter(s -> s.getStartAt() != null && !s.getStartAt().isBefore(now));
        }

        return source
                .filter(s -> s.getStartAt() != null)
                .sort(Comparator.comparing(Schedule::getStartAt))
                .take(l)
                .map(toResponse());
    }

    public Mono<Void> delete(String id) {
        return repository.deleteById(id);
    }

    private Function<Schedule, ScheduleResponse> toResponse() {
        return s -> ScheduleResponse.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .title(s.getTitle())
                .channel(s.getChannel())
                .startAt(s.getStartAt())
                .durationMin(s.getDurationMin())
                .recurrence(s.getRecurrence() != null ? s.getRecurrence().name() : "NONE")
                .programUrl(s.getProgramUrl())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
