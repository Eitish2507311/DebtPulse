package com.debtpulse.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud API Gateway.
 *
 * <p>Single entry point (port 9090) for every DebtPulse client. Responsibilities:</p>
 * <ul>
 *   <li>Route requests to the correct microservice via Eureka (lb://) load balancing.</li>
 *   <li>Validate the JWT on every protected request ({@link JwtAuthenticationGlobalFilter}).</li>
 *   <li>Propagate the authenticated identity (userId / role / branchId) downstream as headers.</li>
 *   <li>Aggregate every service's Swagger UI at http://localhost:9090/swagger-ui.html.</li>
 * </ul>
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
