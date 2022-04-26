package net.markz.awscdkstack.services.webscraperservice;

import net.markz.awscdkstack.constants.AWSConstants;
import net.markz.awscdkstack.constants.NetworkConstants;
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
import software.amazon.awscdk.services.ecs.NetworkMode;
import software.amazon.awscdk.services.ecs.PlacementConstraint;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetGroupsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerAction;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerCertificate;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerCondition;
import software.amazon.awscdk.services.elasticloadbalancingv2.RedirectOptions;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

import java.util.Collections;
import java.util.List;

public class ALBECSEC2 extends Stack {
    public ALBECSEC2(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        final var vpc = Vpc.Builder.create(this, "MarkZVPC")
                .maxAzs(2)  // Default is all AZs in region
                .natGateways(0)
                .subnetConfiguration(
                        List.of(
                                SubnetConfiguration
                                        .builder()
                                        .subnetType(SubnetType.PUBLIC)
                                        .name("MarkZPublicSubnetConfig")
                                        .build()
                        )
                )
                .build();

        final var cluster = Cluster
                .Builder
                .create(this, "MarkZECS")
                .vpc(vpc)
                .build();

        final var asg = AutoScalingGroup
                .Builder
                .create(this, "MarkZECS-Cluster-AutoScalingGroup")
                .vpc(vpc)
                .minCapacity(0)
                .maxCapacity(1)
                .desiredCapacity(1)
                .machineImage(EcsOptimizedImage.amazonLinux2())
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
//                                .spotPrice("0.003") // using free tier now.
                .keyName("mykey")
                .healthCheck(software.amazon.awscdk.services.autoscaling.HealthCheck.ec2())
                .newInstancesProtectedFromScaleIn(false)
                .build();



        final var asgCapacityProvider = AsgCapacityProvider
                .Builder
                .create(this, "WebscraperService-AsgCapacityProvider")
                .autoScalingGroup(asg)
                .build();

        cluster.addAsgCapacityProvider(asgCapacityProvider);

        final var ec2TaskDefinition = Ec2TaskDefinition
                .Builder
                .create(this, "WebscraperService-Ec2TaskDefinition")
                .networkMode(NetworkMode.HOST)
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
                        .image(ContainerImage.fromRegistry("zdy120939259/web-scraper-service:latest"))
                        .portMappings(Collections.singletonList(PortMapping
                                .builder()
                                .containerPort(80)
                                .build()))
                        .memoryLimitMiB(925)
                        .logging(logDriver)
                        .build()
        );

        final var defaultCPS = CapacityProviderStrategy
                .builder()
                .capacityProvider(asgCapacityProvider.getCapacityProviderName())
                .weight(1)
                .build();

        Ec2Service.Builder.create(this, "WebscraperService-SpotInstanceEC2")
                .cluster(cluster)
                .taskDefinition(ec2TaskDefinition)
                .maxHealthyPercent(200)
                .minHealthyPercent(0)
                .capacityProviderStrategies(List.of(defaultCPS))
                // One task per ec2 instance
                .placementConstraints(List.of(PlacementConstraint.distinctInstances()))
                .build();

        final var albSG = SecurityGroup
                .Builder
                .create(this, "WebscraperService-ALB-sg")
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();

        // currently only myself can access the ALB.
        albSG.addIngressRule(Peer.ipv4(NetworkConstants.MY_IP_1.getStr()), Port.allTcp());
        albSG.addIngressRule(Peer.ipv4(NetworkConstants.MY_IP_2.getStr()), Port.allTcp());

        // Add an alb that listen to the ASG
        final var alb = ApplicationLoadBalancer
                .Builder
                .create(this, "WebscraperService-ALB")
                .vpc(vpc)
                .internetFacing(true)
                .securityGroup(albSG)
                .build();

        final var appTargetGroup = ApplicationTargetGroup
                .Builder
                .create(this, "WebscraperService-ALB-Listener-TargetGroup-www.markz-portfolio.uk")
                .vpc(vpc)
                .port(80)
                .protocol(ApplicationProtocol.HTTP)
                .healthCheck(
                        HealthCheck.builder()
                                .healthyHttpCodes("200") // Specify which http codes are considered healthy
                                // The load balancer REQUIRES a healthcheck endpoint to determine the state of the app.
                                // In this example, we're using the Spring Actuator. Configure this in your app if missing.
                                .path("/actuator/health")
                                .port("80") // The default is port 80
                                .build()
                )
                .targets(List.of(asg))
                .build();

        alb.addListener("WebscraperService-ALB-HTTPSListener-www.markz-portfolio.uk",
                ApplicationListenerProps
                        .builder()
                        .loadBalancer(alb)
                        .certificates(List.of(
                                ListenerCertificate
                                        .fromArn(AWSConstants.MY_PUBLIC_SSL_CERTIFICATE_ARN.getStr())
                        ))
                        .protocol(ApplicationProtocol.HTTPS)
                        .defaultAction(ListenerAction.forward(List.of(appTargetGroup)))
                        .port(443)
                        .build()
        );

        alb.addListener("WebscraperService-ALB-HTTPListener-www.markz-portfolio.uk",
                ApplicationListenerProps
                        .builder()
                        .loadBalancer(alb)
                        .protocol(ApplicationProtocol.HTTP)
                        .port(80)
                        .defaultAction(
                                ListenerAction.redirect(
                                        RedirectOptions
                                                .builder()
                                                .protocol(ApplicationProtocol.HTTPS.name())
                                                .port("443")
                                                .build()
                                ))
                        .build()
        );

        final var hostHeaders = ListenerCondition.hostHeaders(
                List.of(NetworkConstants.WEBSCRAPER_SERVICE_DOMAIN_NAME.getStr())
        );

        final var pathPatterns = ListenerCondition.pathPatterns(
                List.of(
                        NetworkConstants.WEBSCRAPER_SERVICE_PATH_PATTERN.getStr()
                )
        );

        alb.getListeners().get(1).addTargetGroups("WebscraperService-ALB-HTTPListener-TargetGroup-www.markz-portfolio.uk",
                AddApplicationTargetGroupsProps
                        .builder()
                        .priority(10)
                        .targetGroups(List.of(appTargetGroup))
                        .conditions(
                                List.of(
                                hostHeaders,
                                pathPatterns
                                )
                        )
                        .build()
        );

    }

}
