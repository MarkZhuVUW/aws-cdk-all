package net.markz.awscdkstack.services.webscraperservice;


import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.AmiHardwareType;
import software.amazon.awscdk.services.ecs.AsgCapacityProvider;
import software.amazon.awscdk.services.ecs.CapacityProviderStrategy;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.Ec2Service;
import software.amazon.awscdk.services.ecs.Ec2TaskDefinition;
import software.amazon.awscdk.services.ecs.EcsOptimizedImage;
import software.amazon.awscdk.services.ecs.NetworkMode;
import software.amazon.awscdk.services.ecs.PlacementConstraint;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.constructs.Construct;

import java.util.Collections;
import java.util.List;

public class ECSFargate extends Stack {
    public ECSFargate(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        final var vpc = Vpc.Builder.create(this, "MarkZVPC")
                .maxAzs(1)  // Default is all AZs in region
                .subnetConfiguration(
                        List.of(SubnetConfiguration
                                .builder()
                                .subnetType(SubnetType.PUBLIC)
                                .name("MarkZPublicSubnetConfig")
                                .build())
                )
                .natGateways(0)
                .build();

        final var cluster = Cluster
                .Builder
                .create(this, "MarkZECS")
                .vpc(vpc)
                .build();

        final var asgCapacityProvider = AsgCapacityProvider
                .Builder
                .create(this, "Webscraper-AsgCapacityProvider")
                .autoScalingGroup(
                        AutoScalingGroup
                                .Builder
                                .create(this, "MarkZECS-Cluster-AutoScalingGroup")
                                .vpc(vpc)
                                .minCapacity(0)
                                .maxCapacity(1)
                                .desiredCapacity(1)
                                .machineImage(EcsOptimizedImage.amazonLinux2(AmiHardwareType.ARM))
                                .instanceType(InstanceType.of(InstanceClass.BURSTABLE4_GRAVITON, InstanceSize.NANO))
                                .spotPrice("0.004")
                                .keyName("mykey")
                                .build()
                )
                .build();

        cluster.addAsgCapacityProvider(
                asgCapacityProvider
        );
        final var ec2TaskDefinition = Ec2TaskDefinition
                .Builder
                .create(this, "WebscraperService-Ec2TaskDefinition")
                .placementConstraints(List.of(
                        PlacementConstraint.memberOf("attribute:ecs.cpu-architecture == arm64")
                ))
                .networkMode(NetworkMode.HOST)
                .build();

        ec2TaskDefinition.addContainer("WebscraperService-Ec2TaskDefinition-Container",
                ContainerDefinitionOptions
                        .builder()
                        .image(ContainerImage.fromRegistry("zdy120939259/web-scraper-service:0.0.1"))
                        .portMappings(Collections.singletonList(PortMapping
                                .builder()
                                .containerPort(8080)
                                .build()))
                        .memoryLimitMiB(400)
                        .build()
        );

        final var defaultCPS = CapacityProviderStrategy
                .builder()
                .capacityProvider(asgCapacityProvider.getCapacityProviderName())
                .weight(1)
                .build();
        Ec2Service.Builder.create(this, "WebscraperService-SpotInstanceEC2")
                .cluster(cluster)           // Required
                .desiredCount(1) // how many instances we want
                .taskDefinition(ec2TaskDefinition)
                .capacityProviderStrategies(List.of(defaultCPS))
                .maxHealthyPercent(200)
                .minHealthyPercent(0)
                .build();

        // Create a load-balanced Fargate service and make it public

        ApplicationLoadBalancedFargateService fargateService =
                ApplicationLoadBalancedFargateService.Builder.create(this, "WebscraperService")
                        .cluster(cluster)           // Required
                        .cpu(512) // how much cpu we need use
                        .desiredCount(2) // how much instances we want
                        .listenerPort(8080)
                        .memoryLimitMiB(1024)
                        .publicLoadBalancer(true)   // Default is false
                        .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                                // The Webscraper app CI pushes the docker image to dockerhub.
                                .image(ContainerImage.fromRegistry("zdy120939259/web-scraper-service:0.0.1"))
                                .containerPort(8080) // Webscraper docker image exposed port.
                                .build())
                        .assignPublicIp(false)
                        .build();

        // For our Spring boot application, we need to configure the health check
        fargateService.getTargetGroup().configureHealthCheck(
                HealthCheck.builder()
                        .healthyHttpCodes("200") // Specify which http codes are considered healthy
                        // The load balancer REQUIRES a healthcheck endpoint to determine the state of the app.
                        // In this example, we're using the Spring Actuator. Configure this in your app if missing.
                        .path("/actuator/health")
                        .port("8080") // The default is port 80
                        .build());

    }

}
