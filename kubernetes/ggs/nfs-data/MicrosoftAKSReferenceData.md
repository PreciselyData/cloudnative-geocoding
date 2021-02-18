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

- Register/Enable the NFS 4.1 protocol for your Azure subscription
  ```
  az feature register --name AllowNfsFileShares --namespace Microsoft.Storage --subscription @SUBSCRIPTION_ID@
  az provider register --namespace Microsoft.Storage
  ```
  Registration approval can take up to an hour. To verify that the registration is complete, use the following commands:
  ```
  az feature show --name AllowNfsFileShares --namespace Microsoft.Storage --subscription @SUBSCRIPTION_ID@
  ```
  
- Create a FileStorage storage account by using following command, only `FileStorage` type storage account has support of [NFS protocol](https://docs.microsoft.com/en-us/azure/storage/files/storage-files-how-to-create-nfs-shares?tabs=azure-portal). Remembering to replace @RESOURCE_GROUP@ with the appropriate values for your environment.

  ```
  az storage account create --name ggsdataaccount --location eastus --sku Premium_LRS --kind FileStorage --https-only false

  ```

- Create an NFS share
  ```
  az storage share-rm create --storage-account ggsdataaccount --name ggsdatashare --quota 100 --enabled-protocol NFS 
  ```

- Grant access of FileStorage from your cluster's [virtual network](https://docs.microsoft.com/en-us/azure/storage/common/storage-network-security?tabs=azure-cli).
  
  - Find node resource group of your AKS cluster - 
    ```
    az aks show --name ggssample --query "nodeResourceGroup"
    ```
    Output:
    ```
    "MC_ss4bd-aks-deployment-sample_ggssample_eastus"
    ```
  - Find name of your AKS cluster's virtual network by using node resource group. 
    ```
    az network vnet list --resource-group MC_ss4bd-aks-deployment-sample_ggssample_eastus --query "[0].name"
    ```
    Output:
    ```
    "aks-vnet-42915476"
    ```
  - Find subnets of your AKS cluster by using node resource group.
    ```
    az network vnet show --resource-group MC_ss4bd-aks-deployment-sample_ggssample_eastus --name aks-vnet-42915476 --query "subnets[*].name"
    ```
    Output:
    ```
    [
      "aks-subnet"
    ]
    ```
  - Enable service endpoint for Azure Storage on your cluster's virtual network and subnet.
    ```
    az network vnet subnet update --resource-group MC_ss4bd-aks-deployment-sample_ggssample_eastus --vnet-name "aks-vnet-42915476" --name "aks-subnet" --service-endpoints "Microsoft.Storage"
    ```
  - Find id of your AKS cluster's subnets
    ```
    az network vnet show --resource-group MC_ss4bd-aks-deployment-sample_ggssample_eastus --name aks-vnet-42915476 --query "subnets[*].id"
    ```
    Output:
    ```
    "/subscriptions/385ad333-7058-453d-846b-6de1aa6c607a/resourceGroups/MC_ss4bd-aks-deployment-sample_ggssample_eastus/providers/Microsoft.Network/virtualNetworks/aks-vnet-42915476/subnets/aks-subnet"
    ```
  - Add a network rule for a virtual network and subnet. 
    ```
    az storage account network-rule add --account-name ggsdataaccount --subnet "/subscriptions/385ad333-7058-453d-846b-6de1aa6c607a/resourceGroups/MC_ss4bd-aks-deployment-sample_ggssample_eastus/providers/Microsoft.Network/virtualNetworks/aks-vnet-42915476/subnets/aks-subnet"
    ```
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
      name: azure-file-storage-secret
      namespace: default
   ```  
#### 4 Create [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/) to access Azure File Share and Blob storage.

  - Create secret with Azure Blob storage account details to access .spd files from Azure Blob Storage.
    ```
     kubectl create secret generic azure-storage-secret --from-literal=azurestorageaccountname="@STORAGE_ACCOUNT_NAME@" --from-literal=azurestorageaccountkey="@STORAGE_ACCOUNT_NAME@" 
    ```

#### 5. Add the Geocoding application Docker image URI.
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

