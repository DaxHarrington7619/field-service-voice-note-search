package dev.infrai.fieldnotes;

import dev.infrai.fieldnotes.config.InfraiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(InfraiProperties.class)
public class FieldNoteSearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(FieldNoteSearchApplication.class, args);
    }
}
