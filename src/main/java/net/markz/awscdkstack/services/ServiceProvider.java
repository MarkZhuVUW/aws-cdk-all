package net.markz.awscdkstack.services;

import net.markz.awscdkstack.services.webscraperservice.ALBECSEC2;
import net.markz.awscdkstack.services.webscraperservice.LambdaAPIGateway;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

public record ServiceProvider() {
    public static void startALBECSEC2WebscraperService(final Construct scope, final String id, final StackProps props) {
        new ALBECSEC2(scope, id, props);
    }

    public static void startLambdaAPIGatewayWebscraperService(final Construct scope, final String id, final StackProps props) {
        new LambdaAPIGateway(scope, id, props);
    }
}
