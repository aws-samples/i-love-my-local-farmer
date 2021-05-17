/*
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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
