package com.gymcrm.gym_crm_spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class GymCrmSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(GymCrmSpringApplication.class, args);
    }
}



