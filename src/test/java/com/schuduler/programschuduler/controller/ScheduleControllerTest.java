package com.schuduler.programschuduler.controller;

import com.schuduler.programschuduler.dto.*;
import com.schuduler.programschuduler.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private ScheduleController scheduleController;

    private ScheduleRequest validScheduleRequest;
    private ScheduleResponse scheduleResponse;
    private SchedulerNowResponse schedulerNowResponse;
    private SchedulerNowResponse.ScheduleEntry scheduleEntry;

    @BeforeEach
    void setUp() {
        // Setup valid request
        validScheduleRequest = ScheduleRequest.builder()
                .userId("user123")
                .title("Test Program")
                .channel("Channel 1")
                .date("2024-01-01")
                .time("14:30")
                .durationMin(60)
                .recurrence("NONE")
                .programUrl("http://example.com/video.mp4")
                .notes("Test notes")
                .build();

        // Setup response
        scheduleResponse = ScheduleResponse.builder()
                .id("schedule123")
                .userId("user123")
                .title("Test Program")
                .channel("Channel 1")
                .startAt(Instant.parse("2024-01-01T14:30:00Z"))
                .durationMin(60)
                .recurrence("NONE")
                .programUrl("http://example.com/video.mp4")
                .notes("Test notes")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Setup schedule entry for now playing
        scheduleEntry = new SchedulerNowResponse.ScheduleEntry(
                "schedule123",
                "http://example.com/video.mp4",
                OffsetDateTime.now(),
                60,
                0
        );

        // Setup now playing response
        schedulerNowResponse = new SchedulerNowResponse(
                "PLAY",
                scheduleEntry,
                null,
                0,
                List.of("PLAY", "SKIP")
        );
    }

    @Test
    void create_ValidRequest_ReturnsCreatedSchedule() {
        // Arrange
        when(scheduleService.createSchedule(any(ScheduleRequest.class)))
                .thenReturn(Mono.just(scheduleResponse));

        // Act & Assert
        StepVerifier.create(scheduleController.create(validScheduleRequest))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(scheduleService, times(1)).createSchedule(validScheduleRequest);
    }

    @Test
    void create_InvalidRequest_PropagatesError() {
        // Arrange
        ScheduleRequest invalidRequest = ScheduleRequest.builder()
                .userId("")  // Invalid: blank userId
                .title("Test")
                .channel("Channel 1")
                .date("invalid-date")  // Invalid date format
                .time("25:61")  // Invalid time
                .durationMin(0)  // Invalid: less than 1
                .recurrence("INVALID")  // Invalid recurrence
                .build();

        when(scheduleService.createSchedule(any(ScheduleRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Validation failed")));

        // Act & Assert
        StepVerifier.create(scheduleController.create(invalidRequest))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void getNow_ValidUserId_ReturnsNowPlaying() {
        // Arrange
        String userId = "user123";
        when(scheduleService.getNowPlaying(userId))
                .thenReturn(Mono.just(schedulerNowResponse));

        // Act & Assert
        StepVerifier.create(scheduleController.getNow(userId))
                .expectNext(schedulerNowResponse)
                .verifyComplete();

        verify(scheduleService, times(1)).getNowPlaying(userId);
    }

    @Test
    void getNow_ServiceReturnsEmpty_ReturnsEmpty() {
        // Arrange
        String userId = "user123";
        when(scheduleService.getNowPlaying(userId))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(scheduleController.getNow(userId))
                .verifyComplete();

        verify(scheduleService, times(1)).getNowPlaying(userId);
    }

    @Test
    void list_NoParameters_ReturnsAllSchedules() {
        // Arrange
        ScheduleResponse response2 = ScheduleResponse.builder()
                .id("schedule456")
                .userId("user456")
                .title("Another Program")
                .channel("Channel 2")
                .startAt(Instant.now())
                .durationMin(30)
                .recurrence("DAILY")
                .build();

        when(scheduleService.listAll())
                .thenReturn(Flux.just(scheduleResponse, response2));

        // Act & Assert
        StepVerifier.create(scheduleController.list(null, null))
                .expectNext(scheduleResponse)
                .expectNext(response2)
                .verifyComplete();

        verify(scheduleService, times(1)).listAll();
        verify(scheduleService, never()).listByUserId(anyString());
        verify(scheduleService, never()).listByChannel(anyString());
    }

    @Test
    void list_WithUserId_ReturnsUserSchedules() {
        // Arrange
        String userId = "user123";
        when(scheduleService.listByUserId(userId))
                .thenReturn(Flux.just(scheduleResponse));

        // Act & Assert
        StepVerifier.create(scheduleController.list(userId, null))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(scheduleService, times(1)).listByUserId(userId);
        verify(scheduleService, never()).listAll();
        verify(scheduleService, never()).listByChannel(anyString());
    }

    @Test
    void list_WithChannel_ReturnsChannelSchedules() {
        // Arrange
        String channel = "Channel 1";
        when(scheduleService.listByChannel(channel))
                .thenReturn(Flux.just(scheduleResponse));

        // Act & Assert
        StepVerifier.create(scheduleController.list(null, channel))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(scheduleService, times(1)).listByChannel(channel);
        verify(scheduleService, never()).listAll();
        verify(scheduleService, never()).listByUserId(anyString());
    }

    @Test
    void list_UserIdAndChannelProvided_PrioritizesUserId() {
        // Arrange
        String userId = "user123";
        String channel = "Channel 1";
        when(scheduleService.listByUserId(userId))
                .thenReturn(Flux.just(scheduleResponse));

        // Act & Assert
        StepVerifier.create(scheduleController.list(userId, channel))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(scheduleService, times(1)).listByUserId(userId);
        verify(scheduleService, never()).listByChannel(anyString());
        verify(scheduleService, never()).listAll();
    }

    @Test
    void list_BlankUserId_FallsBackToAll() {
        // Arrange
        when(scheduleService.listAll())
                .thenReturn(Flux.just(scheduleResponse));

        // Act & Assert
        StepVerifier.create(scheduleController.list(" ", " "))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(scheduleService, times(1)).listAll();
        verify(scheduleService, never()).listByUserId(anyString());
        verify(scheduleService, never()).listByChannel(anyString());
    }

    @Test
    void upcoming_WithAllParameters_ReturnsUpcomingSchedules() {
        // Arrange
        String userId = "user123";
        String channel = "Channel 1";
        Integer limit = 10;
        
        when(scheduleService.upcoming(userId, channel, limit))
                .thenReturn(Flux.just(scheduleResponse));

        // Act & Assert
        StepVerifier.create(scheduleController.upcoming(userId, channel, limit))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(scheduleService, times(1)).upcoming(userId, channel, limit);
    }

    @Test
    void upcoming_WithNullParameters_HandlesNulls() {
        // Arrange
        when(scheduleService.upcoming(null, null, null))
                .thenReturn(Flux.just(scheduleResponse));

        // Act & Assert
        StepVerifier.create(scheduleController.upcoming(null, null, null))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(scheduleService, times(1)).upcoming(null, null, null);
    }

    @Test
    void getById_ValidId_ReturnsSchedule() {
        // Arrange
        String scheduleId = "schedule123";
        when(scheduleService.getById(scheduleId))
                .thenReturn(Mono.just(scheduleResponse));

        // Act & Assert
        StepVerifier.create(scheduleController.get(scheduleId))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(scheduleService, times(1)).getById(scheduleId);
    }

    @Test
    void getById_NonExistentId_ReturnsEmpty() {
        // Arrange
        String scheduleId = "non-existent";
        when(scheduleService.getById(scheduleId))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(scheduleController.get(scheduleId))
                .verifyComplete();

        verify(scheduleService, times(1)).getById(scheduleId);
    }

    @Test
    void delete_ValidId_ReturnsSuccess() {
        // Arrange
        String scheduleId = "schedule123";
        when(scheduleService.delete(scheduleId))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(scheduleController.delete(scheduleId))
                .verifyComplete();

        verify(scheduleService, times(1)).delete(scheduleId);
    }

    @Test
    void delete_NonExistentId_ReturnsEmpty() {
        // Arrange
        String scheduleId = "non-existent";
        when(scheduleService.delete(scheduleId))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(scheduleController.delete(scheduleId))
                .verifyComplete();

        verify(scheduleService, times(1)).delete(scheduleId);
    }

    @Test
    void create_ServiceThrowsException_PropagatesException() {
        // Arrange
        RuntimeException serviceException = new RuntimeException("Service error");
        when(scheduleService.createSchedule(any(ScheduleRequest.class)))
                .thenReturn(Mono.error(serviceException));

        // Act & Assert
        StepVerifier.create(scheduleController.create(validScheduleRequest))
                .expectError(RuntimeException.class)
                .verify();

        verify(scheduleService, times(1)).createSchedule(validScheduleRequest);
    }

    @Test
    void get_ServiceThrowsException_PropagatesException() {
        // Arrange
        String scheduleId = "schedule123";
        RuntimeException serviceException = new RuntimeException("Service error");
        when(scheduleService.getById(scheduleId))
                .thenReturn(Mono.error(serviceException));

        // Act & Assert
        StepVerifier.create(scheduleController.get(scheduleId))
                .expectError(RuntimeException.class)
                .verify();

        verify(scheduleService, times(1)).getById(scheduleId);
    }

    @Test
    void listByUserId_ServiceReturnsEmpty_ReturnsEmptyFlux() {
        // Arrange
        String userId = "user123";
        when(scheduleService.listByUserId(userId))
                .thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(scheduleController.list(userId, null))
                .verifyComplete();

        verify(scheduleService, times(1)).listByUserId(userId);
    }

    @Test
    void upcoming_ServiceReturnsMultipleItems_ReturnsAll() {
        // Arrange
        ScheduleResponse response2 = ScheduleResponse.builder()
                .id("schedule456")
                .userId("user123")
                .title("Another Program")
                .channel("Channel 1")
                .startAt(Instant.now().plusSeconds(3600))
                .durationMin(30)
                .recurrence("WEEKLY")
                .build();

        when(scheduleService.upcoming(anyString(), anyString(), anyInt()))
                .thenReturn(Flux.just(scheduleResponse, response2));

        // Act & Assert
        StepVerifier.create(scheduleController.upcoming("user123", "Channel 1", 10))
                .expectNextCount(2)
                .verifyComplete();

        verify(scheduleService, times(1)).upcoming("user123", "Channel 1", 10);
    }

    // Test for edge cases
    @Test
    void list_EmptyStringUserId_HandlesAsNull() {
        // Arrange
        when(scheduleService.listAll())
                .thenReturn(Flux.just(scheduleResponse));

        // Act & Assert
        StepVerifier.create(scheduleController.list("", null))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(scheduleService, times(1)).listAll();
    }

    @Test
    void upcoming_WithNegativeLimit_HandlesNegative() {
        // Arrange
        Integer negativeLimit = -5;
        when(scheduleService.upcoming(null, null, negativeLimit))
                .thenReturn(Flux.just(scheduleResponse));

        // Act & Assert
        StepVerifier.create(scheduleController.upcoming(null, null, negativeLimit))
                .expectNext(scheduleResponse)
                .verifyComplete();

        verify(scheduleService, times(1)).upcoming(null, null, negativeLimit);
    }
}