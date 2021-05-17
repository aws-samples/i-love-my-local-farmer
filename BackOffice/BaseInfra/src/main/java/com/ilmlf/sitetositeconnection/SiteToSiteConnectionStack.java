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
