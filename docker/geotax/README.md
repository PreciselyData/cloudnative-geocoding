# Sample Geotax Application for Docker

A sample Geotax application that is containerized in Docker. This sample demonstrates the Taxrate information based upon Location and Address (GET/POST) features of the *Geotax SDK* running inside a Docker container. The Geotax application image can then be deployed into a Kubernetes environment. For deployment instructions, see the [Geotax Application for Kubernetes Deployment Guide](../../k8s/README.md).    

## Prerequisites
### Install the client tools 
This sample requires Docker Engine to build the image. 
   * [Docker Engine](https://docs.docker.com/engine/install/)

The image can be pushed to and stored in a cloud registry. Depending on the registry you publish to, determines the tool that you need to install. Here we have used AWS CLI for this sample application. The Docker CLI is included in the Docker install, so a separate tool is not required for publishing images to Docker Hub.
   * [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) - for publishing to Amazon Elastic Container Registry (ECR).
 
 ### Download the SDK & data
   * Geotax SDK war - For information about Geotax-SDK, see the [Precisely Support](https://support.precisely.com/) site and the [Geotax SDK Developer Guide]() documentation. (**TODO** -doc link to be inserted)
   * Geotax reference data in `.spd` format -  For information about Precisely's data portfolio, see the [Precisely Data Guide](https://dataguide.precisely.com/) where you can also sign up for a free account and access sample data available in [Precisely Data Experience](https://data.precisely.com/). 

## Build the Docker image
The geotax Docker sample contains a [Dockerfile](Dockerfile) which will be used to build the docker image.

1. If you haven't already, clone or download this repository to your computer. 
2. Copy the Geotax SDK war file in `<sample-directory>/docker/geotax/gtx` directory.
3. Build & push the Docker image by running the following command from the `<sample-directory>/docker/geotax` directory, where: 
   - `-t [IMAGE]` - image name or ID, and optionally a tag in the ‘name:tag’ format
	  
   ```
   docker build -t [IMAGE] .
   ```
4. Verify the image built successfully and is stored in the local repository. Your image should be listed after running the following command:
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

## Running the Docker image locally     
### Set up the data
Place your data into the `/docker/geotax/spd_files` directory. Run the command mentioned below to extract the reference data on the given location `/gtx_data`, that the application will use at runtime.

   - Windows
   
   ```
    cmd /c "<sample-directory>/docker/geotax/gtx/cli/cli.cmd" extract --s spd_files --d gtx_data
   ```

   - Linux
   
   ```
    sh "<sample-directory>/docker/geotax/gtx/cli/cli.sh" extract --s spd_files --d gtx_data
   ```
### Start the container
Use the docker run command below to start the image on a local Docker daemon.  

This docker command consists of:
* Setting the port forwarding for the Geotax Service.
* Mounting a local directory containing the geotax reference data to a location that the container will access (i.e. `/usr/local/gtx/data`).

The memory requirement may need to be adjusted as datasets are added to the application. 

```
docker run -p 8080:8080 -v <sample-directory>/docker/geotax/gtx_data:/usr/local/gtx/data  -e GTX_DATA=/usr/local/gtx/data -e CATALINA_OPTS='-Xmx10g -Xms10g -XX:MaxPermSize=1024m' [IMAGE ID]
```
### Access the application
 To access the web-based demo application, open the following URL in a browser: 
   http://localhost:8080/

