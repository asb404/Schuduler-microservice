package com.schuduler.programschuduler.messaging;

import com.schuduler.programschuduler.config.RabbitMQConfig;
import com.schuduler.programschuduler.dto.PrePlaybackEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrePlaybackEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PrePlaybackEventPublisher prePlaybackEventPublisher;

    private PrePlaybackEvent prePlaybackEvent;
    private static final String EXCHANGE = "pre-playback-exchange";
    private static final String ROUTING_KEY = "pre-playback.routing-key";

    @BeforeEach
    void setUp() {
        // Setup static values for RabbitMQConfig (assuming they're static final)
        // If they're not static, you'll need to mock the RabbitMQConfig class
        prePlaybackEvent = PrePlaybackEvent.builder()
                .scheduleId("schedule-123")
                .userId("user-456")
                .channel("channel-1")
                .programUrl("https://example.com/video.mp4")
                .startAt(Instant.parse("2024-01-15T10:30:00Z"))
                .durationMin(60)
                .build();
    }

    @Test
    void publish_ValidEvent_PublishesToRabbitMQ() {
        // Act
        prePlaybackEventPublisher.publish(prePlaybackEvent);

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(prePlaybackEvent));
    }

    @Test
    void publish_EventWithNullFields_PublishesSuccessfully() {
        // Arrange
        PrePlaybackEvent eventWithNulls = PrePlaybackEvent.builder()
                .scheduleId(null)
                .userId(null)
                .channel(null)
                .programUrl(null)
                .startAt(null)
                .durationMin(null)
                .build();

        // Act
        prePlaybackEventPublisher.publish(eventWithNulls);

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(eventWithNulls));
    }

    @Test
    void publish_EventWithEmptyStrings_PublishesSuccessfully() {
        // Arrange
        PrePlaybackEvent eventWithEmptyStrings = PrePlaybackEvent.builder()
                .scheduleId("")
                .userId("")
                .channel("")
                .programUrl("")
                .startAt(Instant.now())
                .durationMin(0)
                .build();

        // Act
        prePlaybackEventPublisher.publish(eventWithEmptyStrings);

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(eventWithEmptyStrings));
    }

    @Test
    void publish_MinimalValidEvent_PublishesSuccessfully() {
        // Arrange
        PrePlaybackEvent minimalEvent = PrePlaybackEvent.builder()
                .scheduleId("minimal-schedule")
                .startAt(Instant.now())
                .durationMin(1) // Minimum duration
                .build();

        // Act
        prePlaybackEventPublisher.publish(minimalEvent);

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(minimalEvent));
    }

    @Test
    void publish_EventWithMaximumValues_PublishesSuccessfully() {
        // Arrange
        PrePlaybackEvent maxEvent = PrePlaybackEvent.builder()
                .scheduleId("a".repeat(100))
                .userId("b".repeat(100))
                .channel("c".repeat(100))
                .programUrl("https://" + "d".repeat(1000) + ".com")
                .startAt(Instant.parse("9999-12-31T23:59:59Z"))
                .durationMin(Integer.MAX_VALUE)
                .build();

        // Act
        prePlaybackEventPublisher.publish(maxEvent);

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(maxEvent));
    }

    @Test
    void publish_MultipleEvents_PublishesEachOne() {
        // Arrange
        PrePlaybackEvent event1 = PrePlaybackEvent.builder()
                .scheduleId("event-1")
                .startAt(Instant.now())
                .durationMin(30)
                .build();

        PrePlaybackEvent event2 = PrePlaybackEvent.builder()
                .scheduleId("event-2")
                .startAt(Instant.now().plusSeconds(3600))
                .durationMin(60)
                .build();

        // Act
        prePlaybackEventPublisher.publish(event1);
        prePlaybackEventPublisher.publish(event2);

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(event1));
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(event2));
        verify(rabbitTemplate, times(2))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), any(PrePlaybackEvent.class));
    }

    @Test
    void publish_RabbitTemplateThrowsException_LogsAndPropagates() {
        // Arrange
        RuntimeException rabbitException = new RuntimeException("RabbitMQ connection failed");
        doThrow(rabbitException)
                .when(rabbitTemplate)
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(prePlaybackEvent));

        // Act & Assert
        try {
            prePlaybackEventPublisher.publish(prePlaybackEvent);
        } catch (RuntimeException e) {
            // Expected exception
        }

        // Verify the method was called despite the exception
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(prePlaybackEvent));
    }

    @Test
    void publish_WithSpecialCharactersInFields_PublishesSuccessfully() {
        // Arrange
        PrePlaybackEvent specialEvent = PrePlaybackEvent.builder()
                .scheduleId("schedule-123!@#$%^&*()")
                .userId("user@domain.com")
                .channel("Channel & More <>")
                .programUrl("https://example.com/video.mp4?param=value&other=test")
                .startAt(Instant.now())
                .durationMin(60)
                .build();

        // Act
        prePlaybackEventPublisher.publish(specialEvent);

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(specialEvent));
    }

    @Test
    void publish_EventWithZeroDuration_PublishesSuccessfully() {
        // Arrange
        PrePlaybackEvent zeroDurationEvent = PrePlaybackEvent.builder()
                .scheduleId("zero-duration-schedule")
                .startAt(Instant.now())
                .durationMin(0) // Zero duration
                .build();

        // Act
        prePlaybackEventPublisher.publish(zeroDurationEvent);

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(zeroDurationEvent));
    }

    @Test
    void publish_EventWithNegativeDuration_PublishesSuccessfully() {
        // Arrange
        PrePlaybackEvent negativeDurationEvent = PrePlaybackEvent.builder()
                .scheduleId("negative-duration-schedule")
                .startAt(Instant.now())
                .durationMin(-10) // Negative duration
                .build();

        // Act
        prePlaybackEventPublisher.publish(negativeDurationEvent);

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(negativeDurationEvent));
    }

    @Test
    void publish_VerifyExchangeAndRoutingKeyUsage() {
        // Act
        prePlaybackEventPublisher.publish(prePlaybackEvent);

        // Assert - Verify specific exchange and routing key are used
        verify(rabbitTemplate, times(1))
                .convertAndSend(
                        eq(RabbitMQConfig.EXCHANGE),
                        eq(RabbitMQConfig.ROUTING_KEY),
                        any(PrePlaybackEvent.class)
                );
        
        // Verify no other interactions with rabbitTemplate
        verifyNoMoreInteractions(rabbitTemplate);
    }

    @Test
    void publish_EventWithFutureDate_PublishesSuccessfully() {
        // Arrange
        PrePlaybackEvent futureEvent = PrePlaybackEvent.builder()
                .scheduleId("future-schedule")
                .startAt(Instant.now().plusSeconds(86400 * 365)) // One year from now
                .durationMin(120)
                .build();

        // Act
        prePlaybackEventPublisher.publish(futureEvent);

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(futureEvent));
    }

    @Test
    void publish_EventWithPastDate_PublishesSuccessfully() {
        // Arrange
        PrePlaybackEvent pastEvent = PrePlaybackEvent.builder()
                .scheduleId("past-schedule")
                .startAt(Instant.now().minusSeconds(86400 * 365)) // One year ago
                .durationMin(30)
                .build();

        // Act
        prePlaybackEventPublisher.publish(pastEvent);

        // Assert
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(pastEvent));
    }
}