# Geocoding Deployment for Kubernetes

This sample demonstrates the deployment of the Spectrum Global Geocoding API in a cloud native environment.  It provides elastic REST endpoints for geocoding, reverse geocoding, interactive geocoding, and key lookup that scale based on the number of active connections.

- [Geocoding Application](#geocoding-application)
  - [Features](#features)
- [Architecture](#architecture)
  - [Cluster components](#cluster-components)
  - [Using a Staging Service to install reference data](#using-a-staging-service-to-install-reference-data)
  - [Reference data storage](#geocoding-reference-data-storage)
- [Cluster Performance Metrics and Autoscaling](#cluster-performance-metrics-and-autoscaling)
  - [Metrics monitoring](#metrics-monitoring-active-number-of-connections)
  - [Autoscaling](#autoscaling)
  - [Performance tuning the US geocoder](#performance-tuning-the-US-geocoder)


# Geocoding Application

The Spectrum Global Geocoding SDK (GGS) provides the following capabilities, which are available as REST web services in the Geocoding application:

-   **Geocode Service** - performs forward geocoding using input addresses and returning location data and other information.
-   **Reverse Geocode Service** - performs reverse geocoding using input coordinates and returns address information that is the best match for that point.
-   **Interactive Service** - suggests addresses and place names as you type.
-  **Key Lookup Service** - returns geocoded candidates when given a unique key. It is a more efficient method than matching with an address, as the key is unique to that address. Spectrum Global Geocoding supports the PreciselyID unique identifier for US data and the G-NAF key for Australian data.

**Note**: The sample requires using Spectrum Global Geocoding SDK (GGS) version 3.1.71 or above.

## Features
-   Options that allow control of the searching and matching options, output results, and other preferences
-   Highly configurable cluster using declarative configuration files
-   Load balancing
-   Cluster and pod autoscaling

For complete Global Geocoding SDK documentation, see the [GGS REST and Java API Developer Guide](https://support.pb.com/help/ggs/3.0/en/webhelp/index.html).

# Architecture

The diagrams in this section illustrate the architecture of the Geocoding application deployed in a AWS-hosted environment. The architecture and functionality is similar in the Google Cloud environment except for the naming of some of the cluster components. 

## Cluster components

The configured cluster includes:

-   a minimum of two worker nodes
-   [NGINX](https://www.nginx.com/) for ingress monitoring and metrics
-   [Prometheus](https://prometheus.io/) for metrics collection
- [Prometheus-Adapter](https://github.com/DirectXMan12/k8s-prometheus-adapter) for serving custom metrics
-   Horizontal Pod Autoscaler \(HPA\)
-   Kubernetes Cluster Autoscaler
-   Geocoding reference data installed locally or on cloud-hosted storage
-   the deployed Geocoding application

**Note:** Since cloud environments may have individual corporate requirements for naming conventions, security, and components, some adjustments will most likely be necessary.

## Reference data storage

The geocoding reference data must be available on the file system of the Pod running the Geocoding application. This sample provides two approaches for deploying the reference data: locally and cloud-hosted. Both of these approaches involve attaching an NFS endpoint to the deployment definitions to access the data at runtime.

### Local data storage
In this diagram, the geocoding reference data is deployed to the Geocoding Service Pod using an [emptyDir](https://kubernetes.io/docs/concepts/storage/volumes/#emptydir)
 volume, which is provided fresh for every restart. 

![Geocoding application in a AWS-hosted environment using local data storage](/images/architecture_aws_localdata.png)

### Cloud-hosted storage
In this diagram, the geocoding reference data is deployed to the Geocoding Service Pod using a [persistent volume](https://kubernetes.io/docs/concepts/storage/persistent-volumes/). The persistent volume is backed by an Amazon Elastic File System (EFS) file system and the data is ready to use immediately when the volume is mounted to the pods.
![Geocoding application in a AWS-hosted environment using cloud-hosted storage](/images/architecture_aws_efs.png)

#### Using a staging service to install reference data
The staging service is a single pod used for preparing the geocoding reference data for use on cloud storage; this eliminates manual data preparation for the initial installation as well as any future data updates. Upon startup, the staging service takes the listed SPD datasets and unpacks them to the data storage location so that the data is available immediately when the runtime cluster launches. After the data has been unpacked and tested, the staging service is no longer required and can be deleted.

Data updates are applied using a similar strategy: the cluster manifests for both staging and runtime can be updated with the latest SPDs in a new data location, and the staging service can be started while the existing runtime cluster is actively servicing requests. Using this approach results in data updates with zero downtime and very little manual intervention.

# Cluster Performance Metrics and Autoscaling 

To ensure high performance of the Geocoding application in the Kubernetes cluster, several components are used to monitor and scale the cluster based on user load:

-   **NGINX Ingress Controller** -  load balances and gathers metrics for the active HTTP connections
-   **Prometheus** - monitors and collects metrics
-   **Prometheus Adapter** - custom metrics server providing number of active connections to the HPA
-   **Horizontal Pod Autoscaler \(HPA\)** - scales the number of Pods in the Geocoding Service based on the number of active connections

![cluster performance monitoring and scaling](/images/nginx_ingress_load_balancer.png)

## Metrics monitoring

The *number of active connections* is a custom metric collected by the ingress controller and is used to scale the resources available for the Geocoding application. As the number of users connected to the ingress controller increases, the Geocoding application HPA will increase the number of available pods. When the load decreases, the number of pods will be automatically reduced.

A third-party NGINX Ingress Controller is used to manage the Ingress resources in the cluster. The Geocoding application requires a Kubernetes cluster with two worker nodes and a separate node for the NGINX Ingress Controller. For more information about the NGINX Ingress Controller, see [https://kubernetes.github.io/ingress-nginx/](https://kubernetes.github.io/ingress-nginx/).

Prometheus is installed for monitoring the application along with the Prometheus-Adapter to serve the custom metrics on which the Geocoding application will autoscale. For more information about Prometheus, see [https://prometheus.io/](https://prometheus.io/); for info on Prometheus-Adapter, see [https://github.com/DirectXMan12/k8s-prometheus-adapter](https://github.com/DirectXMan12/k8s-prometheus-adapter).

## Autoscaling

The Horizontal Pod Autoscaler automatically scales the number of pods requested based on the observed custom metrics utilization.  If the active nodes in the cluster cannot provide the resources requested to run the pods, the cluster autoscaler can scale the number of nodes up or down to match the load.

The number of active connections metric \(`nginx_active_connections`\) and its target value \(`targetAverageValue`\) are specified in the *ggs-hpa.yaml* resource manifest, which is used in the Geocoding application deployment process.

The following table provides the recommended HPA settings based on the countries used in the Geocoding application.

|Countries Supported|Recommended Settings for Number of Active Connections|
|:--------------------------:|:---------------------------------------------------:|
|USA only|18 - 20|
|International (with or without USA)|12|

---

## Performance tuning the US geocoder

The Geocoding application creates a pool of geocoder instances. The size of the pool is critical to the application's performance when geocoding US addresses. The runtime scripts internally adjust the size of the pool based on the available CPU resources; however, manually changing these settings is an option if you are modifying the resources for containers in a pod. Make sure you have reviewed the information in this section before you modify these parameter settings.


The following sections describe two geocoding settings:
  - POD_CPU_LIMIT
  - GGS_POOL_MAX_ACTIVE
  
### POD_CPU_LIMIT
We set this value automatically based on the pod's CPU resource limit. It is used to provide the default value for configuring the pool.

### GGS_POOL_MAX_ACTIVE
The CPU resources for your container(s) are automatically allocated by the script. This parameter can be used to override, but the default should be sufficient.
```
env:
  - name: GGS_POOL_MAX_ACTIVE
    value: 1
```
The recommended ratio for the pool setting is 1.5 US geocoder instances per allocated CPU.
- `GGS_POOL_MAX_ACTIVE`  =  ceiling ( CPU_LIMIT * 1.5 )

For example: If the CPU limit = 1.2, then:
  - `GGS_POOL_MAX_ACTIVE` = ceiling( 1.2 * 1.5 ) = 2 


[**Next**: Geocoding Application for Kubernetes Deployment Guide](kubernetes/README.md) 