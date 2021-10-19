#Provman Spring MVC Hibernate Mysql Integration 

This application is a fully functional Provider Management (Provman) service that tracks providers and products for the farms
that partner with our service.

It allows us to update the providers listed in our store application as well as update their stock.

## Configuration

This application was migrated from our on-premise environment into a containerized version that runs on AWS. It uses Spring
MVC and Hibernate to provide an API and perform database operations.

Because we have migrated to AWS, certain configurations are read from environment variables injected into the container
during deployment of the corresponding infrastructure. The rest of the configuration can be found in `src/main/resources/jdbc.properties`

## Deployment

This application can be deployed using the corresponding CDK code in the `cdk` directory