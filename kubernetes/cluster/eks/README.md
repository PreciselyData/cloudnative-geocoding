# Amazon EKS Cluster Setup Guide 
The Amazon Elastic Kubernetes Service (EKS) is a fully managed Kubernetes service. This readme provides the steps to create an EKS cluster to deploy the Geocoding application built with Spectrum Global Geocoding SDK.

## Prerequisites

The Geocoding sample on Amazon EKS requires acess to [S3](https://aws.amazon.com/s3/) and [Amazon Elastic Container Registry (ECR)](https://docs.aws.amazon.com/AmazonECR/latest/userguide/Registries.html). AWS S3 is used to store the reference datasets, and the AWS ECR repository contains the Geocoding application Docker image which is used by the application during runtime. 

To run the Geocoding application in EKS requires permissions on these AWS resources along with some others listed below.

### AWS IAM Permissions
To deploy the Geocoding application on an EKS cluster, make sure you have the necessary permissions listed below:

   * Permissions to create and manage the EKS cluster
   * [AmazonEKSClusterPolicy](https://docs.aws.amazon.com/eks/latest/userguide/service_IAM_role.html)
   * [Docker image push/pull permissions](https://docs.aws.amazon.com/AmazonECR/latest/userguide/ECR_on_EKS.html) on AWS ECR 
   * [Permissions to create the file system, mount the target, and read/write on EFS](https://docs.aws.amazon.com/efs/latest/ug/access-control-managing-permissions.html) 
   * [Read/write permissions](https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_examples_s3_rw-bucket.html) on Amazon S3

## Create the cluster	
Before starting the following steps, make sure you have installed the required tools listed in [Install client tools](../../README.md). 

In the sample, we are using `ggs-cluster` for the cluster name, and `us-east-1` for the region. You can change these values for your environment in the [create-eks-cluster.yaml](create-eks-cluster.yaml) and [cluster-autoscaler.yaml](cluster-autoscaler.yaml) manifest files.

### Create the EKS cluster 
Before running the `eksctl` command, make sure that you have authenticated to AWS and valid AWS credentials are configured. If your AWS credentials haven't been configured, then use the following command and provide the required input:
```
aws configure
```
Create the cluster and autoscaler by executing the following commands:
```
eksctl create cluster -f create-eks-cluster.yaml
kubectl apply -f cluster-autoscaler.yaml
```

### Verify the cluster 
To verify that your cluster was created successfully, and your kubectl client is pointing to the cluster, execute this command:

```
kubectl cluster-info
```
Your output should be similar to this:
```
Kubernetes master is running at https://D2A4776802433A6463A156F0FEA975A4.yl4.us-east-2.eks.amazonaws.com
CoreDNS is running at https://D2A4776802433A6463A156F0FEA975A4.yl4.us-east-1.eks.amazonaws.com/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy

To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
```  

### Configure the `kubectl` client (optional)
To manage the EKS cluster, configure your `kubectl` CLI  to point to your cluster. The `eksctl` command automatically configures the `kubectl` CLI after creating the cluster. If you want to manage the cluster from a different machine, use this command to configure it:
```
aws eks --region us-east-1 update-kubeconfig --name ggs-sample
```
For more information about the `kubectl` configuration, refer to the [AWS documentation](https://docs.aws.amazon.com/eks/latest/userguide/create-kubeconfig.html). 