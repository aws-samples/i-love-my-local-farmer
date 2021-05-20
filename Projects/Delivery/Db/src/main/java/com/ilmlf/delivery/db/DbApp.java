package com.ilmlf.delivery.db;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.StackProps;

public class DbApp {
  public static void main(final String[] args) {
    App app = new App();

    new DbStack(app, "DeliveryProject-Db", StackProps.builder().build());

    app.synth();
  }
}
