package net.markz.awscdkstack.services;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import net.markz.awscdkstack.iam.IAM;
import net.markz.awscdkstack.services.portfoliofrontend.PortfolioFrontend;
import net.markz.awscdkstack.services.webscraperservice.WebscraperService;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

@AllArgsConstructor
@Builder
public class ServiceProvider {



    public void buildWebscraperService(final Construct scope, final String id, final StackProps props) {
        WebscraperService
                .builder()
                .scope(scope)
                .id(id)
                .props(props)
                .build();
    }

    public void buildPortfolioFrontend(final Construct scope, final String id, final StackProps props) {
        PortfolioFrontend
                .builder()
                .scope(scope)
                .id(id)
                .props(props)
                .build();
    }

    public void buildIAMStuff(final Construct scope, final String id, final StackProps props) {
        IAM
                .builder()
                .scope(scope)
                .id(id)
                .props(props)
                .build();
    }
}
