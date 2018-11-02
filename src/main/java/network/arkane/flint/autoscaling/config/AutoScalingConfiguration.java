package network.arkane.flint.autoscaling.config;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoScalingConfiguration {

    @Bean
    public AmazonAutoScaling provideAutoScaling() {
        AmazonAutoScalingClientBuilder standard = AmazonAutoScalingClientBuilder.standard();
        standard.setCredentials(new ProfileCredentialsProvider());
        return standard.build();
    }
}
