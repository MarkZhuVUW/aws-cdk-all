package net.markz.awscdkstack.services;

import net.markz.awscdkstack.services.webscraperservice.ECSEC2;
import net.markz.awscdkstack.services.webscraperservice.ECSFargate;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

public record ServiceProvider() {
    public static void startECSEC2WebscraperService(final Construct scope, final String id, final StackProps props) {
        new ECSEC2(scope, id, props);
    }

    public static void startECSFargateWebscraperService(final Construct scope, final String id, final StackProps props) {
        new ECSFargate(scope, id, props);
    }
}
