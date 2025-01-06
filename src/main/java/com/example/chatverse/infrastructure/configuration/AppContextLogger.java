package com.example.chatverse.infrastructure.configuration;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppContextLogger {

    public AppContextLogger(ApplicationContext context) {
        for (String beanName : context.getBeanDefinitionNames()) {
            System.out.println(beanName);
        }
    }
}

