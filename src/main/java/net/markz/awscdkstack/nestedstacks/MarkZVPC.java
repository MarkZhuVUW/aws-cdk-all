package net.markz.awscdkstack.nestedstacks;

import lombok.Getter;
import software.amazon.awscdk.NestedStack;
import software.amazon.awscdk.NestedStackProps;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.FlowLogDestination;
import software.amazon.awscdk.services.ec2.FlowLogOptions;
import software.amazon.awscdk.services.ec2.FlowLogTrafficType;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

@Getter
public class MarkZVPC extends NestedStack {

    private final Vpc markZVpc;

    @lombok.Builder
    public MarkZVPC(final Construct scope, final String id, final NestedStackProps props) {
        super(scope, id, props);

        this.markZVpc = Vpc.Builder.create(this, "MarkZVPC")
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
                .enableDnsSupport(true)
                .flowLogs(Map.of("rejected accesses log",
                        FlowLogOptions
                                .builder()
                                .trafficType(FlowLogTrafficType.REJECT)
                                .destination(FlowLogDestination.toCloudWatchLogs(
                                        LogGroup
                                                .Builder
                                                .create(this, "MarkZVPC-FlowLog-LogDriver")
//                                        .logGroupName("/MarkZVPC")
                                                .removalPolicy(RemovalPolicy.RETAIN)
                                                .retention(RetentionDays.ONE_DAY)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        markZVpc.applyRemovalPolicy(RemovalPolicy.DESTROY);

        //        final var networkAcl = NetworkAcl
//                .Builder
//                .create(this, "MarkZVPC-PublicSubnet-NetworkAcl")
//                // I have two public subnets in two azs in ap-southeast-2, all subnets use this network acl.
//                .subnetSelection(SubnetSelection.builder().subnets(vpc.getPublicSubnets()).build())
//                .vpc(vpc)
//                .build();
//
//        // Then allow only myself to access the subnets.
//        networkAcl.addEntry("MarkZVPC-PublicSubnet-NetworkAcl-allow-my-ip1",
//                CommonNetworkAclEntryOptions
//                        .builder()
//                        .traffic(AclTraffic.allTraffic())
//                        .cidr(AclCidr.ipv4(NetworkConstants.MY_IP_1.getStr()))
//                        .direction(TrafficDirection.INGRESS)
//                        .ruleAction(Action.ALLOW)
//                        .ruleNumber(1)
//                        .build()
//        );
//
//        networkAcl.addEntry("MarkZVPC-PublicSubnet-NetworkAcl-allow-my-ip2",
//                CommonNetworkAclEntryOptions
//                        .builder()
//                        .traffic(AclTraffic.allTraffic())
//                        .cidr(AclCidr.ipv4(NetworkConstants.MY_IP_2.getStr()))
//                        .direction(TrafficDirection.INGRESS)
//                        .ruleAction(Action.ALLOW)
//                        .ruleNumber(2)
//                        .build()
//        );
//
//        networkAcl.addEntry("MarkZVPC-PublicSubnet-NetworkAcl-allow-all-outbound",
//                CommonNetworkAclEntryOptions
//                        .builder()
//                        .traffic(AclTraffic.allTraffic())
//                        .cidr(AclCidr.anyIpv4())
//                        .direction(TrafficDirection.EGRESS)
//                        .ruleAction(Action.ALLOW)
//                        .ruleNumber(1)
//                        .build()
//        );
    }

}