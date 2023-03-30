package org.rsinitsyn.quiz.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@ConditionalOnProperty(value = "quiz.profiling", havingValue = "true")
public class BusinessServicesAspect {

    @Pointcut("execution(* org.rsinitsyn.quiz.service.*.*(..))")
    public void serviceClassMethods() {
    }

    @Before("serviceClassMethods()")
    public void logMethodName2(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        log.info("=".repeat(10) + joinPoint.getSignature().getDeclaringTypeName() + '.' + methodName + "=".repeat(10));
    }
}
