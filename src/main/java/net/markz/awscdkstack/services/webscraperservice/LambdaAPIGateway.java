package net.markz.awscdkstack.services.webscraperservice;

import net.markz.awscdkstack.constants.NetworkConstants;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.ec2.AclCidr;
import software.amazon.awscdk.services.ec2.AclTraffic;
import software.amazon.awscdk.services.ec2.CommonNetworkAclEntryOptions;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.NetworkAcl;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
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
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Handler;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

import java.util.Collections;
import java.util.List;

public class LambdaAPIGateway extends Stack {
    public LambdaAPIGateway(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
//        final var vpc = Vpc.Builder.create(this, "MarkZVPC")
//                .maxAzs(1)  // Default is all AZs in region
//                .subnetConfiguration(
//                        List.of(SubnetConfiguration
//                                .builder()
//                                .subnetType(SubnetType.PUBLIC)
//                                .name("MarkZPublicSubnetConfig")
//                                .build())
//                )
//                .natGateways(0)
//                .build();
//        var networkAcl = NetworkAcl.Builder.create(this, "MarkZVPC-NetworkACL")
//                .vpc(vpc)
//                .networkAclName("MarkZVPC")
//                .subnetSelection(
//                        SubnetSelection
//                                .builder()
//                                .subnets(vpc.getPublicSubnets())
//                                .subnetType(SubnetType.PUBLIC)
//                                .build()
//                )
//                .build();
//
//        // currently only myself can access the services.
//        networkAcl.addEntry("networkAcl rule for my home ip 1",
//                CommonNetworkAclEntryOptions
//                        .builder()
//                        .traffic(AclTraffic.allTraffic())
//                        .cidr(AclCidr.ipv4(NetworkConstants.MY_IP_1.getStr()))
//                        .build()
//        );
//        networkAcl.addEntry("networkAcl rule for my home ip 2",
//                CommonNetworkAclEntryOptions
//                        .builder()
//                        .traffic(AclTraffic.allTraffic())
//                        .cidr(AclCidr.ipv4(NetworkConstants.MY_IP_2.getStr()))
//                        .build()
//        );
//        final var cluster = Cluster
//                .Builder
//                .create(this, "MarkZECS")
//                .vpc(vpc)
//                .build();
//
//        final var securityGroup = SecurityGroup
//                .Builder
//                .create(this, "ECS-SecurityGroup")
//                .vpc(vpc)
//                .allowAllOutbound(true)
//                .build();
//
//        // currently only myself can access the services.
//        securityGroup.addIngressRule(Peer.ipv4(NetworkConstants.MY_IP_1.getStr()), Port.tcp(80));
//        securityGroup.addIngressRule(Peer.ipv4(NetworkConstants.MY_IP_2.getStr()), Port.tcp(80));
//        securityGroup.addIngressRule(Peer.ipv4(NetworkConstants.MY_IP_1.getStr()), Port.tcp(443));
//        securityGroup.addIngressRule(Peer.ipv4(NetworkConstants.MY_IP_2.getStr()), Port.tcp(443));
//
//
//
//        final var asgCapacityProvider = AsgCapacityProvider
//                .Builder
//                .create(this, "Webscraper-AsgCapacityProvider")
//                .autoScalingGroup(
//                        AutoScalingGroup
//                                .Builder
//                                .create(this, "MarkZECS-Cluster-AutoScalingGroup")
//                                .vpc(vpc)
//                                .minCapacity(0)
//                                .maxCapacity(1)
//                                .desiredCapacity(1)
//                                .machineImage(EcsOptimizedImage.amazonLinux2())
//                                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
////                                .spotPrice("0.003") // using free tier now.
//                                .keyName("mykey")
//                                .securityGroup(securityGroup)
//                                .build()
//                )
//                .build();
//
//        cluster.addAsgCapacityProvider(
//                asgCapacityProvider
//        );
//        final var ec2TaskDefinition = Ec2TaskDefinition
//                .Builder
//                .create(this, "WebscraperService-Ec2TaskDefinition")
//                .build();
//
//        LogDriver logDriver = LogDriver.awsLogs(
//                AwsLogDriverProps
//                        .builder()
//                        .logGroup(
//                                LogGroup
//                                        .Builder
//                                        .create(this, "webscraper-logs")
//                                        .build()
//                        )
//                        .streamPrefix("WebscraperService")
//                        .build()
//        );
//        ec2TaskDefinition.addContainer("WebscraperService-Ec2TaskDefinition-Container",
//                ContainerDefinitionOptions
//                        .builder()
//                        .image(ContainerImage.fromRegistry("zdy120939259/web-scraper-service:latest"))
//                        .portMappings(Collections.singletonList(PortMapping
//                                .builder()
//                                .containerPort(8080)
//                                .hostPort(80)
//                                .build()))
//                        .memoryLimitMiB(950)
////                        .logging(logDriver) // save money.
//                        .build()
//        );
//
//        final var defaultCPS = CapacityProviderStrategy
//                .builder()
//                .capacityProvider(asgCapacityProvider.getCapacityProviderName())
//                .weight(1)
//                .build();
//        Ec2Service.Builder.create(this, "WebscraperService-SpotInstanceEC2")
//                .cluster(cluster)           // Required
//                .desiredCount(1) // how many instances we want
//                .taskDefinition(ec2TaskDefinition)
//                .capacityProviderStrategies(List.of(defaultCPS))
//                .maxHealthyPercent(200)
//                .minHealthyPercent(0)
//                .build();
//
//
//        Function handler = Function.Builder.create(this, "WebscraperSearchHandler")
//                .code(Code.)
//                .handler("widgets.main")
//                .environment(java.util.Map.of   // Java 9 or later
//                        "BUCKET", bucket.getBucketName())
//                .build();
//
//        RestApi api = RestApi.Builder.create(this, "Widgets-API")
//                .restApiName("Widget Service").description("This service services widgets.")
//                .build();
//
//        LambdaIntegration getWidgetsIntegration = LambdaIntegration.Builder.create(handler)
//                .requestTemplates(java.util.Map.of(   // Map.of is Java 9 or later
//                        "application/json", "{ \"statusCode\": \"200\" }"))
//                .build();
//
//        api.getRoot().addMethod("GET", getWidgetsIntegration);
    }

}
