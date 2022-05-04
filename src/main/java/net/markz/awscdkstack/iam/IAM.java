package net.markz.awscdkstack.iam;

import lombok.AllArgsConstructor;
import net.markz.awscdkstack.constants.AWSConstants;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.AccessKey;
import software.amazon.awscdk.services.iam.ArnPrincipal;
import software.amazon.awscdk.services.iam.CompositePrincipal;
import software.amazon.awscdk.services.iam.Policy;
import software.amazon.awscdk.services.iam.PolicyProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.iam.User;
import software.constructs.Construct;

import java.util.List;

@AllArgsConstructor
public class IAM extends Stack {

    @lombok.Builder
    public IAM(
            final Construct scope,
            final String id,
            final StackProps props
    ) {
        super(scope, id, props);

        final var assumeRolePracticeUser = User.Builder.create(this, "AssumeRolePracticeUser")
                .userName(AWSConstants.ASSUME_ROLE_PRACTICE_USER_NAME.getStr())
                .build();

        // Create an IAM role for updateing ecs service and s3 bucket
        final var updateRole = Role.Builder.create(this, "ECS-S3-Update-Role")
                .roleName("ECSS3UpdateRole")
                .assumedBy(
                        // I need this role to be assumed by the ECS service principal as well as the performing user principal
                        new CompositePrincipal(
                                ServicePrincipal
                                        .Builder
                                        .create(AWSConstants.ECS_PRINCIPAL.getStr())
                                        .build(),
                                new ArnPrincipal(assumeRolePracticeUser.getUserArn()),
                                ServicePrincipal
                                        .Builder
                                        .create(AWSConstants.S3_PRINCIPAL.getStr())
                                        .build()
                        )
                )
                .build();
        // Retain this user when we destroy the stack
        updateRole.applyRemovalPolicy(RemovalPolicy.RETAIN);
        updateRole.attachInlinePolicy(new Policy(this, "ECSS3UpdateRolePolicy",
                PolicyProps
                        .builder()
                        .policyName("ECSS3UpdateRolePolicy")
                        .statements(List.of(
                                PolicyStatement
                                        .Builder
                                        .create()
                                        .actions(List.of(
                                                "logs:CreateLogStream",
                                                "logs:PutLogEvents",
                                                "ecs:UpdateService",
                                                "s3:PutObject",
                                                "s3:PutObjectAcl",
                                                "s3:GetObject",
                                                "s3:GetObjectAcl",
                                                "s3:DeleteObject",
                                                "s3:ListBucket"
                                        ))
                                        .resources(List.of(
                                                "*"
                                        ))
                                        .build()
                        ))
                        .build()
        ));
        // Retain this user when we destroy the stack
        assumeRolePracticeUser.applyRemovalPolicy(RemovalPolicy.RETAIN);
        assumeRolePracticeUser.attachInlinePolicy(
                new Policy(this, "AssumeRoleUserPolicy",
                        PolicyProps
                                .builder()
                                .policyName("AssumeRoleUserPolicy")
                                .statements(List.of(
                                        PolicyStatement
                                                .Builder
                                                .create()
                                                .actions(List.of(
                                                        "sts:AssumeRole"
                                                ))
                                                .resources(List.of(
                                                        updateRole.getRoleArn()
                                                ))
                                                .build()
                                ))
                                .build()
                )
        );
        final var assumeRolePracticeUserAccessKey = AccessKey.Builder.create(this, "AssumeRolePolicy-AccessKey")
                .user(assumeRolePracticeUser)
                .build();

        CfnOutput.Builder.create(this, "AssumeRolePolicy-AccessKeyId-Output")
                .exportName("accessKeyId")
                .value(assumeRolePracticeUserAccessKey.getAccessKeyId())
                .build();

        CfnOutput.Builder.create(this, "AssumeRolePolicy-SecretAccessKey-Output")
                .exportName("secretAccessKey")
                .value(assumeRolePracticeUserAccessKey.getSecretAccessKey().toString())
                .build();
    }
}
