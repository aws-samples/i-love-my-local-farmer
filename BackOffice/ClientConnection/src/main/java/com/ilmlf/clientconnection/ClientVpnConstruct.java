package com.ilmlf.clientconnection;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ec2.*;

import java.util.List;
import java.util.stream.Collectors;

public class ClientVpnConstruct extends Construct {

    @lombok.Builder
    @Data
    public static class ClientVpnProps {

        private IVpc vpc;
        private String domainName;
        private String secretId;
        private List<String> dnsIps;
    }

    public ClientVpnConstruct(software.constructs.@NotNull Construct scope, @NotNull String id, ClientVpnProps props) {
        super(scope, id);

        String serverCertificateArn = this.getNode().tryGetContext("clientVpnCertificate").toString();
        String cidr = this.getNode().tryGetContext("clientVpnCidr").toString();
        String onPremiseCidr = this.getNode().tryGetContext("onPremiseCidr").toString();
        String secretId = this.getNode().tryGetContext("DomainAdminSecretArn").toString();

        AdConnectorConstruct adConnector = new AdConnectorConstruct(this, "OnPremiseADConnector",
                 AdConnectorConstruct.AdConnectorProps.builder()
                .vpcId(props.vpc.getVpcId())
                .domainName(props.domainName)
                .dnsIps(props.dnsIps)
                .subnetIds(props.vpc.getPrivateSubnets().stream()
                        .map(ISubnet::getSubnetId)
                        .collect(Collectors.toList()))
                .secretId(secretId)
                .build()
        );

        ClientVpnEndpoint clientVpn = props.vpc.addClientVpnEndpoint("VpnClientEndpoint", ClientVpnEndpointOptions.builder()
                .cidr(cidr)
                .serverCertificateArn(serverCertificateArn)
                .userBasedAuthentication(ClientVpnUserBasedAuthentication.activeDirectory(adConnector.directoryId))
                .dnsServers(props.dnsIps)
                .splitTunnel(true)
                .build());


            props.vpc.getPrivateSubnets().forEach((subnet) ->
            clientVpn.addRoute("onPremiseRoute-" + subnet.getSubnetId(), ClientVpnRouteOptions.builder()
                    .cidr(onPremiseCidr)
                    .target(ClientVpnRouteTarget.subnet(subnet))
                    .build())
        );
        clientVpn.addAuthorizationRule("onPremise", ClientVpnAuthorizationRuleOptions.builder().cidr(onPremiseCidr).build());
    }
}
