package net.markz.awscdkstack;

import net.markz.awscdkstack.constants.AWSConstants;
import net.markz.awscdkstack.services.ServiceProvider;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class AwsCliStacksApp {
    public static void main(final String[] args) {
        final App app = new App();

        final StackProps stackProps = StackProps.builder()
                // If you don't specify 'env', this stack will be environment-agnostic.
                // Account/Region-dependent features and context lookups will not work,
                // but a single synthesized template can be deployed anywhere.

                // Uncomment the next block to specialize this stack for the AWS Account
                // and Region that are implied by the current CLI configuration.

//                .env(Environment.builder()
//                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
//                        .region(System.getenv("CDK_DEFAULT_REGION"))
//                        .build())
                // Uncomment the next block if you know exactly what Account and Region you
                // want to deploy the stack to.

                .env(Environment.builder()
                        .account(AWSConstants.ACCOUNT.getStr())
                        .region(AWSConstants.REGION.getStr())
                        .build())
                // For more information, see https://docs.aws.amazon.com/cdk/latest/guide/environments.html
                .build();

        // Services
        ServiceProvider.buildWebscraperService(app, "WebscraperService-ALBECSEC2", stackProps);
        ServiceProvider.buildPortfolioFrontend(app, "Portfolio-Frontend", stackProps);
        ServiceProvider.buildIAMStuff(app, "IAM-stuff", stackProps);

        app.synth();
    }
}






































