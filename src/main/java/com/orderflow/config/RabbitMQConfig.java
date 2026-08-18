package com.orderflow.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "order.exchange";
    public static final String ROUTING_ORDER_CREATED = "order.created";
    public static final String QUEUE_ORDER_CREATED = "order.created.queue";
    public static final String DLX = "order.dlx";
    public static final String DLQ = "order.dlq";
    public static final String ROUTING_INVENTORY_LOW_STOCK = "inventory.low-stock";
    public static final String QUEUE_INVENTORY_LOW_STOCK = "inventory.low-stock.queue";
    /** 消费端最大尝试次数（含首次），耗尽后进入 DLQ。 */
    public static final int MAX_CONSUME_ATTEMPTS = 3;

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CREATED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(orderExchange()).with(ROUTING_ORDER_CREATED);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ);
    }

    @Bean
    public Queue inventoryLowStockQueue() {
        return QueueBuilder.durable(QUEUE_INVENTORY_LOW_STOCK)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Binding inventoryLowStockBinding() {
        return BindingBuilder.bind(inventoryLowStockQueue()).to(orderExchange()).with(ROUTING_INVENTORY_LOW_STOCK);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(om);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        // Outbox 发布端需要“路由失败可感知”，不能把不可达消息误记为已成功投递。
        template.setMandatory(true);
        return template;
    }

    /**
     * 业务队列的监听容器工厂。
     *
     * <p>两个容易踩的坑，都在这里显式收口：</p>
     * <ol>
     *   <li>一旦声明该 Bean，Spring Boot 自动配置（{@code spring.rabbitmq.listener.simple.*}）
     *       因 {@code @ConditionalOnMissingBean} 整体失效，ack 模式 / 重试 / requeue 策略
     *       必须在此指定，否则静默退回框架默认值；</li>
     *   <li>消费端重试要走 {@code adviceChain}。
     *       {@code setRetryTemplate()} 作用于「发送回复」而非消费失败重试，
     *       误用会导致"以为重试了 3 次、实际一次都没重试"。</li>
     * </ol>
     *
     * <p>{@link RejectAndDontRequeueRecoverer} + {@code defaultRequeueRejected=false} 是关键：
     * 默认策略会把重试耗尽的"毒消息"无限重回原队列反复失败（刷爆日志且永远消费不掉），
     * 改为 reject 后由队列上的 {@code x-dead-letter-exchange} 投递到 DLQ 兜底。</p>
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        // 重试耗尽后不重回原队列，改由死信交换机投递到 DLQ
        factory.setDefaultRequeueRejected(false);
        // 消费并发与预取，避免单条慢消息拖垮整个队列
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(10);
        // 真正生效的消费重试：最多 3 次，指数退避 2s → 4s，耗尽后 reject 不重投
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(MAX_CONSUME_ATTEMPTS)
                .backOffOptions(2000L, 2.0, 10000L)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }

    /**
     * 死信队列专用工厂：不重试、失败不重投。
     * <p>死信是最后一道防线，若它也重试/重投，一条无法归档的消息会在 DLQ 里死循环。</p>
     */
    @Bean
    public SimpleRabbitListenerContainerFactory dlqListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        factory.setConcurrentConsumers(1);
        factory.setPrefetchCount(5);
        return factory;
    }
}
