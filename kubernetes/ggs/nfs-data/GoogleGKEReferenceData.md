# Geocoding Reference Data on Google Filestore
This sample demonstrates using a [persistent volume](https://kubernetes.io/docs/concepts/storage/persistent-volumes/) backed by [Google Filestore](https://cloud.google.com/filestore)  to store the reference data which will be accessed by the Geocoding application at runtime. To initialize the persistent volume it is mounted to a separate, temporary, deployment of the Geocoding application – the “staging” deployment.  When the staging deployment starts, the data is copied from [Google Cloud Storage](https://cloud.google.com/storage) and extracted to the persistent volume.  Once the data has been initialized, the staging deployment can be stopped and the temporary resources deleted.  The Geocoding application will then access the reference data by mounting the same persistent volume that had just been initialized by the staging deployment.

This reference data deployment process needs to be executed only once for the Geocoding application deployment. You only need to re-run it when you want to update the deployed data, such as with a new vintage, or if you want to add support for additional countries.  At that time, you can use the staging process to prepare a new, separate, persistent volume and then update your running deployment of the Geocoding application to use that new persistent volume with zero application downtime.

## Configure the data resources

#### 1. Create a Filestore instance.
The following directions will guide you through the process of preparing a Filestore instance for your deployment by using the gcloud CLI.  If you have already created and configured a Filestore instance that you want to use, and it is accessible from your GKE cluster, then you can skip this step and move on to the next. For more information about creating an instance of Google Filestore, refer to the documentation at:
https://cloud.google.com/filestore/docs/creating-instances

To create a Filestore instance for the Geocoding application:

- Configure the GCP project ID that will be used to create the Filestore instance; otherwise, you will need to provide this project ID in every command:
  ```
  gcloud config set project ggs-demo
  ```

- Locate the VPC network of your cluster - the Filestore instance and GKE cluster must be in the same network to access the data from them in the cluster:
  ```
  gcloud container clusters describe ggs-sample --zone us-east1-c --format="value(network)"
  ```
  Your output should be similar to this:
  ```
  default
  ```
- Locate the compute zone of your cluster; this is also required to create a Filestore instance:
  ```
  gcloud container clusters describe ggs-sample --zone us-east1-c --format="value(location)"
  ```

  Output
  ```
  us-east1-c
  ```

- Create the Filestore instance using the retrieved values: 
  ```
  gcloud filestore instances create ggs-data --zone us-east1-c --network=name=default --file-share=name=ggs_data,capacity=1TB 
  ```
  **Note:** Uppercase and hyphen ('-') are not allowed values for `--file-share`; however, using an underscore ('_') is supported. Google requires a minimum capacity of 1 TB. 
  Creating the Filestore instance takes a few minutes. When the command completes, you can verify that your Filestore instance was created: 
  ```
  gcloud filestore instances describe ggs-data --zone us-east1-c
  ```
  Your output should be similar to this:
  ```
   createTime: '2020-08-18T10:13:20.387685657Z'
   fileShares:
   - capacityGb: '1024'
     name: ggs_data
   name: projects/ggs-demo/locations/us-east1-c/instances/ggs-data
   networks:
   - ipAddresses:
     - 10.13.31.106
     network: default
     reservedIpRange: 10.13.31.104/29
   state: READY
   tier: STANDARD
   ```
#### 2. Update the persistent volume resource definition to use your EFS file system.
- Locate the IP address of your Filestore instance:
   ```
   gcloud filestore instances describe ggs-data --zone us-east1-c --format="value(networks[0].ipAddresses[0])"
   ```
  Your output should be similar to this:
  ```
  10.231.81.122
  ```
- Locate the NFS path (the name) of your Filestore instance:
  
  ```
  gcloud filestore instances describe ggs-data --zone us-east1-c --format="value(fileShares[0].name)"
  ```
  Your output should be similar to this:
  ```
  ggs_data
   ```
- In the `./ggs/nfs-data/gke/ggs-data-pv.yaml` file, update the Filestore path and IP address:
  ```
  nfs:
    path: /ggs_data
    server: 10.231.81.122
  ```  
**Note:** The path of the data that you are going to use must exist on Filestore.

#### 3. Add the Geocoding application Docker image URI.
In the `./ggs/nfs-data/ggs-staging.yaml` file, specify the URI of the Geocoding application Docker image stored in the GCR Repository in the `image` parameter.
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

#### 4. Add the `GS_AUTH2_REFRESH_TOKEN` parameter and Google project name.
In the `../ggs/gke/ggs-dataprep-cm.yaml` file, replace:
-  `@GS_AUTH2_REFRESH_TOKEN@` - a valid gsutil refresh token
- `@PROJECT_ID@` - the Google project name.      

  ```
    .boto: |
    [Credentials]
    gs_oauth2_refresh_token = 1//0gzLN6zasjaksjaksjaksjGBASNwF-L9Irp8J9hTDMmfpiuZp_AsbajsajsajsawrQTgxSi56OWvdMGkssk5zZt0lrc9Y2WaVE
    [Boto]
    https_validate_certificates = True
    [GSUtil]
    content_language = en
    default_api_version = 2
    default_project_id = ggs-demo
  ```

      
## Start the staging deployment
If you haven't already deployed the geocoding preferences, datasets, and data preparation script config maps, then deploy the following manifest files:
```
kubectl apply -f ./ggs/ggs-datasets-cm.yaml
kubectl apply -f ./ggs/geocode-preferences-cm.yaml
kubectl apply -f ./ggs/gke/ggs-dataprep-cm.yaml
```

Deploy the reference data by executing the following commands: 
```
kubectl apply -f ./ggs/nfs-data/gke/ggs-data-pv.yaml
kubectl apply -f ./ggs/nfs-data/ggs-staging.yaml
```
After these commands execute, the staging deployment will start and the init container used in the staging deployment will copy the data to the persistent volume.  Once the staging deployment enters the “running” state, the copy is complete. Depending on the size of the reference datasets, this process may take some time.

You can check the status of the pods using this command:
```
kubectl get pods -w
```
To monitor the progress of the data deployment, you can check the logs of the staging pod using this command:
```
kubectl logs -f -l app=ggs-dataprep -c ggs-dataprep-container
```

## Verify the reference data deployment
To verify that the reference data successfully deployed, you can issue a request to the Geocoding service.

1. Locate your service's external URL and port number:
    ```
    kubectl get services -l app=ggs-dataprep
    ```
2. Use the external URL and port number to access the Geocoding application test page in a browser.
The URL should be formatted like this:
              
    `http://<External-IP>:<port>/geocode` 
    
    For example: http://34.68.55.123/geocode

## Delete the Staging resources
After verifying the data, the staging resources are no longer needed and can be deleted using this command:
```
kubectl delete -f ./ggs/nfs-data/ggs-staging.yaml
```
## Next step
Now that the persistent volume has been created and the reference data has been configured on your Filestore instance, you can mount the persistent volume to use that data in your [Geocoding application deployment](../../README.md).
