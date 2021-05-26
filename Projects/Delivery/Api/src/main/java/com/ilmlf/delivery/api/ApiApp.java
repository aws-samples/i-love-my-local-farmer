package com.ilmlf.delivery.api;

import java.io.IOException;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;

public class ApiApp {
  public static void main(final String[] args) throws IOException {
    App app = new App();

    new ApiStack(
        app,
        "ApiStack",
        ApiStack.ApiStackProps.builder()
            .env(Environment.builder()
                .account("433621526002")
                .region("eu-west-1")
                .build())
            .dbEndpoint("farmerdb.cyo3qc04zrsp.eu-west-1.rds.amazonaws.com:3306")
            .dbProxyEndpoint(
                "farmerdbproxy.proxy-cyo3qc04zrsp.eu-west-1.rds.amazonaws.com:3306/FarmerDB")
            .dbProxyArn("arn:aws:rds:eu-west-1:433621526002:db-proxy:prx-0a792ad6c6e7aef99")
            .dbRegion("eu-west-1")
            .dbUser("lambda_iam")
            .dbUserSecretName("DbUserSecret")
            .dbUserSecretArn("arn:aws:secretsmanager:eu-west-1:433621526002:secret:DbUserSecret-CeVI0L")
            .dbAdminSecretName("DeliveryProjectDbFarmerDBSe-2OOKfJWB3Ue2")
            .dbAdminSecretArn(
                "arn:aws:secretsmanager:eu-west-1:433621526002:secret:DeliveryProjectDbFarmerDBSe-2OOKfJWB3Ue2-mRuwvG")
            .dbSgId("sg-09e6e9b4696bb2a53")
            .dbVpcId("vpc-0505198ab2080d218")
            .build());

    app.synth();
  }
}
