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
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;


public class ClientConnectionStack extends Stack {

  /** VPN Client Connection Stack.
   * @param scope scope
   * @param id id
   * @param props props
   */
  public ClientConnectionStack(final Construct scope, final String id, final StackProps props)
      throws IOException {
    super(scope, id, props);

    /*
      Fetch values from the cdk.context.json (found at the root of ClientConnection folder)
      - vpcId is the identifier of the AWS VPC in which to create the Client VPN and AD Connector
      - domain is the domain name for the Active Directory
      - dnsIps is the list of IP addresses of DNS hosts of our on-premise infrastructure
    */
    String vpcId = this.getNode().tryGetContext("vpcId").toString();
    String domainName = this.getNode().tryGetContext("domain").toString();
    Object dnsIps = this.getNode().tryGetContext("dns");

    /*
      We initialise a VPC object using the passed-in VPC identifier.
      This will allow us to perform CDK operations on it easier in the next steps.
    */
    VpcLookupOptions vpcLookupOptions = new VpcLookupOptions.Builder()
        .vpcId(vpcId)
        .build();
    IVpc vpc = Vpc.fromLookup(this, "Vpc", vpcLookupOptions);

    /*
      Create a Client VPN setup using our own defined CDK construct (defined in ./ClientVpnConstruct.java)
      We pass as props the values we fetched from cdk.context.json previously.
    */
    new ClientVpnConstruct(this, "ClientVpn", ClientVpnConstruct.ClientVpnProps.builder()
        .vpc(vpc)
        .domainName(domainName)
        .dnsIps((List<String>) dnsIps)
        .build());

  }
}
