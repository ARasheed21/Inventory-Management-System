package com.example.inventory.infrastructure.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class EnversConfig {

    @Bean
    public RevisionListener revisionListener() {
        return new AuditRevisionListener();
    }
}
