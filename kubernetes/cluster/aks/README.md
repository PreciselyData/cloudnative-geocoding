# Azure AKS Cluster Setup Guide

[Azure Kubernetes Service (AKS)](https://azure.microsoft.com/en-in/services/kubernetes-service/) is Microsoft's secured and managed Kubernetes service. This readme provides the steps to create a AKS cluster to deploy the Geocoding application built with *Spectrum Operational Addressing SDK*. 

## Prerequisites
The Geocoding sample on Microsoft AKS requires access to [Azure Files Storage](https://azure.microsoft.com/en-in/services/storage/files/), [Azure Container Registry (ACR)](https://azure.microsoft.com/en-us/services/container-registry/) and [Azure Blob Storage](https://azure.microsoft.com/en-in/services/storage/blobs/). Azure Blob Storage is used to store the reference datasets in .spd file format, and the ACR repository contains the Geocoding application's Docker image which is used for the deployment. 

Running the Geocoding application on AKS requires permissions on these Microsoft's Azure Cloud resources along with some others listed below.

### Required Permissions & roles
 - `Contributor` role to create AKS cluster
 - `Azure Blob Storage Reader`  role to download .spd files from Azure Blob Storage
 - `Storage File Data SMB Share Contributor` role to read, write, and delete files from Azure Storage file shares over SMB/NFS

## Create the cluster
Before starting the following steps, make sure you have installed the required tools listed in [Install client tools](../../README.md).	

### Authenticate Azure CLI

Azure CLI supports multiple authentication methods; use any authentication method to sign in. For details about Azure's authentication types see their [documentation](https://docs.microsoft.com/en-us/cli/azure/authenticate-azure-cli).

``` 
az login 
``` 

If your Azure account has multiple subscription IDs then set one ID as default subscription ID, that will be used for all `azure CLI` commands, otherwise you will have to provide this subscription ID in each command.
```
az account set --subscription "@SUBSCRIPTION_ID@"
```
Configure your resource group, this will be used to create all resources- cluster and storage account, otherwise you will have to provide it in each command.
```
az configure --defaults group=@RESOURCE_GROUP@
```
### Create the AKS cluster
To create the cluster, use this command:   
``` 
az aks create --name oassample --attach-acr @ACR_REPOSITORY@ --enable-cluster-autoscaler --min-count 1 --max-count 10 --node-osdisk-type Managed --node-osdisk-size 100 --node-vm-size Standard_DS4_v2  --nodepool-labels node-app=ggs 
```  
  
### Create a node pool for NGINX Ingress
To create a node pool for the NGINX Ingress controller, use this command:
``` 
az aks nodepool add --cluster-name oassample --name ingress --labels node-app=ingress --node-count 1 --node-vm-size Standard_DS4_v2 --node-osdisk-size 50
``` 

### Configure the `kubectl` client 
To manage the AKS cluster, configure your `kubectl` CLI  to point your cluster:
```
az aks get-credentials --name oassample
``` 
For more information about the `kubectl` configuration, refer to [Microsoft's documentation](https://docs.microsoft.com/en-us/azure/aks/kubernetes-walkthrough).

### Verify the cluster 
To verify that your cluster was created successfully, and your kubectl client is pointing to the cluster, run this command:

```
kubectl cluster-info
```
Your output should be similar to this:
```
Kubernetes control plane is running at https://oassample-ss4bd-aks-deploy-385ad3-7cdcfd59.hcp.eastus.azmk8s.io:443
CoreDNS is running at https://oassample-ss4bd-aks-deploy-385ad3-7cdcfd59.hcp.eastus.azmk8s.io:443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy
Metrics-server is running at https://oassample-ss4bd-aks-deploy-385ad3-7cdcfd59.hcp.eastus.azmk8s.io:443/api/v1/namespaces/kube-system/services/https:metrics-server:/proxy 

To further debug and diagnose cluster problems, use `kubectl cluster-info dump`.
``` 

