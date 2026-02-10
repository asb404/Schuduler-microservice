package com.schuduler.programschuduler.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "scheduler.events";
    public static final String QUEUE = "playback.preplay.queue";
    public static final String ROUTING_KEY = "schedule.preplay";

    // Connection Factory
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost("host.docker.internal");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        return factory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         RabbitAdmin admin,
                                         MessageConverter converter) {
        admin.initialize();
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public TopicExchange schedulerExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue playbackQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding binding(Queue playbackQueue, TopicExchange schedulerExchange) {
        return BindingBuilder.bind(playbackQueue)
                .to(schedulerExchange)
                .with(ROUTING_KEY);
    }
}
