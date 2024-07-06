package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class ImportServiceApp
{
    public static void main(final String[] args )
    {
        App app = new App();

        new ImportServiceStack(app, "ImportServiceStack", StackProps.builder().build());

        app.synth();
    }
}
