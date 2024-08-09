package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class AuthServiceApp {

    public static void main(String[] args) {
        App app = new App();

        new AuthServiceStack(app, "AuthServiceStack", StackProps.builder().build());

        app.synth();
    }
}
