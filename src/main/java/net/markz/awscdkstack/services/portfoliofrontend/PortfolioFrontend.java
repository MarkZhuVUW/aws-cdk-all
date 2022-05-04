package net.markz.awscdkstack.services.portfoliofrontend;


import lombok.AllArgsConstructor;
import lombok.Builder;
import net.markz.awscdkstack.constants.AWSConstants;
import net.markz.awscdkstack.constants.NetworkConstants;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.cloudfront.Behavior;
import software.amazon.awscdk.services.cloudfront.CfnDistribution;
import software.amazon.awscdk.services.cloudfront.CloudFrontWebDistribution;
import software.amazon.awscdk.services.cloudfront.OriginAccessIdentity;
import software.amazon.awscdk.services.cloudfront.PriceClass;
import software.amazon.awscdk.services.cloudfront.S3OriginConfig;
import software.amazon.awscdk.services.cloudfront.SourceConfiguration;
import software.amazon.awscdk.services.cloudfront.ViewerCertificate;
import software.amazon.awscdk.services.cloudfront.ViewerCertificateOptions;
import software.amazon.awscdk.services.cloudfront.ViewerProtocolPolicy;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketPolicy;
import software.constructs.Construct;

import java.util.List;


@AllArgsConstructor
public class PortfolioFrontend extends Stack {

    @lombok.Builder
    public PortfolioFrontend(
            final Construct scope,
            final String id,
            final StackProps props
    ) {
        super(scope, id, props);


        final var siteBucket = Bucket
                .Builder
                .create(this, "Portfolio-Frontend-SiteS3-markz-portfolio.uk")
                .bucketName(NetworkConstants.PORTFOLIO_DOMAIN_NAME.getStr())
                .publicReadAccess(false)
                .removalPolicy(RemovalPolicy.DESTROY)
                .websiteIndexDocument("index.html")
                .websiteErrorDocument("index.html")
                .build();

        final var siteBucketPolicy = BucketPolicy
                .Builder
                .create(this, "BucketPolicy-markz-portfolio.uk")
                .bucket(siteBucket)
                .build();

        final var oai =  OriginAccessIdentity
                .Builder
                .create(this, "OAI-CFDistribution-markz-portfolio.uk")
                .comment("access-identity-markz-portfolio.uk.s3.ap-southeast-2.amazonaws.com")
                .build();

        siteBucketPolicy.getDocument().addStatements(PolicyStatement
                .Builder
                .create()
                .principals(List.of(
//                        new ArnPrincipal(AWSConstants.ASSUME_ROLE_PRACTICE_USER_ARN.getStr()), // allow cloudfront to get object in site bucket.
                        oai.getGrantPrincipal()
                ))
                .effect(Effect.ALLOW)
                .actions(List.of(
                        "s3:GetObject"
                ))
                .resources(List.of(
                        siteBucket.getBucketArn() + "/*"
                ))
                .build()
        );
        final var viewerCertificate = ViewerCertificate.fromAcmCertificate(
                Certificate.fromCertificateArn(this, "Certificate", AWSConstants.MY_PUBLIC_SSL_CERTIFICATE_ARN_US_EAST_1.getStr()),
                ViewerCertificateOptions
                        .builder()
                        .aliases(List.of(NetworkConstants.PORTFOLIO_DOMAIN_NAME.getStr()))
                        .build()
        );

        final var cloudFrontDistribution = CloudFrontWebDistribution
                .Builder
                .create(this, "Portfolio-Frontend-CFDistribution-markz-portfolio.uk")
                .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                .originConfigs(List.of(
                        SourceConfiguration
                                .builder()
                                .s3OriginSource(
                                        S3OriginConfig
                                                .builder()
                                                .s3BucketSource(siteBucket)
                                                .originAccessIdentity(
                                                       oai
                                                )
                                                .originShieldRegion(AWSConstants.REGION.getStr())
                                                .build()
                                )

                                .behaviors(List.of(
                                        Behavior
                                                .builder()
                                                .isDefaultBehavior(true)
                                                .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                                                .build()
                                ))

                                .build()
                ))
                .viewerCertificate(viewerCertificate)
                .priceClass(PriceClass.PRICE_CLASS_200)
                .errorConfigurations(List.of(
                        CfnDistribution
                                .CustomErrorResponseProperty
                                .builder()
                                .errorCode(404)
                                .responseCode(200)
                                .responsePagePath("/index.html")
                                .build(),
                        CfnDistribution
                                .CustomErrorResponseProperty
                                .builder()
                                .errorCode(403)
                                .responseCode(200)
                                .responsePagePath("/index.html")
                                .build()
                ))
                .build();

    }

}
