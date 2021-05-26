package com.ilmlf.delivery;

import com.ilmlf.delivery.api.ApiStack;
import com.ilmlf.delivery.db.DbStack;
import java.io.IOException;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class DeliveryApp {
  public static void main(final String[] args) throws IOException {
    App app = new App();

    DbStack db = new DbStack(app, "DeliveryProject-Db", StackProps.builder()
        .env(Environment.builder()
            .account("433621526002")
            .region("eu-west-1")
            .build())
        .build());

    new ApiStack(
        app,
        "DeliveryProject-Api",
        ApiStack.ApiStackProps.builder()
            .env(Environment.builder()
                .account("433621526002")
                .region("eu-west-1")
                .build())
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
