package net.markz.awscdkstack.services.webscraperservice;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.AmiHardwareType;
import software.amazon.awscdk.services.ecs.AsgCapacityProvider;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.CapacityProviderStrategy;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.Ec2Service;
import software.amazon.awscdk.services.ecs.Ec2TaskDefinition;
import software.amazon.awscdk.services.ecs.EcsOptimizedImage;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PlacementConstraint;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

import java.util.Collections;
import java.util.List;

public class ECSEC2 extends Stack {
    public ECSEC2(final Construct scope, final String id, final StackProps props) {
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

        final var securityGroup = SecurityGroup
                .Builder
                .create(this, "ECS-SecurityGroup")
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();

        securityGroup.addIngressRule(Peer.ipv4("125.239.40.58/32"), Port.allTcp()); // currently only myself can access this sg.




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
                                .machineImage(EcsOptimizedImage.amazonLinux2())
                                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.NANO))
                                .spotPrice("0.003")
                                .keyName("mykey")
                                .securityGroup(securityGroup)
                                .build()
                )
                .build();

        cluster.addAsgCapacityProvider(
                asgCapacityProvider
        );
        final var ec2TaskDefinition = Ec2TaskDefinition
                .Builder
                .create(this, "WebscraperService-Ec2TaskDefinition")
                .build();

        LogDriver logDriver = LogDriver.awsLogs(
                AwsLogDriverProps
                        .builder()
                        .logGroup(
                                LogGroup
                                        .Builder
                                        .create(this, "webscraper-logs")
                                        .build()
                        )
                        .streamPrefix("WebscraperService")
                        .build()
        );
        ec2TaskDefinition.addContainer("WebscraperService-Ec2TaskDefinition-Container",
                ContainerDefinitionOptions
                        .builder()
                        .image(ContainerImage.fromRegistry("zdy120939259/web-scraper-service:0.0.1"))
                        .portMappings(Collections.singletonList(PortMapping
                                .builder()
                                .containerPort(8080)
                                .hostPort(80)
                                .build()))
                        .memoryLimitMiB(430)
                        .logging(logDriver)
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
    }

}
