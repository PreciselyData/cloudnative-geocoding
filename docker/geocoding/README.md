# Sample Geocoding Application for Docker

A sample Geocoding application that is containerized in Docker. This sample demonstrates the geocoding, reverse geocoding, verify, predict, lookup and parse features of the *Spectrum Operational Addressing SDK* running inside a Docker container. The Geocoding application image can then be deployed into a Kubernetes environment. For deployment instructions, see the [Geocoding Application for Kubernetes Deployment Guide](../../k8s/README.md).    

## Prerequisites
### Install the client tools 
This sample requires Docker Engine to build the image. 
   * [Docker Engine](https://docs.docker.com/engine/install/)

The image can be pushed to and stored in a cloud registry. Depending on the registry you publish to, determines the tool that you need to install. The Docker CLI is included in the Docker install, so a separate tool is not required for publishing images to Docker Hub.
   * [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) - for publishing to Amazon ECR.
   * [Google Cloud CLI](https://cloud.google.com/sdk) - for publishing to Google GCR.
   * [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli) - for publishing to Microsoft ACR.

 ### Download the SDK & data
   * Spectrum Operational Addressing SDK (OAS) distribution - For information about OAS, see the [Precisely Support](https://support.precisely.com/) site and the [Operational Addressing SDK Developer Guide](https://docs.precisely.com/docs/sftw/ggs/5.0/en/webhelp/index.html#GlobalGeocodingGuide/source/LandingPageForHelp_GGS.html) documentation.
   * Geocoding reference data in `.spd` format -  For information about Precisely's data portfolio, see the [Precisely Data Guide](https://dataguide.precisely.com/) where you can also sign up for a free account and access sample data available in [Precisely Data Experience](https://data.precisely.com/). 

## Build the Docker image
The geocoding Docker sample contains a [Dockerfile](Dockerfile) along with startup script ([deploy-ggs.sh](deploy-ggs.sh)).

1. If you haven't already, clone or download this repository to your computer. 
2. Extract Spectrum Operational Addressing SDK (OAS) distribution's zip in `<sample-directory>/docker/geocoding/ggs` directory.
3. Build & push the Docker image by executing the following command from the `<sample-directory>/docker/geocoding` directory, where: 
   - `-t [IMAGE]` - image name or ID, and optionally a tag in the ‘name:tag’ format
	  
   ```
   docker build -t [IMAGE] .
   ```
4. Verify the image built successfully and is stored in the local repository. Your image should be listed after executing the following command:
   ```
   docker image ls
   ```
5. Push the image to the remote repository:
       
   #### Amazon ECR
   Use one of the following methods to push your image to your remote repository. We’ve provided some example steps but you can refer to the [Amazon documentation](https://docs.aws.amazon.com/AmazonECR/latest/userguide/docker-push-ecr-image.html) for additional support.

   You may need to first set up your environment by running the `_aws_configure_` command.

	Login to the ECR repository:
	   
   ```
   aws ecr get-login-password --region [REGION] | docker login --username AWS --password-stdin [AWS-ACCOUNT-ID].dkr.ecr.[REGION].amazonaws.com
   ```
   Tag your Docker image, where:
   - `[IMAGE]` - image name or ID
   - `[IMAGE]:[TAG]` - image name/ID and tag. If `[TAG]` field is omitted, `latest` is assumed.

   ```
   docker tag [IMAGE] [AWS-ACCOUNT-ID].dkr.ecr.[REGION].amazonaws.com/[IMAGE]:[TAG]
   ```
   Push to the ECR repository:
   ```
   docker push [AWS-ACCOUNT-ID].dkr.ecr.[REGION].amazonaws.com/[IMAGE]:[TAG]
   ```

   #### Google GCR
   Use one of the following methods to push your image to your remote repository. We’ve provided some example steps but you can refer to the [Google documentation](https://cloud.google.com/container-registry/docs/pushing-and-pulling). for additional support.

 	Login to the GCR repository using the service account key file (Google provides multiple methods for login; for other methods, see their [documentation](https://cloud.google.com/container-registry/docs/advanced-authentication)).  
 	      	   
   ```
   gcloud auth activate-service-account --key-file=${keyPath}
   gcloud auth configure-docker --quiet
   ```
   Tag your docker image, where:
   - `[IMAGE]` = the local image name or ID
   - `[IMAGE]:[TAG]` = image name and tag. If `[TAG]` field is omitted, `latest` is assumed.
   ```
   docker tag [IMAGE] us.gcr.io/[PROJECT-ID]/[IMAGE]:[TAG]
   ```
   Push the image to the GCR repository:
   ```
   docker push us.gcr.io/[PROJECT-ID]/[IMAGE]:[TAG]
   ```

   #### Microsoft ACR
   Use one of the following methods to push your image to your remote repository. We’ve provided some example steps, but you can refer to the [Microsoft documentation](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-get-started-docker-cli) for additional support.

   Azure CLI supports multiple authentication methods; use any authentication method to sign in. For details about Azure's authentication types see their [documentation](https://docs.microsoft.com/en-us/cli/azure/authenticate-azure-cli).

   - `[AZURE_CONTAINER_REGISTRY]` =  Azure container registry name
   - `[SUBSCRIPTION_ID]` =  Azure subscription ID
   
   ``` 
   az login 
   az acr login --name [AZURE_CONTAINER_REGISTRY] --subscription [SUBSCRIPTION_ID]
   ``` 
	
   Tag your docker image, where:
   - `[IMAGE]` = the local image name or ID
   - `[IMAGE]:[TAG]` = image name and tag. If `[TAG]` field is omitted, `latest` is assumed.
   ```
   docker tag [IMAGE] [STORAGE_ACCOUNT_NAME].azurecr.io/[PROJECT-ID]/[IMAGE]:[TAG]
   ```
   Push the image to the GCR repository:
   ```
   docker push [STORAGE_ACCOUNT_NAME].azurecr.io/[PROJECT-ID]/[IMAGE]:[TAG]
   ```

## Running the Docker image locally     
### Set up the data
Place your data into the `/docker/geocoding/spd_files` directory. Run the command mentioned below to extract the reference data on the given location `/ggs_data`, that the application will use at runtime.

   - Windows
   
   ```
    cmd /c "<sample-directory>/docker/geocoding/ggs/cli/cli.cmd" extract --s spd_files --d ggs_data
   ```

   - Linux
   
   ```
    sh "<sample-directory>/docker/geocoding/ggs/cli/cli.sh" extract --s spd_files --d ggs_data
   ```
### Start the container
Use the docker run command below to start the image on a local Docker daemon.  

This docker command consists of:
* Setting the port forwarding for the Geocoding Service.
* Mounting a local directory containing the geocoding reference data to a location that the container will access (i.e. `/usr/local/ggs/data`).

The memory requirement may need to be adjusted as datasets are added to the geocoder. 

A general recommendation for minimum heap size is:
   - 1-2 datasets - 2 GB
   - 3 or more datasets - 8 GB 
```
docker run -p 8080:8080 -v <sample-directory>/docker/geocoding/ggs_data:/usr/local/ggs/data  -e GGS_DATA=/usr/local/ggs/data -e CATALINA_OPTS='-Xmx10g -Xms10g -XX:MaxPermSize=1024m' [IMAGE ID]
```
### Access the application
 To access the web-based demo application, open the following URL in a browser: 
   http://localhost:8080/geocode
 
 The Operational Addressing REST services are deployed at the following URL:
 http://localhost:8080/geocode/rest/addressing?_wadl

  



