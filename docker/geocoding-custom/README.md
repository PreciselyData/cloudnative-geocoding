# Custom Geocoding for Docker Sample

A sample geocoding application built using the *Spectrum Operational Addressing SDK* and Spring Boot, and packaged as a container using Docker.  This sample demonstrates a simple REST service endpoint that takes a partial address and uses the Addressing Predict API to find matching candidates.

## Prerequisites
### Install the client tools
This sample requires Docker Engine to build the image. 
   * [Docker Engine](https://docs.docker.com/engine/install/)     

### Download the SDK & data
   * Spectrum Operational Addressing SDK (OAS) distribution - For information about OAS, see the [Precisely Support](https://support.precisely.com/) site and the [Operational Addressing SDK Developer Guide](https://docs.precisely.com/docs/sftw/ggs/5.0/en/webhelp/index.html#GlobalGeocodingGuide/source/LandingPageForHelp_GGS.html) documentation.
   * Geocoding reference data in `.spd` format -  For information about Precisely's data portfolio, see the [Precisely Data Guide](https://dataguide.precisely.com/) where you can also sign up for a free account and access sample data available in [Precisely Data Experience](https://data.precisely.com/).

## Setup instructions
1. If you haven't already, clone or download this repository to your computer.
2.  Place your OAS distribution into the `{project}/lib` directory and execute:
        
        gradlew extractGGSDist

    This will unzip your OAS distribution into the `{project}/src/main/jib/var/lib/ggs` directory.
    * Everything under `{project}/src/main/jib` will be included in the Docker image.
3. Place your Interactive geocoding dataset, in SPD format, into the `{project}/data` directory.
    * The `{project}/data` directory will not be included in the Docker image, and will need to be provided to the container at runtime.

## Build the sample application
The sample is bundled with a gradle build script to help build, test, and package the project. There are several tasks associated with local development that can be helpful:
* Unit tests can be executed using: `gradlew test`
* The web service can be executed locally using: `gradlew bootRun` after which the service can be accessed locally with a URL, such as: http://localhost:8080/addressing/predict/usa?input=350%20jordan 
    

  Your service is ready to access when you see the following console message after executing `gradlew bootRun`:
  ```
  2020-09-11 10:22:11.312  INFO 15192 --- [main] com.pb.bigdata.sample.Application: Started Application in 33.317 seconds (JVM running for 33.649)
  ```  
### Build the Docker image
The Gradle build uses the Jib plugin to create a Docker image for the Spring Boot service.

Create and deploy the image to a local Docker daemon using this command:
```
gradlew jibDockerBuild
```
* If you want to deploy the Docker image to a remote repository, there are additional configuration requirements that need to be set; for details, refer to the Jib's [documentation](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin#Configuration):

**Note:** If you noticed the error below, then you are hitting `gradle-docker-plugin`'s known [issue](https://github.com/bmuschko/gradle-docker-plugin/issues/769) for the Windows platform. For a workaround, you need to enable the `"Expose daemon on tcp://localhost:2375 without TLS"` option in the `"Docker Desktop"` settings.

  > “Error during callback
com.bmuschko.gradle.docker.shaded.org.apache.http.conn.HttpHostConnectException: Connect to 127.0.0.1:2375 [/127.0.0.1] failed: Connection refused: connect”.
    
### Start the container
Use the run command below to start the image on a local Docker daemon. This docker command consists of:
* Setting the port forwarding for the Spring Boot web service.
* Mounting a local directory containing the data for geocoding to the expected location (`/mnt/ggs_data`).

The memory requirement may need to be adjusted as datasets are added to the geocoder. 

A general recommendation for minimum heap size is:
   - 1-2 datasets - 2 GB
   - 3 or more datasets - 8 GB 
``` 
docker run -p 8080:8080 -v c:\geocoding-samples\docker\geocoding-custom\data:/mnt/ggs_data geocoding-custom:1.0.0
```
### Access the application
 To access the demo web service, open the following URL in a browser: 
 `http://localhost:8080/addressing/predict/<country>?input=<address>` 
 
For example:
    http://localhost:8080/addressing/predict/usa?input=350%20jordan 
 
 