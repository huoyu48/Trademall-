package com.orderflow.config;

import com.orderflow.platform.PlatformTenantInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PlatformTenantInterceptor platformTenantInterceptor;

    public WebConfig(PlatformTenantInterceptor platformTenantInterceptor) {
        this.platformTenantInterceptor = platformTenantInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注意：context-path=/api 已被剥离，这里匹配的是剥离后的路径
        registry.addInterceptor(platformTenantInterceptor).addPathPatterns("/platform/**");
    }
}
