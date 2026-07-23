package com.ledger.config;

import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * Java 21 Virtual Threads wiring.
 *
 * `spring.threads.virtual.enabled=true` (application.yml) already switches
 * Tomcat's request-handling executor to virtual threads. This config adds
 * a matching virtual-thread-backed executor for any @Async work (e.g.
 * fire-and-forget notification/audit events triggered after a transfer),
 * so that background work benefits from the same cheap-blocking model as
 * the request path instead of falling back to Spring's default bounded
 * platform-thread pool.
 */
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Ensures Tomcat itself accepts connections using virtual threads even
     * if the `spring.threads.virtual.enabled` property is ever removed or
     * overridden in a specific profile — belt and suspenders for a core
     * banking engine.
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
