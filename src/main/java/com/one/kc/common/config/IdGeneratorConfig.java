package com.one.kc.common.config;

import com.one.kc.common.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfig {

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(
            @Value("${snowflake.worker-id}") long workerId,
            @Value("${snowflake.datacenter-id}") long datacenterId) {

        return new SnowflakeIdGenerator(workerId, datacenterId);
    }
}

