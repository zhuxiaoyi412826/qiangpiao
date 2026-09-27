package com.qiangpiao.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Controller;

/**
 * 根容器配置：只加载 Service / Mapper / 缓存 / 安全等基础设施，
 * Controller 与全局异常处理器由 SpringMVC 子容器加载。
 */
@Configuration
@PropertySource(value = "classpath:config.properties", encoding = "UTF-8")
@Import({
        DataSourceConfig.class,
        MybatisConfig.class,
        RedisConfig.class,
        CacheConfig.class,
        SecurityConfig.class,
        AsyncConfig.class
})
@ComponentScan(
        basePackages = "com.qiangpiao",
        excludeFilters = {
                @Filter(type = FilterType.ANNOTATION, classes = {
                        Controller.class, RestController.class,
                        ControllerAdvice.class, RestControllerAdvice.class
                }),
                @Filter(type = FilterType.REGEX, pattern = "com\\.qiangpiao\\.config\\..*"),
                @Filter(type = FilterType.REGEX, pattern = "com\\.qiangpiao\\.controller\\..*")
        }
)
@EnableTransactionManagement
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
@EnableScheduling
public class RootConfig {
}
