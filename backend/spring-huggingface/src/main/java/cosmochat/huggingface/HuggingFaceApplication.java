package cosmochat.huggingface;

import cosmochat.huggingface.config.HuggingFaceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(HuggingFaceProperties.class)
public class HuggingFaceApplication {
    public static void main(String[] args) {
        SpringApplication.run(HuggingFaceApplication.class, args);
    }
}
