package org.rsinitsyn.quiz.aop;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@ConditionalOnProperty(value = "quiz.profiling", havingValue = "true")
@RequiredArgsConstructor
public class BusinessServicesAspect {

    private final MeterRegistry meterRegistry;

    @Pointcut("execution(* org.rsinitsyn.quiz.service.*.*(..))")
    public void serviceLayerMethods() {
    }

    @SneakyThrows
    @Around("serviceLayerMethods()")
    public Object monitorTimeExecution(final ProceedingJoinPoint joinPoint) {
        final var signature = joinPoint.getSignature();
        final var className = signature.getDeclaringType().getSimpleName();
        final var methodName = signature.getName();

        log.debug("Monitor time execution [{}.{}]", className, methodName);

        final var timer = Timer.builder("quiz.service.response.time")
                .tag("class", className)
                .tag("method", methodName)
                .register(meterRegistry);

        return timer.recordCallable(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }
}
