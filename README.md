# Geotax Deployment for Kubernetes

This sample demonstrates the deployment of the Geotax API in a cloud native environment.  It provides elastic REST endpoints for Get Taxrate by Location, Post Taxrate by Location, Get Taxrate by Address, Post Taxrate by Address that scale based on the number of active connections.

- [Geotax Application](#geotax-application)
  - [Features](#features)
- [Architecture](#architecture)
  - [Cluster components](#cluster-components)
  - [Using a Staging Service to install reference data](#using-a-staging-service-to-install-reference-data)
  - [Reference data storage](#geotax-reference-data-storage)
- [Cluster Performance Metrics and Autoscaling](#cluster-performance-metrics-and-autoscaling)
  - [Metrics monitoring](#metrics-monitoring-active-number-of-connections)
  - [Autoscaling](#autoscaling)

# Geotax Application

The Geotax SDK provides the following capabilities, which are available as REST web services in the Geotax application:

- **Get Taxrate by Location** - Retrieves tax rates applicable to a specific location. This service accepts longitude, latitude and supported tax rate type as inputs to retrieve applicable tax rates.
- **Post Taxrate by Location** - This is a Batch offering for 'Taxrate By Location' service. It accepts a single location coordinate or a list of location coordinates and retrieve applicable tax rates.
- **Get Taxrate by Address**: Retrieves tax rates applicable to a specific address. This service accepts address and supported tax rate type as inputs to retrieve applicable tax rates.
- **Post Taxrate by Address** -This is a Batch offering for 'Taxrate By Address' service. It accepts a single address or a list of addresses and retrieve applicable tax rates.


## Features
-   Options that allow control of the searching and matching options, output results, and other preferences
-   Highly configurable cluster using declarative configuration files
-   Load balancing
-   Cluster and pod autoscaling

For complete Geotax SDK documentation, see the [Geotax SDK Developer Guide]() (to be inserted).

# Architecture

The diagrams in this section illustrate the architecture of the Geotax application deployed in a AWS-hosted environment. The architecture and functionality is similar in the Google Cloud and Microsoft Azure environments except for the naming of some of the cluster components. 

## Cluster components

The configured cluster includes:

-   A minimum of two worker nodes
-   [NGINX](https://www.nginx.com/) for ingress monitoring and metrics
-   [Prometheus](https://prometheus.io/) for metrics collection
-   [Prometheus-Adapter](https://github.com/DirectXMan12/k8s-prometheus-adapter) for serving custom metrics
-   Horizontal Pod Autoscaler \(HPA\)
-   Kubernetes Cluster Autoscaler
-   Geotax reference data installed locally or on cloud-hosted storage
-   The deployed Geotax application

**Note:** Since cloud environments may have individual corporate requirements for naming conventions, security, and components, some adjustments will most likely be necessary.

## Reference data storage

The geotax reference data must be available on the file system of the Pod running the Geotax application. This sample provides two approaches for deploying the reference data: locally and cloud-hosted. Both of these approaches involve attaching an NFS endpoint to the deployment definitions to access the data at runtime.

### Local data storage
In this diagram, the geotax reference data is deployed to the Geotax Service Pod using an [emptyDir](https://kubernetes.io/docs/concepts/storage/volumes/#emptydir)
 volume, which is provided fresh for every restart. 

![Geotax application in a AWS hosted environment using local data storage](/images/architecture_aws_localdata.png)

### Cloud-hosted storage
In this diagram, the geotax reference data is deployed to the Geotax Service Pod using a [persistent volume](https://kubernetes.io/docs/concepts/storage/persistent-volumes/). The persistent volume is backed by an Amazon Elastic File System (EFS) file system and the data is ready to use immediately when the volume is mounted to the pods.
![Geotax application in a AWS hosted environment using cloud-hosted storage](/images/architecture_aws_efs.png)

#### Using a staging service to install reference data
The staging service is a single pod used for preparing the geotax reference data for use on cloud storage; this eliminates manual data preparation for the initial installation as well as any future data updates. Upon startup, the staging service takes the listed SPD datasets and unpacks them to the data storage location so that the data is available immediately when the runtime cluster launches. After the data has been unpacked and tested, the staging service is no longer required and can be deleted.

Data updates are applied using a similar strategy: the cluster manifests for both staging and runtime can be updated with the latest SPDs in a new data location, and the staging service can be started while the existing runtime cluster is actively servicing requests. Using this approach results in data updates with zero downtime and very little manual intervention.

# Cluster Performance Metrics and Autoscaling 

To ensure high performance of the Geotax application in the Kubernetes cluster, several components are used to monitor and scale the cluster based on user load:

-   **NGINX Ingress Controller** -  load balances and gathers metrics for the active HTTP connections
-   **Prometheus** - monitors and collects metrics
-   **Prometheus Adapter** - custom metrics server providing number of active connections to the HPA
-   **Horizontal Pod Autoscaler \(HPA\)** - scales the number of Pods in the Geotax Service based on the number of active connections

![cluster performance monitoring and scaling](/images/nginx_ingress_load_balancer.png)

## Metrics monitoring

The *number of active connections* is a custom metric collected by the ingress controller and is used to scale the resources available for the Geotax application. As the number of users connected to the ingress controller increases, the Geotax application HPA will increase the number of available pods. When the load decreases, the number of pods will be automatically reduced.

A third-party NGINX Ingress Controller is used to manage the Ingress resources in the cluster. The Geotax application requires a Kubernetes cluster with two worker nodes and a separate node for the NGINX Ingress Controller. For more information about the NGINX Ingress Controller, see [https://kubernetes.github.io/ingress-nginx/](https://kubernetes.github.io/ingress-nginx/).

Prometheus is installed for monitoring the application along with the Prometheus-Adapter to serve the custom metrics on which the Geotax application will autoscale. For more information about Prometheus, see [https://prometheus.io/](https://prometheus.io/); for info on Prometheus-Adapter, see [https://github.com/DirectXMan12/k8s-prometheus-adapter](https://github.com/DirectXMan12/k8s-prometheus-adapter).

[**Next**: Geotax Application for Kubernetes Deployment Guide](kubernetes/README.md) 