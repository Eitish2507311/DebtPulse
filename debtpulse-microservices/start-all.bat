@echo off
REM ============================================================================
REM Launches every DebtPulse service in its own window, in dependency order.
REM Prerequisite: MySQL running on localhost:3306 (root/root) and each module built
REM   (run build.sh / mvn install first).
REM Start order matters: Config Server -> Eureka -> Gateway -> business services.
REM ============================================================================
setlocal
set MVN=mvn -q -Dmaven.resolver.transport=wagon -Dmaven.wagon.http.ssl.insecure=true spring-boot:run

echo Starting config-server (8888)...
start "config-server" cmd /k "cd config-server && %MVN%"
timeout /t 25 /nobreak

echo Starting eureka-server (8761)...
start "eureka-server" cmd /k "cd eureka-server && %MVN%"
timeout /t 25 /nobreak

echo Starting api-gateway (9090)...
start "api-gateway" cmd /k "cd api-gateway && %MVN%"

echo Starting business services...
start "auth-service (8081)"         cmd /k "cd auth-service && %MVN%"
start "account-service (8082)"      cmd /k "cd account-service && %MVN%"
start "contact-service (8083)"      cmd /k "cd contact-service && %MVN%"
start "field-service (8084)"        cmd /k "cd field-service && %MVN%"
start "settlement-service (8085)"   cmd /k "cd settlement-service && %MVN%"
start "legal-service (8086)"        cmd /k "cd legal-service && %MVN%"
start "analytics-service (8087)"    cmd /k "cd analytics-service && %MVN%"
start "notification-service (8088)" cmd /k "cd notification-service && %MVN%"

echo All services launching. Swagger aggregation: http://localhost:9090/swagger-ui.html
echo Eureka dashboard: http://localhost:8761
endlocal
