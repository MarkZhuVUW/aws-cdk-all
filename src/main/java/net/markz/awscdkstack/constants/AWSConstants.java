package net.markz.awscdkstack.constants;

public enum AWSConstants {
    MY_PUBLIC_SSL_CERTIFICATE_ARN_US_EAST_1("arn:aws:acm:us-east-1:142621353074:certificate/8cc5f751-ea0f-4be4-b839-65239c59d7e3"),
    MY_PUBLIC_SSL_CERTIFICATE_ARN_AP_SOUTHEAST_2("arn:aws:acm:ap-southeast-2:142621353074:certificate/023f438e-5cd1-4e16-b34c-1eb8ef5dfcda"),
    ACCOUNT("142621353074"),
    REGION("ap-southeast-2"),
    ECS_CLUSTER("MarkZECSCluster"),
    ECS_SERVICE("WebscraperService"),
    ECS_PRINCIPAL("ecs.amazonaws.com"),
    S3_PRINCIPAL("s3.amazonaws.com"),
    ASSUME_ROLE_PRACTICE_USER_NAME("AssumeRolePracticeUser"),
    ASSUME_ROLE_PRACTICE_USER_ARN("arn:aws:iam::142621353074:user/AssumeRolePracticeUser"),
    ASG_DEBUG_SG_NAME("ASG_DEBUG_SG"),
    ALB_SG_NAME("ALB_SG_NAME"),
    MARKZVPC_PUBLICSUBNET_NACL_NAME("MARKZVPC_PUBLICSUBNET_NACL");


    private final String str;

    AWSConstants(final String str) {
        this.str = str;
    }

    public String getStr() {
        return str;
    }

}
