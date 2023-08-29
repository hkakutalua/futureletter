package com.strategicimperatives.futureletter.config;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfiguration {
    @Bean
    public Jdk8Module jsonNullableModule() {
        return new Jdk8Module();
    }
}
