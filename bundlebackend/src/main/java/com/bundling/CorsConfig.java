package com.bundling;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 配置跨域资源共享（CORS）的配置类。
 */
@Configuration
public class CorsConfig {
    /**
     * 跨域请求的最大缓存时间，单位为秒（24小时）。
     */
    private static final long MAX_AGE = 24L * 60 * 60;

    /**
     * 创建并配置 CORS 过滤器 bean。
     *
     * @return 配置好的 CORS 过滤器。
     */
    @Bean
    public CorsFilter corsFilter() {
        // 创建 URL 基础的 CORS 配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 创建 CORS 配置对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 允许所有头信息
        corsConfiguration.addAllowedHeader("*");
        // 允许所有 HTTP 方法
        corsConfiguration.addAllowedMethod("*");
        // 设置最大缓存时间
        corsConfiguration.setMaxAge(MAX_AGE);
        // 注册 CORS 配置，应用于所有路径
        source.registerCorsConfiguration("/**", corsConfiguration);
        // 返回配置好的 CORS 过滤器
        return new CorsFilter(source);
    }
}



