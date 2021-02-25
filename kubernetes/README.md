# Geocoding Application for Kubernetes Deployment Guide
This guide provides detailed instructions for deploying the sample Spectrum Global Geocoding application in a Kubernetes environment.

## Install client tools
To deploy the Geocoding application in a Kubernetes environment, install the following client tools that are applicable to your environment:
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
- [Helm 3](https://helm.sh/docs/intro/install/)
##### Amazon EKS
- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)
- [eksctl](https://docs.aws.amazon.com/eks/latest/userguide/getting-started-eksctl.html)
##### Google GKE
- [Google Cloud SDK](https://cloud.google.com/sdk/install)
##### Microsoft AKS
- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)

## Deploy the Geocoding application Docker image
The Geocoding application is packaged as a Docker image and should be deployed to an accessible container registry, such as [Amazon ECR](https://docs.aws.amazon.com/AmazonECR/latest/userguide/Registries.html) for EKS, or on [Google GCR](https://cloud.google.com/container-registry) registry for GKE, or on [Azure ACR](https://azure.microsoft.com/en-in/services/container-registry/) registry for Microsoft AKS.

To build the Docker image, use one of these methods:
- To build using the provided Spectrum Global Geocoding REST APIs, see [docker/geocoding](../docker/geocoding)
- To build a custom application using Spectrum Global Geocoding Java SDK, see [docker/geocoding-custom](../docker/geocoding-custom)

## Create the Kubernetes cluster
The sample geocoding application requires a Kubernetes cluster with at least one node to run the Geocoding application and a separate node for the NGINX ingress controller. This sample cluster will scale the number of nodes available for running the Geocoding application up to a maximum of 10, based on user load.

##### Amazon EKS
>To create an Amazon EKS cluster, follow the instructions in [AmazonEKSSetup.md](./cluster/eks/AmazonEKSSetup.md). 
##### Google GKE
>To create a Google GKE cluster, follow the instructions in [GoogleGKESetup.md](./cluster/gke/GoogleGKESetup.md).
##### Microsoft AKS
>To create a Microsoft AKS cluster, follow the instructions in [MicrosoftAKSSetup.md](./cluster/aks/MicrosoftAKSSetup.md).

## Configure Helm
Add the required Helm chart repositories. These repositories will be used to deploy components in the cluster:
```
helm repo add stable https://kubernetes-charts.storage.googleapis.com
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add stable https://charts.helm.sh/stable

```
## Deploy the NGINX Ingress Controller and Prometheus-Adapter with Prometheus
   The Geocoding application uses the NGINX Ingress Controller as a load balancer in order to monitor the number of active users. In addition,  Prometheus-Adapter is installed along with Prometheus Server in order to provide this data as custom metrics in Kubernetes, which the Geocoding application uses to autoscale. 
   1. Install Prometheus Server using Helm:      
      ```
      helm install prometheus stable/prometheus
      ```
        
   2. Install Prometheus-Adapter using Helm:
      
      ```
      helm install prometheus-adapter stable/prometheus-adapter  -f ./ggs-ingress/prometheus-adapter/values.yaml
      ```
   3. Install the NGINX Ingress Controller by executing this command:
        
      ```
      helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
      helm install nginx-ingress ingress-nginx/ingress-nginx -f ./ggs-ingress/nginx-ingress-controller/values.yaml --version "2.16.0" 
      ```
      
      Wait for all pods to come up in a running state; it generally takes 2 to 5 minutes.
      ```
      kubectl get pods -w
      ```  
            
      Verify the installation of the NGINX Ingress controller using the following command. It may take few minutes for the external address to be available
         
      ```
      kubectl get --raw="/apis/external.metrics.k8s.io/v1beta1/namespaces/default/nginx_active_connections"
      ```
      - When the NGINX Ingress Controller is running properly, you will see the output below. You can now proceed to the next step.

        ```
        {"kind":"ExternalMetricValueList","apiVersion":"external.metrics.k8s.io/v1beta1","metadata":{"selfLink":"/apis/external.metrics.k8s.io/v1beta1/namespaces/default/nginx_active_connections"},"items":[{"metricName":"nginx_active_connections","metricLabels":{},"timestamp":"2020-04-28T10:48:44Z","value":"1"}]}
        ```       
      - If the command displays the following output message, it means Prometheus-Adapter is not running. Check its status and wait until it is up before retrying.
        >`Error from server (NotFound): the server could not find the requested resource`
              
      - If you see either of the following messages, it means the NGINX Ingress Controller is still coming online. You should wait a minute or two and try running the same command again.
        >`Error from server (NotFound): the server could not find the metric nginx_active_connections for namespaces`              
        
        >`Error from server (ServiceUnavailable): the server is currently unable to handle the request`

## Update credentials in Kubernetes secret script

This is required to access .spd files from cloud storage. In the `./ggs/ggs-storage-secret.yml` file, specify any one of these credentials in the `stringData` parameter.

##### Amazon [S3](https://aws.amazon.com/s3/)

- Provide your S3 credentials
    - `S3_ACCESS_KEY` - s3 access key
    - `S3_SECRET_KEY`  - s3 secret key
  ```
   stringData:
     S3_ACCESS_KEY: "AKI*****************J"
     S3_SECRET_KEY: "o70*************************************Re" 
  ```
  
##### Google [Cloud Storage](https://cloud.google.com/storage)
- Provide json string of your Google service account's key
    - `GOOGLE_SERVICE_ACCOUNT_KEY_JSON` - json string of Google service account's key
    
  ```
   stringData:
     GOOGLE_SERVICE_ACCOUNT_KEY_JSON: |
         {
          "type": "service_account",
          "project_id": "l*******l",
          "private_key_id": "133********************************14c8f",
          "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvAIB*********************************CzDQ==\n-----END PRIVATE KEY-----\n",
          "client_email": "s*****tion@l***********l.iam.gserviceaccount.com",
          "client_id": "10************************893",
          "auth_uri": "https://accounts.google.com/o/oauth2/auth",
          "token_uri": "https://oauth2.googleapis.com/token",
          "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
          "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/s*****tion@l***********l.iam.gserviceaccount.com"
         }
  ```
  
##### Microsoft [Azure Blob Storage](https://azure.microsoft.com/en-in/services/storage/blobs/)
- Provide your Azure Blob storage account's name and key
    - `AZURE_STORAGE_ACCOUNT` - storage account's name
    - `AZURE_STORAGE_ACCOUNT_KEY`  - storage account's key
  ```
   stringData:
     AZURE_STORAGE_ACCOUNT: "s***d"
     AZURE_STORAGE_ACCOUNT_KEY: "+mBd***********************************************************X5g==" 
  ```
**Note:** To create this secret from Azure Key Vault, you can follow Microsoft's documentations for [Azure Key Vault](https://docs.microsoft.com/en-us/azure/key-vault/general/key-vault-integrate-kubernetes)

Deploy the secret manifest script:
```
kubectl apply -f ./ggs/ggs-storage-secret.yml
``` 

## Configure the reference datasets
The Geocoding application requires geocoding reference datasets, which are .spd files that must be available on [S3](https://aws.amazon.com/s3/) for EKS, [Google Storage](https://cloud.google.com/storage/docs/creating-buckets) for GKE, or [Azure Blob Storage](https://azure.microsoft.com/en-in/services/storage/blobs/) for Microsoft AKS. The datasets will be accessed from the `./ggs/ggs-datasets-cm.yaml` config map.

* If you have not already downloaded the reference data, for information about Precisely's data portfolio, see the [Precisely Data Guide](https://dataguide.precisely.com/) where you can also sign up for a free account and access sample data available in [Precisely Data Experience](https://data.precisely.com/).

In the `./ggs/ggs-datasets-cm.yaml` file, specify the full path of each dataset file kept on cloud storage in the `spd.list` parameter.

**Note:** Dont use URI scheme in complete path of dataset.

```
  spd.list : |
    com-precisely-geocoding/data/2020.12/GCM-WORLD-STREET-WBL-112-202012-INTERACTIVE.spd
    com-precisely-geocoding/data/2020.12/EGM-WORLD-STREET-WBL-112-202012-GEOCODING.spd
```

Deploy the datasets manifest script:
```
kubectl apply -f ./ggs/ggs-datasets-cm.yaml
``` 

## Deploy the geocoder default preferences and shared resources
These resources will be described the same across all Kubernetes platforms.

To modify the geocoder default preferences, see the `ggs/geocode-preferences-cm.yaml` file for descriptions of the configuration parameters.

Execute these commands:   
   ```
   kubectl apply -f ./ggs/geocode-preferences-cm.yaml    
   kubectl apply -f ./ggs/ggs-dataprep-cm.yaml 
   kubectl apply -f ./ggs/ggs-service.yaml
   kubectl apply -f ./ggs-ingress/ggs-ingress-resource.yaml 
   kubectl apply -f ./ggs/ggs-hpa.yaml  
   ```

## Deploy the Geocoding application
Spectrum Global Geocoding requires the reference data to be available on the file system of the pod running the geocoding service. Due to the size of the reference data, the data is managed outside of the docker image and configured during deployment.  Two options for configuring the reference data are provided:

- Option A: The reference data is initialized on an [emptyDir volume](https://kubernetes.io/docs/concepts/storage/volumes/#emptydir)
- Option B: The reference data is initialized on a [persistent volume](https://kubernetes.io/docs/concepts/storage/volumes/#nfs)

#### Option A: Reference data is initialized on an emptyDir volume
This is the simplest approach to deploy the Geocoding application. During startup, a geocoding pod copies the data from Cloud Storage (S3 or GS) to an emptyDir volume that's mounted to a local directory.

**Note**: Each new geocoding pod copies the data from the storage bucket to the local directory. This increases the pod startup time, so this approach may not be appropriate for production usage where faster startup time is required.

Steps to deploy:

  1. Add the Geocoding application Docker image URI.
     
     For `Google GKE` or `Amazon EKS` use `./ggs/local-data/ggs-runtime.yaml` file, but for `Microsoft AKS` use `./ggs/local-data/aks/ggs-runtime.yaml` file. In the file, replace:
     - `@IMAGE_URI@` - the URI of the Geocoding application Docker image stored in the Docker repository in the `image` parameter. The `@IMAGE_URI@` parameter needs to be replaced in two places.
      ```
          initContainers:
             - name: ggs-dataprep-container
               image: @IMAGE_URI@
       ```
       and
       ```
             containers:
               - name: ggs-container
                 image: @IMAGE_URI@
       ```  
 
  2. Deploy the Geocoding application runtime: 
     
     ```
       kubectl apply -f ./ggs/local-data/ggs-runtime.yaml
     ``` 
#### Option B: Reference data is initialized on a persistent volume
This approach minimizes pod startup time by preparing the reference data ahead of deployment on a shared persistent volume.  The deployment of the geocoding data using a persistent volume is a 2-step process:

  1. Configure the persistent volume with reference data.
  2. Deploy the geocoding application using the data from the persistent volume.

 #### 1. Configure the persistent volume with reference data
 This sample demonstrates configuring a persistent volume backed by high performance cloud based file storage.  Though the steps outlined are written for specific products (`Amazon EFS`, `Google Filestore`, `Azure Files`), the process is generally applicable for other persistent volume types. Follow the steps below based on your platform.
 ##### Amazon EKS
 >To deploy the geocoding reference data using [Amazon Elastic File System](https://aws.amazon.com/efs/), follow the instructions in [AmazonEKSReferenceData.md](./ggs/nfs-data/AmazonEKSReferenceData.md). 
 ##### Google GKE
 >To deploy the geocoding reference data on [Google Filestore](https://cloud.google.com/filestore), follow the instructions in [GoogleGKEReferenceData.md](./ggs/nfs-data/GoogleGKEReferenceData.md).
##### Microsoft AKS
>To deploy the geocoding reference data using [Microsoft Azure Files](https://azure.microsoft.com/en-in/services/storage/files/), follow the instructions in [MicrosoftAKSReferenceData.md](./ggs/nfs-data/MicrosoftAKSReferenceData.md).


#### 2. Deploy the geocoding application using the data from the persistent volume
The Geocoding application uses the same persistent volume where you deployed the reference data in the previous step. 
 To deploy the application:
 
   - Deploy the persistent volume:
     
     **Note:** If you deployed the persistent volume in the previous step and have not deleted it, then it should be available. In that case, you can skip this step.
      ##### Amazon EKS
      ```
      kubectl apply -f ./ggs/nfs-data/eks/ggs-data-pv.yaml
      ```
     ##### Google GKE
      ```
      kubectl apply -f ./ggs/nfs-data/gke/ggs-data-pv.yaml
      ```
     ##### Microsoft AKS
      ```
      kubectl apply -f ./ggs/nfs-data/aks/ggs-data-pv.yaml
      ```

      - In the `./ggs/nfs-data/ggs-runtime.yaml` file, replace:
      -  `@IMAGE_URI@` - the URI of the Geocoding application Docker image stored in the Docker repository in the `image` parameter.
   
   - Deploy the Geocoding application runtime:
     ```
     kubectl apply -f ./ggs/nfs-data/ggs-runtime.yaml
     ``` 
## Access the Geocoding application
Once the above steps have completed, the Geocoding application is up and running. You can access the Geocoding services endpoints using a web browser. You can also use a Web Service invocation tool to access the REST service endpoints available in the application.

Retrieve the Ingress service address which exposes the Geocoding application to the external world using this command:
              
  ```
  kubectl  get services -o wide -w nginx-ingress-ingress-nginx-controller
   ```
Copy the external IP/URL and port of the Ingress service, and then create the Geocoding application test page URL: 
              
  `http://<External-IP>:<port>/geocode`                
              
  For example: `http://ac97c5928849311ea9e8602781e57924-913734340.us-east-1.elb.amazonaws.com/geocode`

