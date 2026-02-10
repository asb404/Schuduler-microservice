package com.schuduler.programschuduler.repository;

import com.schuduler.programschuduler.model.Schedule;

import java.time.Instant;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ScheduleRepository extends ReactiveCrudRepository<Schedule, String> {
    Flux<Schedule> findAllByUserId(String userId);
    Flux<Schedule> findAllByChannel(String channel);
    Flux<Schedule> findAllByUserIdAndStartAtGreaterThanEqual(String userId, Instant startAt);
    Flux<Schedule> findAllByChannelAndStartAtGreaterThanEqual(String channel, Instant startAt);
    Flux<Schedule> findAllByStartAtBetween(Instant start, Instant end);

}
