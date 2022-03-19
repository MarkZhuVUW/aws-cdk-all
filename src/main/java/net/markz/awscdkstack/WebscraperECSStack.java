package net.markz.awscdkstack;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.AddCapacityOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.Ec2Service;
import software.amazon.awscdk.services.ecs.Ec2TaskDefinition;
import software.amazon.awscdk.services.ecs.EcsOptimizedImage;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.constructs.Construct;

import java.util.Collections;

public class WebscraperECSStack extends Stack {
    public WebscraperECSStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public WebscraperECSStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Vpc vpc = Vpc.Builder.create(this, "MarkZVPC")
                .maxAzs(1)  // Default is all AZs in region
                .build();

        Cluster cluster = Cluster
                .Builder
                .create(this, "MarkZECS")
                .vpc(vpc)
                .capacity(
                        AddCapacityOptions
                                .builder()
                                .instanceType(InstanceType.of(InstanceClass.BURSTABLE4_GRAVITON, InstanceSize.NANO))
                                .machineImage(EcsOptimizedImage.amazonLinux2())
                                .desiredCapacity(1)
                                .spotPrice("0.004")
                                .build()
                )
                .build();

        Ec2TaskDefinition ec2TaskDefinition = Ec2TaskDefinition
                .Builder
                .create(this, "WebscraperService-Ec2TaskDefinition")
                .build();

        ec2TaskDefinition.addContainer("WebscraperService-Ec2TaskDefinition-Container",
                ContainerDefinitionOptions
                        .builder()
                        .image(ContainerImage.fromRegistry("zdy120939259/web-scraper-service:latest"))
                        .cpu(1)
                        .portMappings(Collections.singletonList(PortMapping
                                .builder()
                                .containerPort(8080)
                                .build()))
                        .memoryLimitMiB(512)
                        .build()
        );

        Ec2Service.Builder.create(this, "WebscraperService-SpotInstanceEC2")
                .cluster(cluster)           // Required
                .desiredCount(1) // how much instances we want
                .taskDefinition(ec2TaskDefinition)
                .assignPublicIp(false)
                .build();
    }

    // Create a load-balanced Fargate service and make it public
//        ApplicationLoadBalancedFargateService fargateService =
//                ApplicationLoadBalancedFargateService.Builder.create(this, "WebscraperService")
//                        .cluster(cluster)           // Required
//                        .cpu(512) // how much cpu we need use
//                        .desiredCount(2) // how much instances we want
//                        .listenerPort(8080)
//                        .memoryLimitMiB(1024)
//                        .publicLoadBalancer(true)   // Default is false
//                        .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
//                                // The Webscraper app CI pushes the docker image to dockerhub.
//                                .image(ContainerImage.fromRegistry("zdy120939259/web-scraper-service:0.0.1"))
//                                .containerPort(8080) // Webscraper docker image exposed port.
//                                .build())
//                        .assignPublicIp(false)
//                        .build();
    //        fargateService.
//                // For our Spring boot application, we need to configure the health check
//                        fargateService.getTargetGroup().configureHealthCheck(HealthCheck.builder()
//                .healthyHttpCodes("200") // Specify which http codes are considered healthy
//                // The load balancer REQUIRES a healthcheck endpoint to determine the state of the app.
//                // In this example, we're using the Spring Actuator. Configure this in your app if missing.
//                .path("/actuator/health")
//                .port("8080") // The default is port 80
//                .build());
}
