package dev.infrai.healthupload;

import dev.infrai.healthupload.config.InfraiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(InfraiProperties.class)
public class HealthUploadApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthUploadApplication.class, args);
    }
}
