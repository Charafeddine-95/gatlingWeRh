Gatling plugin for Maven - Exported Gatling Studio Simulation
============================================

Welcome, this is your exported project. It is ready to be used on Gatling Enterprise as a Java packaged simulation. 
It has been configured for you using our [Configuration as Code](https://docs.gatling.io/reference/execute/cloud/user/configuration-as-code/) feature.

This documentation helps you upload your simulation to Gatling Enterprise. Although you most likely have changed the simulation in your local environment, this procedure works even if you are just testing the export code feature without making any code changes.  
The following procedure allows you to deploy your exported simulation to Gatling Enterprise:

## Create an API Token 

Here is the link to our documentation about [API tokens configuration](https://docs.gatling.io/reference/execute/cloud/admin/api-tokens/).  

1. Go to https://cloud.gatling.io/ and click on the API tokens icon.

2. Create a new API Token with `Configure` permission on the organization, or at least the team you used when building your simulation.

3. Copy the generated token to your clipboard.

## Set your token as an environment variable. 

Follow the instructions to set up the [Maven Plugin prerequisites](https://docs.gatling.io/reference/integrations/build-tools/maven-plugin/#prerequisites) and configure your API Token as a `GATLING_ENTERPRISE_API_TOKEN` environment variable.

## Deploy your simulation using the maven plugin

Now, to deploy this project into Gatling Enterprise Cloud, you can run:

```console
./mvnw gatling:enterpriseDeploy
```

Find more details explaining how to use the gatling-maven-plugin in our [dedicated documentation page](https://docs.gatling.io/reference/integrations/build-tools/maven-plugin/).
