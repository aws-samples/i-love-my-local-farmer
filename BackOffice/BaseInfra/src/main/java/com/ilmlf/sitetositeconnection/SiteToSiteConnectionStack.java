package com.ilmlf.sitetositeconnection;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcProps;
import software.amazon.awscdk.services.ec2.VpnConnectionOptions;

import java.util.Collections;

public class SiteToSiteConnectionStack extends Stack {
    public SiteToSiteConnectionStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public SiteToSiteConnectionStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        String cidr = this.getNode().tryGetContext("vpcCidr").toString();
        String ip = this.getNode().tryGetContext("customerGatewayDeviceIP").toString();
        String onPremiseCidr = this.getNode().tryGetContext("onPremiseCidr").toString();

        VpnConnectionOptions vpcConnectionOption = new VpnConnectionOptions.Builder()
                .ip(ip)
                .staticRoutes(Collections.singletonList(onPremiseCidr))
                .build();

        new Vpc(this, "VPC", VpcProps
                .builder()
                .cidr(cidr)
                .maxAzs(2)
                .vpnConnections(Collections.singletonMap("static", vpcConnectionOption))
                .build()
        );
    }
}
