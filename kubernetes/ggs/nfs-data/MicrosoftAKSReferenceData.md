# Geocoding Reference Data on Microsoft Azure Files

This sample demonstrates using a [persistent volume](https://kubernetes.io/docs/concepts/storage/persistent-volumes/) back by [Microsoft Azure Files](https://azure.microsoft.com/en-in/services/storage/files/) to store the reference data which will be accessed by the Geocoding application at runtime. To initialize the persistent volume it is mounted to a separate, temporary, deployment of the Geocoding application – the “staging” deployment.  When the staging deployment starts, the data is copied from [Azure Blob Storage](https://azure.microsoft.com/en-in/services/storage/blobs/) and extracted to the persistent volume.  Once the data has been initialized, the staging deployment can be stopped and the temporary resources deleted.  The Geocoding application will then access the reference data by mounting the same persistent volume that had just been initialized by the staging deployment.

This reference data deployment process needs to be executed only once for the Geocoding application deployment. You only need to re-run it when you want to update the deployed data, such as with a new vintage, or if you want to add support for additional countries.  At that time, you can use the staging process to prepare a new, separate, persistent volume and then update your running deployment of the Geocoding application to use that new persistent volume with zero application downtime.

## Create and configure an Azure Files share
The following directions will guide you through the process of preparing an `Azure Files` for your deployment by using the `Azure CLI`.  If you have already created and configured an ` Azure Files share` that you want to use, and it is accessible from your AKS cluster, then you can skip this step and move on to the next.

#### 1. Deploy the CSI Driver - this will be used to mount  Azure Files storage with a persistent volume.
```
helm repo add azurefile-csi-driver https://raw.githubusercontent.com/kubernetes-sigs/azurefile-csi-driver/master/charts
helm repo update
helm install azurefile-csi-driver azurefile-csi-driver/azurefile-csi-driver --namespace kube-system --set cloud=AzureStackCloud
```

#### 2. Create the Azure Files storage.
If you have already created & configured an instance of the Azure Files share, and it is accessible from your AKS cluster, then you can ignore this step and move to the next step.

- Loging to Azure CLI

  ```
  az login
  ```

- Get key of storage account, 

  This key will be used to create an instance of `Azure Files`. Before executing this command replace `@RESOURCE_GROUP@` with your resource group and `@STORAGE_ACCOUNT_NAME@` with storage account's name.
  ```
  az storage account keys list --resource-group @RESOURCE_GROUP@ --account-name @STORAGE_ACCOUNT_NAME@ --query "[0].value" | tr -d '"'
  ```
  Your output should be similar to this:

  ```
  oentIRQt28dlVd62Sb6HpSYb3EmlNwUw0dpl+bwmdXpn2E3iR2+x2Q3ztRfkspWUiHYRbiaZ8UOeSjsjrQZ4vw==
  ```
- Create the EFS file system.

  We are using `ggsdatashare` as the name for the Azure Files share name; if you want to use a different name, then you can update the following command accordingly.
  
  Your output should be similar to this:

   ```
   az storage share create --account-name @STORAGE_ACCOUNT_NAME@--account-key @STORAGE_ACCOUNT_KEY@ --name "ggsdatashare"
   ```
  Your output should be similar to this:

   ```
   {
      "created": true
   }
   ```
  **Note:** It is recommended that you use [Premium Azure Files](https://docs.microsoft.com/en-us/azure/storage/files/storage-files-scale-targets) for consistent performance of the application.

#### 3. Update the persistent volume resource definition to use your Azure files system.
If you don’t have your EFS FileSystemId, see “Query the FileSystemId for your EFS file system” above.

In the `./ggs/nfs-data/aks/ggs-data-pv.yaml` file, replace:
- `@STORAGE_ACCOUNT_NAME@` - your storage account name
- `@AZURE_FILES_SHARE_NAME@` - your Azure Files share name
  ```
  csi:
    driver: file.csi.azure.com
    readOnly: false
    volumeHandle: ggs-data-pv  # make sure it's a unique id in the cluster
    volumeAttributes:
      storageAccount: ggsdata
      shareName: ggsdatashare
    nodeStageSecretRef:
      name: azure-storage-secret
      namespace: default
   ```  
#### 4. Add the Geocoding application Docker image URI.
In the `./ggs/nfs-data/aks/ggs-staging.yaml` file, replace:
- `@IMAGE_URI@` - the URI of the Geocoding application Docker image stored in the [ACR Repository](https://azure.microsoft.com/en-in/services/container-registry/) in the `image` parameter. The `@IMAGE_URI@` parameter needs to be replaced in two places.
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
## Ccreate [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/) with storage account name and key. 

This secret will be used by your cluster to access Azure Blob Storage and Azure Files.
```
kubectl create secret generic azure-storage-secret --from-literal=azurestorageaccountname="@STORAGE_ACCOUNT_NAME@" --from-literal=azurestorageaccountkey="@STORAGE_ACCOUNT_NAME@" 
```
## Start the staging deployment
If you haven't already deployed the geocoding preferences, datasets, and data preparation script config maps, then deploy the following manifest files:
```
kubectl apply -f ./ggs/ggs-datasets-cm.yaml
kubectl apply -f ./ggs/geocode-preferences-cm.yaml
kubectl apply -f ./ggs/aks/ggs-dataprep-cm.yaml
```
Create the persistent volume and staging deployment:
```
kubectl apply -f ./ggs/nfs-data/aks/ggs-data-pv.yaml
kubectl apply -f ./ggs/nfs-data/aks/ggs-staging.yaml
```
After these commands execute, the staging deployment will start, and the init container used in the staging deployment will copy the data to the persistent volume.  Once the staging deployment enters the “running” state, the copy is complete. Depending on the size of the reference datasets, this process may take some time.

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

   For example: http://52.213.65.21/geocode

## Delete the staging resources
After verifying the data, the staging resources are no longer needed and can be deleted using this command:
```
kubectl delete -f ./ggs/nfs-data/aks/ggs-staging.yaml
```
## Next step
Now that the persistent volume has been created, and the reference data has been configured on your Azure Files share, you can mount the persistent volume to use that data in your [Geocoding application deployment](../../README.md).

