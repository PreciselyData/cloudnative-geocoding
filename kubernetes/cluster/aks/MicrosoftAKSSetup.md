# Azure AKS Cluster Setup Guide

Azure Kubernetes Service (AKS) is Microsoft's secured and managed Kubernetes service with four-way autoscaling and multi-cluster support. This readme provides the steps to create a AKS cluster to deploy the Geocoding application built with Spectrum Global Geocoding SDK. 

## Prerequisites
The Geocoding sample on Microsoft AKS requires access to [Azure Files Storage](https://azure.microsoft.com/en-in/services/storage/files/), [Azure Container Registry (ACR)](https://azure.microsoft.com/en-us/services/container-registry/). [Azure Blob Storage](https://azure.microsoft.com/en-in/services/storage/blobs/) is used to store the reference datasets in .spd file format, and the ACR repository contains the Geocoding application's Docker image which is used for the deployment. 

To run the Geocoding application on AKS requires permissions on these Microsoft's Azure Cloud resources along with some others listed below.

### Required Permissions
To deploy the Geocoding application on a AKS cluster, make sure you have at least `Contributor` roles and permissions.

## Create the cluster
Before starting the following steps, make sure you have installed the required tools listed in [Install client tools](../../README.md).	

### Authenticate and configure gcloud
Replace the parameters with the values from  your service principal.
``` 
az login --service-principal -u @APP_ID@ -p @SECRET@ --tenant @TENANT_ID@
``` 
**Note:** For other methods for azure CLI authntication, you can follow [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/authenticate-azure-cli) documentations.

If your Azure account has multiple subscription IDs than configure a Azure subscription ID that will be used  to create the Azure Files Storage instance; otherwise, you will have to provide this subscription ID in each command. We are using `Precisely Gold Sponsorship` as the subscription ID.
```
az account set --subscription "Precisely Gold Sponsorship"
```
### Create the AKS cluster
To create the cluster, use this command:   
``` 
az aks create --name ggssample --resource-group @RESOURCE_GROUP@ --attach-acr @ACR_REPOSITORY@ --enable-cluster-autoscaler --min-count 1 --max-count 2 --node-osdisk-type Managed --node-osdisk-size 100 --node-vm-size Standard_DS4_v2  --nodepool-labels node-app=ggs
```  
  
### Create a node pool for NGINX Ingress
To create a node pool for the NGINX Ingress controller, use this command:
``` 
az aks nodepool add --cluster-name ggssample --name ingress --resource-group @RESOURCE_GROUP@ --labels node-app=ingress --node-count 1 --node-vm-size Standard_DS4_v2 --node-osdisk-size 50
``` 

### Configure the `kubectl` client 
To manage the AKS cluster, configure your `kubectl` CLI  to point your cluster.
The `gcloud` command automatically configures the `kubectl` CLI after creating the cluster. If you want to manage the cluster from a different machine, use this command to configure it:
```
az aks get-credentials --resource-group @RESOURCE_GROUP@ --name ggssample --overwrite-existing
``` 
For more information about the `kubectl` configuration, refer to [Google's documentation](https://cloud.google.com/kubernetes-engine/docs/how-to/cluster-access-for-kubectl).

### Verify the cluster 
To verify that your cluster was created successfully, and your kubectl client is pointing to the cluster, execute this command:

```
kubectl cluster-info
```
Your output should be similar to this:
```
Kubernetes master is running at https://ggssample-ss4bd-aks-deploy-385ad3-47738680.hcp.eastus.azmk8s.io:443
CoreDNS is running at https://ggssample-ss4bd-aks-deploy-385ad3-47738680.hcp.eastus.azmk8s.io:443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy
Metrics-server is running at https://ggssample-ss4bd-aks-deploy-385ad3-47738680.hcp.eastus.azmk8s.io:443/api/v1/namespaces/kube-system/services/https:metrics-server:/proxy   
```  

To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.

