package com.ilmlf.product.db.schemaManager;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.BundlingOptions;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.IConstruct;
import software.amazon.awscdk.customresources.*;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.s3.assets.Asset;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.ISource;
import software.amazon.awscdk.services.s3.deployment.Source;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A CDK construct that runs flyway migration scripts against a redshift cluster.
 * <p>
 * This construct is based on two main resource, an AWS Lambda hosting a flyway runner
 * and one custom resource invoking it when content of migrationScriptsFolderAbsolutePath changes.
 * </p>
 */
public class FlywayRunner extends Construct {
    /**
     * Properties needed to run flyway migration scripts.
     */
    @lombok.Builder
    @Data
    public static class FlywayRunnerProps {
        /**
         * The absolute path to the flyway migration scripts.
         * Those scripts needs to follow expected flyway naming convention.
         *
         * @see <a href="https://flywaydb.org/documentation/concepts/migrations.html#sql-based-migrations">for more details.</a>
         */
        private String migrationScriptsFolderAbsolutePath;

        /**
         * The cluster to run migration scripts against.
         */
//    private rds cluster: redshift.Cluster;

        /**
         * The vpc hosting the cluster.
         */
        private Vpc vpc;

        /**
         * The database name to run migration scripts against.
         */
        private String databaseName;

        /**
         * The database instance to connect to
         */
        private DatabaseInstance databaseInstance;

        /**
         * Period to keep the logs around.
         *
         * @default logs.RetentionDays.ONE_DAY (1 day)
         */
        private RetentionDays logRetention;
    }

    /**
     * Constructs a new pipeline stack.
     *
     * @param scope
     * @param id
     * @param options specify the stages environment details (account and region) and pipeline environement
     * @throws Exception thrown in case of war build failure
     */
    public FlywayRunner(Construct scope, String id, FlywayRunnerProps options) {
        super(scope, id);

        @NotNull ISource sqlFilesAsset = Source.asset(options.migrationScriptsFolderAbsolutePath);

        Bucket migrationFilesBucket = new Bucket(this, "MigrationFilesBucket");
        BucketDeployment migrationFilesDeployment = BucketDeployment.Builder.create(this, "DeploySQLMigrationFiles").sources(List.of(sqlFilesAsset)).destinationBucket(migrationFilesBucket).build();

        /*
         * Command for building Java handler inside a container
         */
        List<String> packagingInstructions =
                Arrays.asList(
                        "/bin/sh", "-c",
                        "./gradlew shadowJar -x test" + "&& cp build/libs/flyway-all.jar /asset-output/"
                );


        BundlingOptions builderOptions =
                BundlingOptions.builder()
                        .command(packagingInstructions)
                        .image(Runtime.JAVA_11.getBundlingImage())
                        .build();

        Function flywayServiceLambda = new Function(
                this,
                "runner",
                FunctionProps.builder()
                        .environment(
                                Map.of(
                                        "S3_BUCKET", migrationFilesBucket.getBucketName(),
                                        "DB_CONNECTION_STRING", String.format("jdbc:mysql://%s/%s", options.databaseInstance.getDbInstanceEndpointAddress(), options.getDatabaseName()),
                                        "DB_SECRET", options.databaseInstance.getSecret().getSecretFullArn()))
                        .runtime(Runtime.JAVA_11)
                        .code(
                                Code.fromAsset(
                                        "../../FlywayLambdaService/",
                                        AssetOptions.builder()
                                                .bundling(builderOptions)
                                                .build()))
                        .timeout(Duration.minutes(15))
                        .memorySize(2048)
                        .handler("com.geekoosh.flyway.FlywayHandler::handleRequest")
                        .vpc(options.vpc)
                        .securityGroups(options.databaseInstance.getConnections().getSecurityGroups())
                        .build());

        // Let Flyway Lambda get DB creds
        options.databaseInstance.getSecret().grantRead(flywayServiceLambda);

        // Let Flyway Lambda access the DB
        options.databaseInstance.getConnections().allowDefaultPortInternally();

        // Let Flyway Lambda access the migration files bucket
        migrationFilesBucket.grantRead(flywayServiceLambda);

        // Leverage AwsCustomResource to trigger the FlywayLambdaService on asset change
        new AwsCustomResource(this, "trigger", AwsCustomResourceProps.builder()
                .logRetention(options.logRetention != null ? options.logRetention : RetentionDays.ONE_DAY)
                .onUpdate(AwsSdkCall.builder()
                        .service("Lambda")
                        .action("invoke")
                        .physicalResourceId(PhysicalResourceId.of("flywayTrigger"))
                        .parameters(Map.of(
                                "FunctionName", flywayServiceLambda.getFunctionName(),
                                "InvocationType", "RequestResponse",
                                "Payload", "{\"flywayRequest\":{\"flywayMethod\": \"migrate\"}, \"assetHash\": \"" + ((Asset) migrationFilesDeployment.getNode().findChild("Asset1")).getAssetHash()+ "\"}"
                        )).build())
                .policy(AwsCustomResourcePolicy.fromStatements(List.of(PolicyStatement.Builder.create()
                        .actions(List.of("lambda:InvokeFunction"))
                        .resources(List.of(flywayServiceLambda.getFunctionArn()))
                        .build())))
                .build());
    }
}

