package com.schuduler.programschuduler.service;

import com.schuduler.programschuduler.dto.*;
import com.schuduler.programschuduler.messaging.PrePlaybackEventPublisher;
import com.schuduler.programschuduler.model.Schedule;
import com.schuduler.programschuduler.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository repository;

    @Mock
    private PrePlaybackEventPublisher eventPublisher;

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @InjectMocks
    private ScheduleService scheduleService;

    private ScheduleRequest scheduleRequest;
    private Schedule schedule;
    private ScheduleResponse scheduleResponse;
    private Instant testInstant;
    private String scheduleId = "test-schedule-id";

    @BeforeEach
    void setUp() {
        testInstant = Instant.parse("2024-01-15T10:30:00Z");
        
        scheduleRequest = ScheduleRequest.builder()
                .userId("test-user")
                .title("Test Program")
                .channel("Test Channel")
                .date("2024-01-15")
                .time("10:30")
                .durationMin(60)
                .recurrence("NONE")
                .programUrl("http://example.com/video.mp4")
                .notes("Test notes")
                .build();

        schedule = Schedule.builder()
                .id(scheduleId)
                .userId("test-user")
                .title("Test Program")
                .channel("Test Channel")
                .startAt(testInstant)
                .durationMin(60)
                .recurrence(Schedule.Recurrence.NONE)
                .programUrl("http://example.com/video.mp4")
                .notes("Test notes")
                .preplayPublished(false)
                .createdAt(testInstant)
                .updatedAt(testInstant)
                .build();

        scheduleResponse = ScheduleResponse.builder()
                .id(scheduleId)
                .userId("test-user")
                .title("Test Program")
                .channel("Test Channel")
                .startAt(testInstant)
                .durationMin(60)
                .recurrence("NONE")
                .programUrl("http://example.com/video.mp4")
                .notes("Test notes")
                .createdAt(testInstant)
                .updatedAt(testInstant)
                .build();
    }

    @Test
    void createSchedule_ValidRequest_ReturnsScheduleResponse() {
        // Arrange
        when(repository.save(any(Schedule.class))).thenReturn(Mono.just(schedule));

        // Act & Assert
        StepVerifier.create(scheduleService.createSchedule(scheduleRequest))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(repository, times(1)).save(any(Schedule.class));
    }

    @Test
    void createSchedule_RepositoryThrowsException_PropagatesError() {
        // Arrange
        RuntimeException repoException = new RuntimeException("Database error");
        when(repository.save(any(Schedule.class))).thenReturn(Mono.error(repoException));

        // Act & Assert
        StepVerifier.create(scheduleService.createSchedule(scheduleRequest))
                .expectError(RuntimeException.class)
                .verify();

        verify(repository, times(1)).save(any(Schedule.class));
    }

    @Test
    void getById_ValidId_ReturnsScheduleResponse() {
        // Arrange
        when(repository.findById(scheduleId)).thenReturn(Mono.just(schedule));

        // Act & Assert
        StepVerifier.create(scheduleService.getById(scheduleId))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(repository, times(1)).findById(scheduleId);
    }

    @Test
    void getById_NonExistentId_ReturnsEmpty() {
        // Arrange
        when(repository.findById("non-existent")).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(scheduleService.getById("non-existent"))
                .verifyComplete();

        verify(repository, times(1)).findById("non-existent");
    }

    @Test
    void listAll_ReturnsAllSchedules() {
        // Arrange
        Schedule schedule2 = Schedule.builder()
                .id("schedule-2")
                .userId("user2")
                .title("Program 2")
                .channel("Channel 2")
                .startAt(testInstant.plusSeconds(3600))
                .durationMin(30)
                .recurrence(Schedule.Recurrence.DAILY)
                .build();

        when(repository.findAll()).thenReturn(Flux.just(schedule, schedule2));

        // Act & Assert
        StepVerifier.create(scheduleService.listAll())
                .expectNextCount(2)
                .verifyComplete();

        verify(repository, times(1)).findAll();
    }

    @Test
    void listByChannel_ValidChannel_ReturnsChannelSchedules() {
        // Arrange
        String channel = "Test Channel";
        when(repository.findAllByChannel(channel)).thenReturn(Flux.just(schedule));

        // Act & Assert
        StepVerifier.create(scheduleService.listByChannel(channel))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(repository, times(1)).findAllByChannel(channel);
    }

    @Test
    void listByUserId_ValidUserId_ReturnsUserSchedules() {
        // Arrange
        String userId = "test-user";
        when(repository.findAllByUserId(userId)).thenReturn(Flux.just(schedule));

        // Act & Assert
        StepVerifier.create(scheduleService.listByUserId(userId))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(repository, times(1)).findAllByUserId(userId);
    }

    @Test
    void delete_ValidId_DeletesSuccessfully() {
        // Arrange
        when(repository.deleteById(scheduleId)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(scheduleService.delete(scheduleId))
                .verifyComplete();

        verify(repository, times(1)).deleteById(scheduleId);
    }

    @Test
    void getNowPlaying_NoSchedules_ReturnsNoneStatus() {
        // Arrange
        String userId = "test-user";
        when(repository.findAllByUserId(userId)).thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(scheduleService.getNowPlaying(userId))
                .assertNext(response -> {
                    assertEquals("NONE", response.status());
                    assertNull(response.entry());
                    assertNull(response.nextEntry());
                })
                .verifyComplete();
    }

    @Test
    void getNowPlaying_ActiveSchedule_ReturnsPlayStatus() {
        // Arrange
        String userId = "test-user";
        Instant now = Instant.now();
        Instant startTime = now.minusSeconds(300); 
        Instant endTime = startTime.plusSeconds(3600);

        Schedule activeSchedule = Schedule.builder()
                .id("active-schedule")
                .userId(userId)
                .title("Active Program")
                .channel("Channel 1")
                .startAt(startTime)
                .durationMin(60)
                .programUrl("http://active.com/video.mp4")
                .build();

        when(repository.findAllByUserId(userId)).thenReturn(Flux.just(activeSchedule));

        // Act & Assert
        StepVerifier.create(scheduleService.getNowPlaying(userId))
                .assertNext(response -> {
                    assertEquals("PLAY", response.status());
                    assertNotNull(response.entry());
                    assertEquals("active-schedule", response.entry().id());
                    assertTrue(response.entry().skipStartMin() > 0); 
                })
                .verifyComplete();
    }

    @Test
    void getNowPlaying_FutureSchedule_ReturnsNoneWithNextEntry() {
        // Arrange
        String userId = "test-user";
        Instant now = Instant.now();
        Instant futureTime = now.plusSeconds(3600); // 1 hour from now

        Schedule futureSchedule = Schedule.builder()
                .id("future-schedule")
                .userId(userId)
                .title("Future Program")
                .channel("Channel 1")
                .startAt(futureTime)
                .durationMin(30)
                .programUrl("http://future.com/video.mp4")
                .build();

        when(repository.findAllByUserId(userId)).thenReturn(Flux.just(futureSchedule));

        // Act & Assert
        StepVerifier.create(scheduleService.getNowPlaying(userId))
                .assertNext(response -> {
                    assertEquals("NONE", response.status());
                    assertNull(response.entry());
                    assertNotNull(response.nextEntry());
                    assertEquals("future-schedule", response.nextEntry().id());
                })
                .verifyComplete();
    }

    @Test
    void getNowPlaying_MultipleSchedules_ReturnsCorrectActiveAndNext() {
        // Arrange
        String userId = "test-user";
        Instant now = Instant.now();
        
        // Past schedule (already ended)
        Schedule pastSchedule = Schedule.builder()
                .id("past-schedule")
                .userId(userId)
                .title("Past Program")
                .startAt(now.minusSeconds(7200))
                .durationMin(30)
                .build();
        
        // Active schedule
        Schedule activeSchedule = Schedule.builder()
                .id("active-schedule")
                .userId(userId)
                .title("Active Program")
                .startAt(now.minusSeconds(300))
                .durationMin(60)
                .programUrl("http://active.com/video.mp4")
                .build();
        
        // Future schedule
        Schedule futureSchedule = Schedule.builder()
                .id("future-schedule")
                .userId(userId)
                .title("Future Program")
                .startAt(now.plusSeconds(3600))
                .durationMin(30)
                .build();

        when(repository.findAllByUserId(userId)).thenReturn(Flux.just(pastSchedule, activeSchedule, futureSchedule));

        // Act & Assert
        StepVerifier.create(scheduleService.getNowPlaying(userId))
                .assertNext(response -> {
                    assertEquals("PLAY", response.status());
                    assertEquals("active-schedule", response.entry().id());
                    assertEquals("future-schedule", response.nextEntry().id());
                })
                .verifyComplete();
    }

   

    @Test
    void upcoming_WithLimit_ReturnsLimitedResults() {
        // Arrange
        Instant now = Instant.now();
        Schedule schedule1 = Schedule.builder()
                .id("schedule-1")
                .userId("user1")
                .title("Program 1")
                .channel("Channel 1")
                .startAt(now.plusSeconds(600))
                .build();
        
        Schedule schedule2 = Schedule.builder()
                .id("schedule-2")
                .userId("user1")
                .title("Program 2")
                .channel("Channel 1")
                .startAt(now.plusSeconds(1200))
                .build();

        when(repository.findAllByUserIdAndStartAtGreaterThanEqual(anyString(), any(Instant.class)))
                .thenReturn(Flux.just(schedule1, schedule2));

        // Act & Assert
        StepVerifier.create(scheduleService.upcoming("user1", null, 1))
                .expectNextCount(1) 
                .verifyComplete();
    }

    @Test
    void upcoming_WithNullLimit_UsesDefaultLimit() {
        // Arrange
        Instant now = Instant.now();
        when(repository.findAll()).thenReturn(Flux.empty());

        // Act
        StepVerifier.create(scheduleService.upcoming(null, null, null))
                .verifyComplete();

        verify(repository, times(1)).findAll();
    }

    @Test
    void upcoming_WithNegativeLimit_UsesDefaultLimit() {
        // Arrange
        Instant now = Instant.now();
        when(repository.findAll()).thenReturn(Flux.empty());

        // Act
        StepVerifier.create(scheduleService.upcoming(null, null, -5))
                .verifyComplete();

        verify(repository, times(1)).findAll();
    }

    @Test
    void getLastScan_ReturnsLastScanTime() {
        // Arrange
        Instant expected = Instant.now();
        AtomicReference<Instant> lastScan = (AtomicReference<Instant>) 
                ReflectionTestUtils.getField(scheduleService, "lastScan");
        lastScan.set(expected);

        // Act
        Instant result = scheduleService.getLastScan();

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void getScanCount_ReturnsScanCount() {
        // Arrange
        int expected = 5;
        AtomicInteger scanCount = (AtomicInteger) 
                ReflectionTestUtils.getField(scheduleService, "scanCount");
        scanCount.set(expected);

        // Act
        int result = scheduleService.getScanCount();

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void parseStartAt_ValidDateAndTime_ReturnsCorrectInstant() {
        // Arrange
        String date = "2024-01-15";
        String time = "14:30";

        // Act
        Instant result = (Instant) ReflectionTestUtils.invokeMethod(
                scheduleService, "parseStartAt", date, time);

        // Assert
        assertNotNull(result);
    }


}