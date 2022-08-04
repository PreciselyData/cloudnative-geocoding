# Geotax Reference Data on Amazon Elastic File System (EFS)

This sample demonstrates using a [persistent volume](https://kubernetes.io/docs/concepts/storage/persistent-volumes/) backed by [Amazon EFS](https://aws.amazon.com/efs/) to store the reference data which will be accessed by the Geotax application at runtime. To initialize the persistent volume, it is mounted to a separate, temporary, deployment of the Geotax application – the “staging” deployment.  When the staging deployment starts, the data is copied from S3 and extracted to the persistent volume.  Once the data has been initialized, the staging deployment can be stopped and the temporary resources can be deleted.  The Geotax application will then access the reference data by mounting the same persistent volume that had just been initialized by the staging deployment.

This reference data deployment process needs to be executed only once for the Geotax application deployment. You only need to re-run it when you want to update the deployed data, such as with a new vintage.  At that time, you can use the staging process to prepare a new, separate, persistent volume and then update your running deployment of the Geotax application to use that new persistent volume with zero application downtime.

## Create and configure an EFS file system
The following directions will guide you through the process of preparing an EFS file system for your deployment by using the AWS CLI.  If you have already created and configured an EFS file system that you want to use, and it is accessible from your EKS cluster, then you can skip this step and move on to the next.

#### 1. Deploy the CSI Driver - this will be used to mount EFS storage with a persistent volume.
```
helm repo add aws-efs-csi-driver https://kubernetes-sigs.github.io/aws-efs-csi-driver/
helm repo update
helm install aws-efs-csi-driver aws-efs-csi-driver/aws-efs-csi-driver
```

#### 2. Create the EFS file system.
If you have already created & configured an instance of the EFS file system, and it is accessible from your EKS cluster, then you can ignore this step and move to the next step.

- Create the EFS file system.
  
   We are using `gtx-sample-efs` as the name for the file system; if you want to use a different name, then you can update the following command accordingly.
    
   ```
   aws efs create-file-system --throughput-mode provisioned --provisioned-throughput-in-mibps 100 --tags "Key=Name,Value=gtx-sample-efs" --creation-token gtx-sample-efs
   ```
  Your output should be similar to this:

   ```
  {
    "OwnerId": "603016229198",
    "CreationToken": "gtx-sample-efs",
    "FileSystemId": "fs-0f2fc14bcf6d0a020",
    "FileSystemArn": "arn:aws:elasticfilesystem:us-east-1:603016229198:file-system/fs-0f2fc14bcf6d0a020",
    "CreationTime": "2022-07-25T15:47:24+05:30",
    "LifeCycleState": "creating",
    "Name": "gtx-sample-efs",
    "NumberOfMountTargets": 0,
    "SizeInBytes": {
        "Value": 0,
        "ValueInIA": 0,
        "ValueInStandard": 0
    },
    "PerformanceMode": "generalPurpose",
    "Encrypted": false,
    "ThroughputMode": "provisioned",
    "ProvisionedThroughputInMibps": 100.0,
    "Tags": [
        {
            "Key": "Name",
            "Value": "gtx-sample-efs"
        }
      ]
    }
    ```
   **Note:** It is recommended that you use [Provisioned throughput mode](https://docs.aws.amazon.com/efs/latest/ug/performance.html) for consistent performance of the application.
    
- Query the FileSystemId for your EFS file system:
   ```
   aws efs describe-file-systems --creation-token gtx-sample-efs --query "FileSystems[0].FileSystemId" --output text
   ``` 
   Your output should be similar to this:
   ```
   fs-0f2fc14bcf6d0a020
   ``` 

- Query the VPC ID of your EKS cluster:
 
   ```
   aws eks describe-cluster --name gtx-sample --query "cluster.resourcesVpcConfig.vpcId" --output text
   ```
   Your output should be similar to this:
   ```
   vpc-0e79c569388587b66
   ```
 - Use your VPC ID to query the CIDR block of your cluster:
  
   ```
   aws ec2 describe-vpcs --vpc-ids vpc-0e79c569388587b66 --query "Vpcs[].CidrBlock" --output text
   ```
   Your output should be similar to this:
   ```
   192.168.0.0/16
   ```

 - Create a security group to access the EFS file system from your EKS cluster:

   ```
   aws ec2 create-security-group --description "To access EFS in EKS"  --vpc-id vpc-0e79c569388587b66 --group-name gtx-sample-nfs
   ```
    Your output should be similar to this:
   ```
   {
       "GroupId": "sg-07ab84810122e4649"
   }
   ```
 - Configure the security group to allow inbound NFS traffic from your EKS cluster:  
   ```
   aws ec2 authorize-security-group-ingress --group-id sg-07ab84810122e4649 --cidr 192.168.0.0/16 --protocol tcp --port 2049
   ```
 - Query the subnets of your EKS cluster:
   ```
   aws eks describe-cluster --name gtx-sample --query "cluster.resourcesVpcConfig.subnetIds"
   ```
   Your output should be similar to this:
   ```
   [
       "subnet-044d57f4be9e7276c",
       "subnet-0f2dbd354391aadc8",
       "subnet-066ffaf829c412f3c",
       "subnet-0829782dbcc49e12d",
       "subnet-05b5c1f185b2b4c1c",
       "subnet-0c9468f25d184ccd7"
   ]
   ```
 - Configure an EFS mount target for each subnet.
      
   Execute the following commands by providing the file system ID, subnet ID, and security group:
   ```
   aws efs create-mount-target --file-system-id fs-0f2fc14bcf6d0a020 --subnet-id subnet-044d57f4be9e7276c --security-groups sg-07ab84810122e4649
   aws efs create-mount-target --file-system-id fs-0f2fc14bcf6d0a020 --subnet-id subnet-0f2dbd354391aadc8 --security-groups sg-07ab84810122e4649
   aws efs create-mount-target --file-system-id fs-0f2fc14bcf6d0a020 --subnet-id subnet-066ffaf829c412f3c --security-groups sg-07ab84810122e4649
   aws efs create-mount-target --file-system-id fs-0f2fc14bcf6d0a020 --subnet-id subnet-0829782dbcc49e12d --security-groups sg-07ab84810122e4649
   aws efs create-mount-target --file-system-id fs-0f2fc14bcf6d0a020 --subnet-id subnet-05b5c1f185b2b4c1c --security-groups sg-07ab84810122e4649
   aws efs create-mount-target --file-system-id fs-0f2fc14bcf6d0a020 --subnet-id subnet-0c9468f25d184ccd7 --security-groups sg-07ab84810122e4649
   ```
   **Note:** The Amazon [documentation](https://docs.aws.amazon.com/efs/latest/ug/mounting-fs.html) recommends that you wait 90 seconds after creating a mount target before you access these mount targets in the persistent volumes. This wait lets the DNS records propagate fully in the AWS Region where the file system is located.
#### 3. Update the persistent volume resource definition to use your EFS file system.
If you don’t have your EFS FileSystemId, see “Query the FileSystemId for your EFS file system” above.

  In the `./gtx/nfs-data/eks/gtx-data-pv.yaml` file, replace:
   - `@FILE_SYSTEM_ID@` - your FileSystemId 
   - `@REGION@` - the region of your EFS file system 
  ```
   csi:
       driver: efs.csi.aws.com
       volumeHandle: fs-0f2fc14bcf6d0a020.efs.us-east-1.amazonaws.com
       volumeAttributes:
       path: /
   ```  
#### 4. Add the Geotax application Docker image URI.
In the `./gtx/nfs-data/gtx-staging.yaml` file, replace:
- `@IMAGE_URI@` - the URI of the Geotax application Docker image stored in the ECR Repository in the `image` parameter. The `@IMAGE_URI@` parameter needs to be replaced in two places.
  ```
  initContainers:
     - name: gtx-dataprep-container
       image: @IMAGE_URI@
  ```
  and
  ```
   containers:
      - name: gtx-container
        image: @IMAGE_URI@
  ```  
## Start the staging deployment
If you haven't already deployed the geotax datasets, and data preparation script config maps, then deploy the following manifest files:
```
kubectl apply -f ./gtx/gtx-datasets-cm.yaml
kubectl apply -f ./gtx/gtx-dataprep-cm.yaml
```
Create the persistent volume and staging deployment: 
```
kubectl apply -f ./gtx/nfs-data/eks/gtx-data-pv.yaml
kubectl apply -f ./gtx/nfs-data/gtx-staging.yaml
```
After these commands execute, the staging deployment will start and the init container used in the staging deployment will copy the data to the persistent volume.  Once the staging deployment enters the “running” state, the copy is complete. Depending on the size of the reference datasets, this process may take some time.

You can check the status of the pods using this command:
```
kubectl get pods -w
```
To monitor the progress of the data deployment, you can check the logs of the staging pod using this command:
```
kubectl logs -f -l app=gtx-dataprep -c gtx-dataprep-container
```

## Verify the reference data deployment
To verify that the reference data successfully deployed, you can issue a request to the Geotax service.

1. Locate your service's external URL and port number:
    ```
    kubectl get services -l app=gtx-dataprep
    ```
2. Use the external URL and port number to access the Geotax application test page in a browser.
The URL should be formatted like this:
              
    `http://<External-IP>:<port>/`             
         
    For example: http://ac97c5928849311ea9e8602781e57924-913734340.us-east-1.elb.amazonaws.com/
	   
## Delete the staging resources
After verifying the data, the staging resources are no longer needed and can be deleted using this command:
```
kubectl delete -f ./gtx/nfs-data/gtx-staging.yaml
```
## Next step
Now that the persistent volume has been created and the reference data has been configured on your EFS file system, you can mount the persistent volume to use that data in your [Geotax application deployment](../../README.md).

