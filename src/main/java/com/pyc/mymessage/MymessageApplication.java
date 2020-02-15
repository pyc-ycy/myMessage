package com.pyc.mymessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.scheduling.PollerMetadata;

@SpringBootApplication
public class MymessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MymessageApplication.class, args);
    }

    // using @value annotation to gain resources from https://spring.io/blog.atom by automatic
    @Value("https://spring.io/blog.atom")
    Resource resource;

    // using Fluent API and pollers to configure acquiescent way of poll
    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata poller(){
        return Pollers.fixedRate(500).get();
    }

}
