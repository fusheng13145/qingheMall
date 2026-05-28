package com.qinghe.mall.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 交换器名称
     */
    public static final String ORDER_EXCHANGE = "order.exchange";

    /**
     * 延迟队列名称
     */
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";

    /**
     * 路由键
     */
    public static final String ORDER_ROUTING_KEY = "order.cancel";

    /**
     * 消息队列交换器
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    /**
     * 延迟队列
     * x-dead-letter-exchange: 死信交换器
     * x-dead-letter-routing-key: 死信路由键
     */
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 30分钟延迟
        args.put("x-message-ttl", 30 * 60 * 1000);
        args.put("x-dead-letter-exchange", ORDER_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_ROUTING_KEY);
        return new Queue(ORDER_DELAY_QUEUE, true, false, false, args);
    }

    /**
     * 绑定延迟队列到交换器
     */
    @Bean
    public Binding orderBinding() {
        return BindingBuilder
                .bind(orderDelayQueue())
                .to(orderExchange())
                .with(ORDER_ROUTING_KEY);
    }

    /**
     * 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
