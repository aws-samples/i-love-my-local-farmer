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

    /*
      Fetch values from the cdk.context.json (found at the root of SiteToSiteConnection folder)
      - vpcCidr is the CIDR range for our AWS VPC that we will create as part of this stack
      - customerGatewayDeviceIP  is the IP address of our on-premise VPN device
      - onPremiseCidr is the CIDR range for our on-premise network
    */
    String vpcCidr = this.getNode().tryGetContext("vpcCidr").toString();
    String customerGatewayDeviceIP = this.getNode().tryGetContext("customerGatewayDeviceIP").toString();
    String onPremiseCidr = this.getNode().tryGetContext("onPremiseCidr").toString();

    /*
      Specify the options for our Site To Site VPN connection:
      - the IP of the on-premise VPN device
      - static routing between the AWS VPC and the on-premise network
    */
    VpnConnectionOptions vpcConnectionOption = new VpnConnectionOptions.Builder()
        .ip(customerGatewayDeviceIP)
        .staticRoutes(Collections.singletonList(onPremiseCidr))
        .build();

    /*
      Create the AWS VPC that the Site-to-Site VPN will be associated with
      Requires the specification of:
      - the CIDR range for the VPC
      - the number of availabilty zones
      - the static VPN connection, using the options we specified previously
    */
    new Vpc(this, "VPC", VpcProps
        .builder()
        .cidr(vpcCidr)
        .maxAzs(2)
        .vpnConnections(Collections.singletonMap("static", vpcConnectionOption))
        .build()
    );
  }
}
