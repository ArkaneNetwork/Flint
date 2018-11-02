package network.arkane.flint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j()
public class Flint {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No options were provided");
            System.out.println("Possible options are:");
            System.out.println("autoscaling");
        } else {
            SpringApplication.run(Flint.class, args);
        }
    }
}
