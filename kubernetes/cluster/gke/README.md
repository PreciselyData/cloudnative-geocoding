# Google GKE Cluster Setup Guide

[Google Kubernetes Engine (GKE)](https://cloud.google.com/kubernetes-engine) is Google's secured and managed Kubernetes service with four-way autoscaling and multi-cluster support. This readme provides the steps to create a GKE cluster to deploy the Geocoding application built with *Spectrum Operational Addressing SDK*. 

## Prerequisites
The Geocoding sample on Google GKE requires access to [Cloud Storage buckets](https://cloud.google.com/storage/docs/creating-buckets) and [Google Container Registry (GCR)](https://cloud.google.com/container-registry). Google Cloud Storage (GS) is used to store the reference datasets, and the GCR repository contains the Geocoding application's Docker image which is used for the deployment. 

Running the Geocoding application on GKE requires permissions on these Google Cloud resources along with some others listed below.

### GCP IAM Permissions
To deploy the Geocoding application on a GKE cluster, make sure you have the following IAM roles and permissions:
   * `roles/container.admin` -  to create and manage a GKE cluster
   * `roles/iam.serviceAccountUser` - to assign a service account to Nodes 
   * `roles/storage.admin` - to read/write  data in Google Storage
   * `roles/file.editor` - to read/write data from Google Filestore

For more details about IAM roles and permissions, see Google's [documentation](https://cloud.google.com/iam/docs/understanding-roles).

## Create the cluster
Before starting the following steps, make sure you have installed the required tools listed in [Install client tools](../../README.md).	

### Authenticate and configure gcloud
Replace the `@KEY_FILE@` parameter with the absolute path to your service account key file, and execute the command below. For more options for authentication, refer to the [Google Cloud documentation](https://cloud.google.com/sdk/gcloud/reference/auth).
``` 
gcloud auth activate-service-account  --key-file=@KEY_FILE@ 
``` 
Configure a GCP project ID to create the Filestore instance; otherwise, you will have to provide this project ID in each command. We are using `oas-demo` as the project ID.
```
gcloud config set project oas-demo
```
### Create the GKE cluster
To create the cluster, use this command:   
``` 
gcloud container clusters create oas-sample --disk-size=200G --zone us-east1-c --machine-type n1-standard-8 --num-nodes 1 --enable-autoscaling --min-nodes 1 --max-nodes 10 --node-labels=node-app=ggs
```  
  
### Create a node pool for NGINX Ingress
To create a node pool for the NGINX Ingress controller, use this command:
``` 
gcloud container node-pools create ingress-pool --cluster oas-sample --machine-type n1-standard-4 --num-nodes 1 --zone us-east1-c --node-labels=node-app=ingress
``` 

### Verify the cluster 
To verify that your cluster was created successfully, and your kubectl client is pointing to the cluster, execute this command:

```
kubectl cluster-info
```
Your output should be similar to this:
```
Kubernetes control plane is running at https://35.229.88.97
GLBCDefaultBackend is running at https://35.229.88.97/api/v1/namespaces/kube-system/services/default-http-backend:http/proxy
KubeDNS is running at https://35.229.88.97/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy
Metrics-server is running at https://35.229.88.97/api/v1/namespaces/kube-system/services/https:metrics-server:/proxy

To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
```  
### Configure the `kubectl` client (optional)
To manage the GKE cluster, configure your `kubectl` CLI  to point your cluster.
The `gcloud` command automatically configures the `kubectl` CLI after creating the cluster. If you want to manage the cluster from a different machine, use this command to configure it: 
```
gcloud container clusters get-credentials oas-sample --zone us-east1-c
``` 
For more information about the `kubectl` configuration, refer to [Google's documentation](https://cloud.google.com/kubernetes-engine/docs/how-to/cluster-access-for-kubectl). 