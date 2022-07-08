package net.markz.awscdkstack.services.webscraperservice;

import lombok.AllArgsConstructor;
import net.markz.awscdkstack.constants.AWSConstants;
import net.markz.awscdkstack.constants.NetworkConstants;
import net.markz.awscdkstack.nestedstacks.MarkZVPC;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.NestedStackProps;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableClass;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
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
import software.amazon.awscdk.services.elasticache.CfnReplicationGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetGroupsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerAction;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerCertificate;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerCondition;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.RedirectOptions;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.List;

@AllArgsConstructor
public class WebscraperService extends Stack {

    @lombok.Builder
    public WebscraperService(
            final Construct scope,
            final String id,
            final StackProps props
    ) {
        super(scope, id, props);

        final var markZVpc =  MarkZVPC
                .builder()
                .scope(this)
                .id("MarkZVPCStack")
                .props(
                        NestedStackProps
                                .builder()
                                .build()
                )
                .build()
                .getMarkZVpc();

        final var asgDebugSG = SecurityGroup
                .Builder
                .create(this, "WebscraperService-ASG-Debug-sg")
                .vpc(markZVpc)
                .allowAllOutbound(true)
                .disableInlineRules(true)
                .build();

        // currently only myself can access the ALB.
        asgDebugSG.addIngressRule(Peer.ipv4(NetworkConstants.MY_IP_1.getStr()), Port.allTcp());
        asgDebugSG.addIngressRule(Peer.ipv4(NetworkConstants.MY_IP_2.getStr()), Port.allTcp());

        final var albSG = SecurityGroup
                .Builder
                .create(this, "WebscraperService-ALB-sg")
                .vpc(markZVpc)
                .disableInlineRules(true)
                .allowAllOutbound(true)
                .build();


        // currently only myself can access the ALB.
        albSG.addIngressRule(Peer.ipv4(NetworkConstants.MY_IP_1.getStr()), Port.allTcp());
        albSG.addIngressRule(Peer.ipv4(NetworkConstants.MY_IP_2.getStr()), Port.allTcp());

        final var cluster = Cluster
                .Builder
                .create(this, "MarkZECS")
                .clusterName(AWSConstants.ECS_CLUSTER.getStr())
                .vpc(markZVpc)
                .build();

        cluster.applyRemovalPolicy(RemovalPolicy.DESTROY);

        final var asg = AutoScalingGroup
                .Builder
                .create(this, "MarkZECS-Cluster-AutoScalingGroup")
                .vpc(markZVpc)
                .minCapacity(0)
                .maxCapacity(1)
                .desiredCapacity(1)
                .machineImage(EcsOptimizedImage.amazonLinux2())
                .securityGroup(asgDebugSG)
                .vpcSubnets(SubnetSelection.builder().subnets(List.of(
                        markZVpc.getPublicSubnets().get(0)
                )).build())
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
//                                .spotPrice("0.003") // using free tier now.
                .keyName("mykey")
                .healthCheck(software.amazon.awscdk.services.autoscaling.HealthCheck.ec2())
                .newInstancesProtectedFromScaleIn(false) // disable scale-in protection to help destory stacks.
                .associatePublicIpAddress(true)
                .build();

        asg.applyRemovalPolicy(RemovalPolicy.DESTROY);

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

        var logDriver = LogDriver.awsLogs(
                AwsLogDriverProps
                        .builder()
                        .logGroup(
                                LogGroup
                                        .Builder
                                        .create(this, "WebscraperService-LogDriver")
//                                        .logGroupName("/webscraper-service")
                                        .removalPolicy(RemovalPolicy.RETAIN)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build()
                        )
                        .streamPrefix("WebscraperService")
                        .build()
        );
        ec2TaskDefinition.addContainer("WebscraperService-Ec2TaskDefinition-SpringBootApp-Container",
                ContainerDefinitionOptions
                        .builder()
                        .image(ContainerImage.fromRegistry("zdy120939259/web-scraper-service:latest"))

                        .portMappings(List.of(
                                PortMapping
                                        .builder()
                                        .containerPort(80)
                                        .build()
                        ))
                        .memoryLimitMiB(300)
                        .cpu(1024)
                        .logging(logDriver)
                        .healthCheck(
                                software.amazon.awscdk.services.ecs.HealthCheck
                                        .builder()
                                        .command(List.of(
                                                "CMD-SHELL", "curl -f http://localhost:80/webscraper-api/status || exit 1"
                                        ))
                                        .build()
                        )
                        .build()
        );

        ec2TaskDefinition.addContainer("WebscraperService-Ec2TaskDefinition-SeleniumRemoteChromeDriverService-Container",
                ContainerDefinitionOptions
                        .builder()
                        .image(ContainerImage.fromRegistry("selenium/standalone-chrome"))
                        .portMappings(List.of(
                                PortMapping
                                        .builder()
                                        .containerPort(4444)
                                        .build()
                        ))
                        .cpu(1024)
                        .memoryLimitMiB(630)
                        .healthCheck(
                                software.amazon.awscdk.services.ecs.HealthCheck
                                        .builder()
                                        .command(List.of(
                                                "CMD-SHELL", "curl -f http://localhost:4444/status || exit 1"
                                        ))
                                        .build()
                        )
                        .build()
        );

        ec2TaskDefinition.applyRemovalPolicy(RemovalPolicy.DESTROY);

        final var defaultCPS = CapacityProviderStrategy
                .builder()
                .capacityProvider(asgCapacityProvider.getCapacityProviderName())
                .weight(1)
                .build();

        final var ec2Service = Ec2Service.Builder.create(this, "WebscraperService-SpotInstanceEC2")
                .cluster(cluster)
                .serviceName(AWSConstants.ECS_SERVICE.getStr())
                .taskDefinition(ec2TaskDefinition)
                .maxHealthyPercent(200)
                .minHealthyPercent(0)
                .capacityProviderStrategies(List.of(defaultCPS))
                // One task per ec2 instance
                .placementConstraints(List.of(PlacementConstraint.distinctInstances()))
                .build();

        ec2Service.applyRemovalPolicy(RemovalPolicy.SNAPSHOT);
        // Add an alb that listen to the ASG
        final var alb = ApplicationLoadBalancer
                .Builder
                .create(this, "WebscraperService-ALB")
                .vpc(markZVpc)
                .vpcSubnets(SubnetSelection.builder().subnets(markZVpc.getPublicSubnets()).build())
                .internetFacing(true)
                .securityGroup(albSG)
                .build();

        final var appTargetGroup = ApplicationTargetGroup
                .Builder
                .create(this, "WebscraperService-ALB-Listener-TargetGroup-www.markz-portfolio.uk")
                .vpc(markZVpc)
                .port(80)
                .protocol(ApplicationProtocol.HTTP)
                .healthCheck(
                        HealthCheck.builder()
                                .protocol(Protocol.HTTP)
                                .healthyHttpCodes("200") // Specify which http codes are considered healthy
                                // The load balancer REQUIRES a healthcheck endpoint to determine the state of the app.
                                // In this example, we're using the Spring Actuator. Configure this in your app if missing.
                                .path(String.format("%s/status", NetworkConstants.WEBSCRAPER_SERVICE_PATH_PATTERN.getStr()))
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
                                        .fromArn(AWSConstants.MY_PUBLIC_SSL_CERTIFICATE_ARN_AP_SOUTHEAST_2.getStr())
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

        final var onlineShoppingItems = Table
                .Builder
                .create(this, "WebscraperService-DynamoDB-OnlineShoppingItemsTable")
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .timeToLiveAttribute("ttl")
                .sortKey(Attribute.builder().name("SK").type(AttributeType.STRING).build())
                .partitionKey(Attribute.builder().name("PK").type(AttributeType.STRING).build())
                .tableClass(TableClass.STANDARD_INFREQUENT_ACCESS)
//                .readCapacity(1L)
//                .writeCapacity(1L)
                .tableName(AWSConstants.WEBSCRAPERSERVICE_DYNAMODB_TABLE_ONLINESHOPPINGITEMS.getStr())
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        final var dlq = Queue
                .Builder
                .create(this, "WebscraperService-SQS-DLQ")
                .removalPolicy(RemovalPolicy.DESTROY)
                .queueName(AWSConstants.WEBSCRAPERSERVICE_SQS_DLQ_NAME.getStr())
                .visibilityTimeout(Duration.seconds(
                        Integer.parseInt(AWSConstants.WEBSCRAPERSERVICE_SQS_VISIBILITY_TIMEOUT_SECONDS.getStr())
                ))

                .build();

        final var queue = Queue
                .Builder
                .create(this, "WebscraperService-SQS-Queue")
                .removalPolicy(RemovalPolicy.DESTROY)
                .queueName(AWSConstants.WEBSCRAPERSERVICE_SQS_QUEUE_NAME.getStr())
                .visibilityTimeout(Duration.seconds(
                        Integer.parseInt(AWSConstants.WEBSCRAPERSERVICE_SQS_VISIBILITY_TIMEOUT_SECONDS.getStr())
                ))
                .deadLetterQueue(
                        DeadLetterQueue
                                .builder()
                                .queue(dlq)
                                .maxReceiveCount(10)
                                .build()
                )
                .build();

//        final var redisCluster = CfnReplicationGroup
//                .Builder
//                .create(this, "WebscraperService-RedisCluster")
//                .port(1234)
//                .engine("redis")
//                .automaticFailoverEnabled(false)
//                .cacheNodeType("cache.t4g.micro")
//                .
//                .build();
    }

}

