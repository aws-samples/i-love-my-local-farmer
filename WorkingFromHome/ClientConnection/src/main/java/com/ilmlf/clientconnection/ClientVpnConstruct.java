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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ec2.ClientVpnAuthorizationRuleOptions;
import software.amazon.awscdk.services.ec2.ClientVpnEndpoint;
import software.amazon.awscdk.services.ec2.ClientVpnEndpointOptions;
import software.amazon.awscdk.services.ec2.ClientVpnRouteOptions;
import software.amazon.awscdk.services.ec2.ClientVpnRouteTarget;
import software.amazon.awscdk.services.ec2.ClientVpnUserBasedAuthentication;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.IVpc;

/** ClientVpnConstruct. */
public class ClientVpnConstruct extends Construct {

  /** Client VPN required properties. */
  @lombok.Builder
  @Data
  public static class ClientVpnProps {

    private IVpc vpc;
    private String domainName;
    private String secretId;
    private List<String> dnsIps;
  }

  /**
   * Client VPN Construct.
   *
   * @param scope CDK construct scope
   * @param id construct Id
   * @param props ClientVpnProps
   */
  public ClientVpnConstruct(
      software.constructs.@NotNull Construct scope, @NotNull String id, ClientVpnProps props)
      throws IOException {
    super(scope, id);

    /*
      Fetch values from the cdk.context.json (found at the root of ClientConnection folder)
      - clientVpnCertificate is the Amazon Resource Name (ARN) of the Amazon Certificate Manager
        certificate that is used to authenticate connections to the Client VPN.
      - clientVpnCidr is the VPC CIDR range that can be reached through the Client VPN
      - onPremiseCidr is the CIDR range of the on-premise resources
      - DomainAdminSecretArn is the ARN of the Secrets Manager 'DomainAdminPassword' secret
    */
    String serverCertificateArn = this.getNode().tryGetContext("clientVpnCertificate").toString();
    String cidr = this.getNode().tryGetContext("clientVpnCidr").toString();
    String onPremiseCidr = this.getNode().tryGetContext("onPremiseCidr").toString();
    String secretId = this.getNode().tryGetContext("DomainAdminSecretArn").toString();

    /*
      Create the AD Connector using our own CDK Custom Resource.
      This requires that we pass in property values that we have already fetched.
    */
    AdConnectorConstruct adConnector =
        new AdConnectorConstruct(
            this,
            "OnPremiseADConnector",
            AdConnectorConstruct.AdConnectorProps.builder()
                .vpcId(props.vpc.getVpcId())
                .domainName(props.domainName)
                .dnsIps(props.dnsIps)
                .subnetIds(
                    props.vpc.getPrivateSubnets().stream()
                        .map(ISubnet::getSubnetId)
                        .collect(Collectors.toList()))
                .secretId(secretId)
                .build());

    /*
      The Client VPN is created using a method on the VPC object that we passed in as a property.
      We specify the authentication certificate, and that users must authenticate through the
      active directory connection that we created with the AD Connector Construct.
    */
    ClientVpnEndpoint clientVpn =
        props.vpc.addClientVpnEndpoint(
            "VpnClientEndpoint",
            ClientVpnEndpointOptions.builder()
                .cidr(cidr)
                .serverCertificateArn(serverCertificateArn)
                .userBasedAuthentication(
                    ClientVpnUserBasedAuthentication.activeDirectory(adConnector.directoryId))
                .dnsServers(props.dnsIps)
                .splitTunnel(true)
                .build());

    /*
      For each of the private subnets in the VPC, we must add a route between it and the Client VPN.
    */
    props.vpc
        .getPrivateSubnets()
        .forEach(
            (subnet) ->
                clientVpn.addRoute(
                    "onPremiseRoute-" + subnet.getSubnetId(),
                    ClientVpnRouteOptions.builder()
                        .cidr(onPremiseCidr)
                        .target(ClientVpnRouteTarget.subnet(subnet))
                        .build()));
    clientVpn.addAuthorizationRule(
        "onPremise", ClientVpnAuthorizationRuleOptions.builder().cidr(onPremiseCidr).build());
  }
}
