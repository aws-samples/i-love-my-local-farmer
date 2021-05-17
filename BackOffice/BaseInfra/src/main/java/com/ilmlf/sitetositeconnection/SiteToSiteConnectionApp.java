package com.ilmlf.sitetositeconnection;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class SiteToSiteConnectionApp {
    public static void main(final String[] args) {
        App app = new App();

        new SiteToSiteConnectionStack(app, "SiteToSiteBaseInfra", StackProps.builder()
                .env(Environment.builder()
                        .account("433621526002")
                        .region("eu-west-1")
                        .build())
                .build());


        app.synth();
    }
}
