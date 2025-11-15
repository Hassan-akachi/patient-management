package com.example.stack;

// Imports necessary classes from the AWS CDK library
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awscdk.App;
import software.amazon.awscdk.AppProps;
import software.amazon.awscdk.BootstraplessSynthesizer;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Token;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.CloudMapNamespaceOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.route53.CfnHealthCheck;


// This class defines a simple AWS CDK stack for LocalStack (local AWS emulator)
public class LocalStack extends Stack {

    // Declare a private final field to hold the VPC object
    private final Vpc vpc;

    // Declare a private final field to hold the ECS Cluster object
    private final Cluster ecsCluster;

    // Constructor for the stack
    public LocalStack(final App scope, final String id, final StackProps props) {
        // Calls the parent Stack constructor, passing scope, ID, and properties
        super(scope, id, props);

        // Call the private method to create a VPC and store it
        this.vpc = createVpc();

        // Create the RDS database instance for the Authentication Service
        DatabaseInstance authServiceDb =
                createDatabase("AuthServiceDB","auth-service-db");

        // Create the RDS database instance for the Patient Service
        DatabaseInstance patientServiceDb =
                createDatabase("PatientServiceDB","patient-service-db");

        // Create a Route53 Health Check for the Auth Service DB
        CfnHealthCheck authDbHealthCheck =
                createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");

        // Create a Route53 Health Check for the Patient Service DB
        CfnHealthCheck patientDbHealthCheck =
                createDbHealthCheck(patientServiceDb, "PatientServiceDBHealthCheck");

        // Create the MSK (Kafka) cluster resource
        CfnCluster mskCluster = createMskCluster();

        // Create the ECS cluster where all services will run
        this.ecsCluster = createEcsCluster();

        // Deploy the Authentication Fargate Service
        FargateService authService =
                createFargateService("AuthService",
                        "auth-service",
                        List.of(4005), // Container port
                        authServiceDb, // Database dependency
                        // Additional environment variables (e.g., JWT secret key)
                        Map.of("JWT_SECRET","Y2V0T0o1Y2d6dFp5Tkt1U2l0ZzVvRk1mZldEcFpaY1E="));

        // Ensure the service deployment waits for the DB and Health Check to complete
        authService.getNode().addDependency(authDbHealthCheck);
        authService.getNode().addDependency(authServiceDb);

        // Deploy the Billing Fargate Service
        FargateService billingService =
                createFargateService("BillingService",
                        "billing-service",
                        List.of(4001, 9001), // REST and gRPC ports
                        null,
                        null);

        // Deploy the Analytics Fargate Service
        FargateService analyticsService =
                createFargateService("AnalyticsService",
                        "analytics-service",
                        List.of(4002),
                        null,
                        null);

        // Ensure the Analytics Service waits for the MSK Cluster
        analyticsService.getNode().addDependency(mskCluster);

        // Deploy the Patient Fargate Service
        FargateService patientService = createFargateService("PatientService",
                "patient-service",
                List.of(4000),
                patientServiceDb,
                Map.of( // Environment variables for inter-service communication
                        "BILLING_SERVICE_ADDRESS", "host.docker.internal", // LocalStack internal DNS
                        "BILLING_SERVICE_GRPC_PORT", "9001"
                ));

        // Set dependencies for the Patient Service deployment
        patientService.getNode().addDependency(patientServiceDb);
        patientService.getNode().addDependency(patientDbHealthCheck);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(mskCluster);

        // Deploy the API Gateway (Load Balanced Fargate Service)
        createApiGatewayService();
    }

