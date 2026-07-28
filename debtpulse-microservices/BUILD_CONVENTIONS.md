# DebtPulse Microservices — Build Conventions (MANDATORY for every service)

Follow these EXACTLY. Use the already-built **auth-service** as the reference template
(read its files under `auth-service/src/main/java/com/debtpulse/auth/...` and its `pom.xml`).
DO NOT run Maven — the orchestrator compiles centrally.

## Module / Maven
- Directory: `debtpulse-microservices/<service>` (e.g. `account-service`).
- `pom.xml`: copy `auth-service/pom.xml`, change `<artifactId>`, `<name>`, `<description>`.
  - REMOVE the three `jjwt` dependencies (only auth-service issues tokens).
  - ADD Resilience4j circuit breaker:
    ```xml
    <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId></dependency>
    ```
  - Keep: common-lib, starter-web, data-jpa, security, validation, actuator, aop,
    eureka-client, config, openfeign, mysql, flyway-mysql, springdoc-webmvc-ui, mapstruct, lombok, test, security-test.

## Base package: `com.debtpulse.<svc>` (svc = account | contact | field | settlement | legal | notification | analytics)

## Main application class
```java
@SpringBootApplication(scanBasePackages = "com.debtpulse")
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing   // only if any entity uses @CreatedDate/@LastModifiedDate
public class XxxServiceApplication { public static void main(String[] a){ SpringApplication.run(XxxServiceApplication.class,a);} }
```

## `src/main/resources/application.yml` (copy auth-service, change name only)
```yaml
spring:
  application:
    name: <service-name>
  config:
    import: "optional:configserver:http://localhost:8888"
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

## Required packages/layers (each service MUST have all of these)
- `entity` — JPA entities. Store cross-service references as PLAIN ID STRINGS (e.g. `accountId`,
  `agentId`, `officerId`) — NO `@ManyToOne` to entities owned by another service. Relations WITHIN
  the same service may use `@ManyToOne`.
  - **Primary keys**: `String` `@Id` annotated with `@com.debtpulse.common.id.BusinessId(prefix = "XXX")`
    (NOT `@GeneratedValue`). This yields sortable, human-readable ids `PREFIX-YEAR-NNNNNN`
    (e.g. `ACC-2026-000001`). Pick a unique 3-letter uppercase prefix per entity. Requires the
    `id_sequence` table — add `V2__id_sequence.sql` to the service's Flyway migrations.
- Enums are SHARED, not per-service: import them from `com.debtpulse.common.enums.*` in common-lib
  (see domain spec). This includes the single shared `Role` enum (the 9 roles). Do NOT recreate a
  per-service `common` enum package.
- `exception` — each service owns its own `com.debtpulse.<svc>.exception` package containing
  `ResourceNotFoundException`, `BusinessRuleException`, `UnauthorizedActionException`, an
  `ErrorResponse` envelope record, and a `@RestControllerAdvice GlobalExceptionHandler`. This is
  deliberately NOT shared via common-lib so each service can evolve its error semantics independently.
- `dto/request` (Bean Validation annotations: `@NotNull/@NotBlank/@Positive/@Email/@Size`) and `dto/response`.
- `repository` — Prefer `PagingAndSortingRepository<T,ID> + CrudRepository<T,ID>` with derived
  finders. Use `JpaRepository<T,ID> + JpaSpecificationExecutor<T>` ONLY when Specifications/dynamic
  filtering are needed.
- `mapper` — plain `@Component` class with `toDto`/`toEntity` methods (no MapStruct interface required).
- `service` (interface) + `service/impl` (class annotated `@Service`, constructor injection, SLF4J logger,
  log key actions like auth-service does).
- `controller` — `@RestController`, `@RequestMapping`, `@Tag`, `@Operation`, `@PreAuthorize` roles exactly
  as specified in the domain spec. Use `com.debtpulse.common.dto.PageResponse` for paginated results.
- `config` — `SecurityConfig`, `OpenApiConfig`, `FeignConfig` (see below).
- `feign` — `@FeignClient` interfaces + `feign/dto` (local record copies of the JSON) + `feign/fallback`
  (fallback classes for Resilience4j).

## config/SecurityConfig (copy auth-service, adjust permitAll)
- `@Configuration @EnableWebSecurity @EnableMethodSecurity`, stateless, csrf disabled.
- permitAll: `"/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**","/webjars/**","/actuator/**","/error"`
  and `"/api/internal/**"` requires authenticated (leave under `.anyRequest().authenticated()`).
- Register the shared filter: `.addFilterBefore(new com.debtpulse.common.security.RoleBasedHeaderFilter(), UsernamePasswordAuthenticationFilter.class)`
- Do NOT declare a PasswordEncoder bean (not needed outside auth-service).

## config/OpenApiConfig — copy auth-service's, change title/description. Include the JWT bearerAuth scheme.

## config/FeignConfig
```java
@org.springframework.context.annotation.Configuration
@org.springframework.context.annotation.Import(com.debtpulse.common.config.FeignClientInterceptor.class)
public class FeignConfig {}
```

## Feign clients + Resilience4j
- Declare `@FeignClient(name = "auth-service", path = "/api/internal", fallback = AuthClientFallback.class)` etc.
- Provide a `@Component` fallback class per client returning safe defaults (null / empty list / false / 0) and logging a warning.
- The shared config already sets `feign.circuitbreaker.enabled=true` and Resilience4j instances.

## Shared library (already built) — use these, do NOT recreate:
- `com.debtpulse.common.dto.PageResponse`
- `com.debtpulse.common.enums.*` (all domain enums + the shared `Role`)
- `com.debtpulse.common.validation.{StrongPassword, Phone, CorporateEmail}` (reusable Bean Validation
  constraints — apply on request DTOs; combine with `@NotBlank` where the field is mandatory)
- `com.debtpulse.common.id.BusinessId` (identifier generator annotation for entity `@Id` fields)
- `com.debtpulse.common.security.{AuthContext,SecurityHeaders,RoleBasedHeaderFilter}`
- `com.debtpulse.common.config.FeignClientInterceptor`
- `com.debtpulse.common.aspect.LoggingAspect`

> NOTE: exceptions + the `GlobalExceptionHandler` are NO LONGER in common-lib — each service
> defines its own under `com.debtpulse.<svc>.exception` (see the `exception` layer above).
Current user id / role inside a service: `AuthContext.currentUserId()` / `AuthContext.currentRole()`.

## Flyway migration
- `src/main/resources/db/migration/V1__<svc>_schema.sql` — `CREATE TABLE IF NOT EXISTS ...` for every
  entity table (snake_case columns). Enum columns as `VARCHAR`. No cross-schema foreign keys.

## Tests (JUnit 5 + Mockito) — REQUIRED
- At least one `*ServiceImplTest` (pure Mockito, `@ExtendWith(MockitoExtension.class)`) covering happy path
  + a not-found/business-rule path.
- At least one `*ControllerTest` using `MockMvcBuilders.standaloneSetup(controller)` with a mocked service
  (mirror `auth-service`'s `AuthControllerTest`). Use AssertJ.

## Logging
- `logging.file.name=logs/spring.log` comes from config-server; the shared `LoggingAspect` logs all
  controller/service methods. Also keep explicit `log.info(...)` in service impls.
