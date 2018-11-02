package network.arkane.flint.autoscaling;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class AutoScalingCommand implements CommandLineRunner {

    private AmazonAutoScaling autoScalingClient;
    private Options options;
    private CommandLineParser parser;
    private CommandLine cmd;
    private HelpFormatter formatter;


    public AutoScalingCommand(AmazonAutoScaling autoScalingClient) {
        this.autoScalingClient = autoScalingClient;
    }

    public List<AutoScalingGroup> getAutoScalingGroups() {
        return autoScalingClient.describeAutoScalingGroups().getAutoScalingGroups();
    }

    @PostConstruct
    public void init() {
        options = new Options();
        parser = new DefaultParser();
        formatter = new HelpFormatter();

        final Option listOption = Option.builder("list")
                                        .desc("Will print out all available autoscaling groups")
                                        .build();
        final Option upScaleOption = Option.builder("upscale")
                                           .desc("Will upgrade the specific AutoScalingGroup")
                                           .hasArg()
                                           .argName("name")
                                           .build();
        final Option downScaleOption = Option.builder("downscale")
                                             .desc("Will downscale the specific AutoScalingGroup")
                                             .hasArg()
                                             .argName("name")
                                             .build();
        options.addOption(listOption);
        options.addOption(upScaleOption);
        options.addOption(downScaleOption);
    }

    @Override
    public void run(String... args) throws Exception {
        if ("autoscaling".equals(args[0]) || "as".equals(args[0])) {
            try {
                cmd = parser.parse(options, args);
            } catch (final Exception ex) {
                formatter.printHelp("flint autoscaling", options);
                return;
            }

            if (cmd.hasOption("list")) {
                printGroups();
            } else if (cmd.hasOption("upscale") && !cmd.getOptionValue("upgrade").isEmpty()) {
                upscaleASG(cmd.getOptionValue("upgrade"));
            } else if (cmd.hasOption("downscale") && !cmd.getOptionValue("downscale").isEmpty()) {
                downScaleASG(cmd.getOptionValue("downscale"));
            } else {
                printGroups();
            }
        }
    }

    @SneakyThrows
    private void downgradeWhenAllHealthy(final String asGroup) {
        final DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest = new DescribeAutoScalingGroupsRequest();
        describeAutoScalingGroupsRequest.setAutoScalingGroupNames(Arrays.asList(asGroup));

        final DescribeAutoScalingGroupsResult result = this.autoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest);
        if (result.getAutoScalingGroups().isEmpty()) {
            System.out.println("Unable to find autoscaling group " + asGroup);
        } else {
            final AutoScalingGroup autoScalingGroup = result.getAutoScalingGroups().get(0);
            final List<Instance> instances = autoScalingGroup.getInstances();
            if (instances.size() <= 1) {
                System.out.println("Only " + instances.size() + " instances, waiting...");
                Thread.sleep(10000L);
                downgradeWhenAllHealthy(asGroup);
            } else {
                System.out.println("Enough instances found to downscale");
                if (instances.stream().anyMatch(x -> !x.getHealthStatus().equals("Healthy"))) {
                    System.out.println("At least one instance is not healthy, waiting to downscale");
                    downgradeWhenAllHealthy(asGroup);
                } else {
                    scaleInstances(asGroup, 1);
                }
            }
        }
    }

    private void upscaleASG(String asGroup) {
        scaleInstances(asGroup, 2);
    }

    private void downScaleASG(final String asGroup) {
        downgradeWhenAllHealthy(asGroup);
    }

    private void scaleInstances(String asGroup, final int capacity) {
        System.out.println("Setting Desired capacity of " + asGroup + " to " + capacity);
        final UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest = new UpdateAutoScalingGroupRequest();
        updateAutoScalingGroupRequest.setAutoScalingGroupName(asGroup);
        updateAutoScalingGroupRequest.setDesiredCapacity(capacity);
        final UpdateAutoScalingGroupResult updateAutoScalingGroupResult = autoScalingClient.updateAutoScalingGroup(updateAutoScalingGroupRequest);
        System.out.println(updateAutoScalingGroupResult.toString());
    }

    private void printGroups() {
        System.out.println("Available AS-Groups:\n");
        getAutoScalingGroups().forEach(x -> System.out.println(x.getAutoScalingGroupName()));
    }
}
