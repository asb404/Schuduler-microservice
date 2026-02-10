package com.schuduler.programschuduler.messaging;

import com.schuduler.programschuduler.config.RabbitMQConfig;
import com.schuduler.programschuduler.dto.PrePlaybackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrePlaybackEventPublisher {

    private final RabbitTemplate template;

    public void publish(PrePlaybackEvent event) {
        log.info("Publishing PrePlaybackEvent scheduleId={} startAt={} to exchange={} routingKey={}",
            event.getScheduleId(), event.getStartAt(), RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY);
        template.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
    }
}
