package com.orderflow.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 依赖健康检查。无需鉴权，供部署探针与本地联调使用。
 * <p>逐个探活 MySQL / Redis / RabbitMQ：全部正常返回 200，任一异常返回 503 并列出失败原因，
 * 便于快速区分"应用挂了"和"某个中间件挂了"。</p>
 */
@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    public HealthController(DataSource dataSource,
                            StringRedisTemplate redisTemplate,
                            RabbitTemplate rabbitTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> components = new LinkedHashMap<>();
        boolean allUp = true;

        allUp &= check(components, "mysql", () -> {
            try (Connection c = dataSource.getConnection()) {
                c.createStatement().execute("SELECT 1");
            }
        });
        allUp &= check(components, "redis", () -> redisTemplate.hasKey("health:probe"));
        allUp &= check(components, "rabbitmq",
                () -> rabbitTemplate.execute(channel -> channel.queueDeclarePassive(
                        com.orderflow.config.RabbitMQConfig.QUEUE_ORDER_CREATED)));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", allUp ? "UP" : "DOWN");
        body.put("components", components);

        return allUp
                ? ResponseEntity.ok(ApiResponse.success(body))
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.success(body));
    }

    private boolean check(Map<String, Object> components, String name, Probe probe) {
        try {
            probe.run();
            components.put(name, Map.of("status", "UP"));
            return true;
        } catch (Exception ex) {
            log.warn("健康检查失败: component={}, err={}", name, ex.getMessage());
            components.put(name, Map.of("status", "DOWN", "error", String.valueOf(ex.getMessage())));
            return false;
        }
    }

    @FunctionalInterface
    private interface Probe {
        void run() throws Exception;
    }
}
