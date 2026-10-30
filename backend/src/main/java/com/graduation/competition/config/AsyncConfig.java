package com.graduation.competition.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 文档解析及 PPT/视频生成共用的异步线程池。
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "applicationTaskExecutor")
    public Executor applicationTaskExecutor(
            @Value("${app.async.core-pool-size:4}") int corePoolSize,
            @Value("${app.async.max-pool-size:8}") int maxPoolSize,
            @Value("${app.async.queue-capacity:100}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("competition-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