    // Creates a Route53 Health Check resource pointing to the database endpoint
    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db,String id) {

        return CfnHealthCheck.Builder.create(this,id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP") // Check connectivity over TCP
                        .port(Token.asNumber(db.getDbInstanceEndpointPort())) // DB port
                        .ipAddress(db.getDbInstanceEndpointAddress()) // DB address
                        .requestInterval(30) // Check every 30 seconds
                        .failureThreshold(3) // 3 failures required to mark as unhealthy
                        .build())
                .build();
    }

    // Creates an MSK (Kafka) Cluster
    private CfnCluster createMskCluster(){
        return CfnCluster.Builder.create(this, "MskCluster")
                .clusterName("kafa-cluster")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.m5.xlarge")
                        // Map private subnets of the VPC to the broker nodes
                        .clientSubnets(vpc.getPrivateSubnets().stream()
                                .map(ISubnet::getSubnetId)
                                .collect(Collectors.toList()))
                        .brokerAzDistribution("DEFAULT")
                        .build())
                .build();
    }

    // Creates the ECS Cluster with a default Cloud Map namespace for service discovery
    private Cluster createEcsCluster(){

        return Cluster.Builder.create(this,"PatientManagementCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(
                        CloudMapNamespaceOptions
                                .builder()
                                // Services will be discoverable under e.g., auth-service.patient-management.local
                                .name("patient-management.local")
                                .build())
                .build();
    }

    // Creates a generic Fargate Service for a microservice
    private FargateService createFargateService(String id, String imageName, List<Integer> ports, DatabaseInstance db, Map<String, String> additionalEnvVars) {

        // Define the Task Definition (CPU/Memory requirements)
        FargateTaskDefinition taskDefinition=FargateTaskDefinition.Builder.create(this,id +"Task")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        // Define Container Configuration
        ContainerDefinitionOptions.Builder containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry(imageName)) // Image name (e.g., auth-service)
                        // Map container ports to host ports
                        .portMappings(ports.stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())
                        // Configure AWS CloudWatch logging
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
                                        .logGroupName("/ecs/" + imageName)
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix(imageName)
                                .build()));

        // Set up Environment Variables
        Map<String,String> envVars = new HashMap<>();
        // Default Kafka bootstrap servers (LocalStack-specific endpoint)
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS","localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");

        // Merge any provided additional environment variables
        if(additionalEnvVars != null) {
            envVars.putAll(additionalEnvVars);
        }

        // Configure Database connection variables if a DB is provided
        if(db != null){
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s-db".formatted(
                    db.getDbInstanceEndpointAddress(),
                    db.getDbInstanceEndpointPort(),
                    imageName
            ));
            envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");
            // Retrieve password securely from the generated secret
            envVars.put("SPRING_DATASOURCE_PASSWORD",
                    db.getSecret().secretValueFromJson("password").toString());
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
        }

        containerOptions.environment(envVars);
        taskDefinition.addContainer(imageName + "Container", containerOptions.build());

        // Create the Fargate Service
        return FargateService.Builder.create(this, id)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .serviceName(imageName)
                .build();
    }

    // Creates the API Gateway service using an Application Load Balancer
    private void createApiGatewayService() {
        // Define the Task Definition for the API Gateway
        FargateTaskDefinition taskDefinition =
                FargateTaskDefinition.Builder.create(this, "APIGatewayTaskDefinition")
                        .cpu(256)
                        .memoryLimitMiB(512)
                        .build();

        // Define Container Options for the API Gateway
        ContainerDefinitionOptions containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry("api-gateway"))
                        // Set environment variables for Spring profiles and Auth Service URL
                        .environment(Map.of(
                                "SPRING_PROFILES_ACTIVE", "prod",
                                "AUTH_SERVICE_URL", "http://host.docker.internal:4005"
                        ))
                        // Map port 4004 (API Gateway listener port)
                        .portMappings(List.of(4004).stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())
                        // Configure logging
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                                        .logGroupName("/ecs/api-gateway")
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix("api-gateway")
                                .build()))
                        .build();


        taskDefinition.addContainer("APIGatewayContainer", containerOptions);

        // Create the Load Balanced Fargate Service
        ApplicationLoadBalancedFargateService apiGateway =
                ApplicationLoadBalancedFargateService.Builder.create(this, "APIGatewayService")
                        .cluster(ecsCluster)
                        .serviceName("api-gateway")
                        .taskDefinition(taskDefinition)
                        .desiredCount(1)
                        .healthCheckGracePeriod(Duration.seconds(60)) // Allow time for service startup
                        .build();
    }

    // Main method to run the CDK application
    public static void main(final String[] args) {

        // Create a new CDK app instance and set the output directory for the templates
        App app = new App(AppProps.builder().outdir("./cdk.out").build());

        // Define stack properties to disable AWS resource bootstrapping
        StackProps props = StackProps.builder()
                // Use BootstraplessSynthesizer to deploy to LocalStack or environments without prior AWS bootstrap
                .synthesizer(new BootstraplessSynthesizer())
                .build();

        // Create a new instance of the LocalStack stack definition
        new LocalStack(app, "localstack", props);

        // Instruct the CDK application to generate the CloudFormation template(s)
        app.synth();

        // Print message to indicate the synthesizing process is complete
        System.out.println("App synthesizing in progress......");
    }

    // Private method to define and provision a new VPC
    private Vpc createVpc() {

        return Vpc.Builder
                // Start building the VPC with a unique logical ID
                .create(this, "PatientManagementVPC")
                // Set the physical name of the VPC in AWS (optional but helpful)
                .vpcName("PatientManagementVPC")
                // Distribute resources across 2 Availability Zones (AZs)
                .maxAzs(1)
                // Finalize the VPC creation
                .build();
    }

    private DatabaseInstance createDatabase(String id, String dbName) {
        // Defines a private method to create and return a PostgreSQL DatabaseInstance
        return DatabaseInstance.Builder
                // Starts building the RDS DatabaseInstance resource.
                // 'this' refers to the CDK Stack, and 'id' is the unique logical ID for the resource.
                .create(this, id)
                // Specifies the database engine configuration
                .engine(DatabaseInstanceEngine.postgres(
                        // Starts building the properties specific to the PostgreSQL engine
                        PostgresInstanceEngineProps.builder()
                                // Sets the exact version of PostgreSQL to be provisioned (Version 17.2)
                                .version(PostgresEngineVersion.VER_17_2)
                                .build()
                ))
                // Place the database inside the defined VPC
                .vpc(vpc)
                // Use a t2.micro instance type (suitable for LocalStack and testing)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                // Allocate 20 GB of storage
                .allocatedStorage(20)
                // Generate a random password and use 'admin_user' as the master username
                .credentials(Credentials.fromGeneratedSecret("admin_user"))
                // Set the initial database name
                .databaseName(dbName)
                // Ensure the database is destroyed when the stack is deleted (good for development)
                .removalPolicy(RemovalPolicy.DESTROY)
                // Finalizes the configuration of the DatabaseInstance
                .build();
    }
}