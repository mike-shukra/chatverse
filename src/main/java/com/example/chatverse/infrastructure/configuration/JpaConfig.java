package com.example.chatverse.infrastructure.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@Configuration
@ComponentScan("com.example.chatverse")
@EnableJpaRepositories("com.example.chatverse.domain.repository")
@EnableTransactionManagement
public class JpaConfig {}