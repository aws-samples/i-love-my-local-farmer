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

package com.ilmlf.delivery;

import com.ilmlf.delivery.api.ApiStack;
import com.ilmlf.delivery.db.DbStack;
import java.io.IOException;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.StackProps;

public class DeliveryApp {
  public static void main(final String[] args) throws IOException {
    App app = new App();

    DbStack db = new DbStack(app, "DeliveryProject-Db", StackProps.builder()
        .build());

    new ApiStack(
        app,
        "DeliveryProject-Api",
        ApiStack.ApiStackProps.builder()
            .dbEndpoint(db.getInstanceEndpoint())
            .dbProxyEndpoint(db.getProxyEndpoint())
            .dbProxyArn(db.getProxyArn())
            .dbRegion(db.getRegion())
            .dbUser(db.getUser())
            .dbUserSecretName(db.getUserSecret().getSecretName())
            .dbUserSecretArn(db.getUserSecret().getSecretArn())
            .dbAdminSecretName(db.getAdminSecret().getSecretName())
            .dbAdminSecretArn(db.getAdminSecret().getSecretArn())
            .dbSg(db.getSecurityGroup())
            .dbVpc(db.getVpc())
            .build());

    app.synth();
  }
}
