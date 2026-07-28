package com.debtpulse.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server.
 *
 * <p>Serves centralized configuration for every DebtPulse microservice from the
 * local {@code config-repo} folder (native profile). Start this FIRST, before any
 * other service, so that services can bootstrap their configuration from it.</p>
 */
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
