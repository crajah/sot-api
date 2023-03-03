SOT API
=======

Installation requirements
-------------------------
On Mac:
```
$ brew update && brew install scala && brew install sbt && brew install kubernetes-cli && brew install kubectl && brew cask install virtualbox docker minikube
```

Swagger
-------
This API has the associate Swagger named **reflex_api_v&lt;version>.yaml**.
Copy and paste the file into [Swagger Editor](https://proximi.io/rest-api) for easier viewing.

Unit Testing
------------
```
$ sbt test
```

Integration Testing
-------------------
```
$ sbt it:test
```

Gatling
-------
Gatling tests can act as performance tests and acceptance tests (thus providing regression tests).
```
$ sbt gatling-it:test
```

Release
-------
```
$ sbt "release with-defaults"
```

Run Application Locally
-----------------------
Within the root directory of this project we can run the application using "sbt run".
However, as the application creates directories/files running outside a Docker container will cause issues in a local environment.
In this case we want to only create necessary directories and files within a "temp" directory in this project.
That being the case, then run locally with the following, noting the use of the "integration testing configuration":
```
$ sbt '; set javaOptions += "-Dconfig.file=./src/main/resources/application.local.conf"; run'
```

Application Endpoint Examples
-----------------------------
Health:
```
$ curl http://localhost:8880
"Works" 
```

Rule status:
```
$ curl http://localhost:8880/sot-api/2/rule/my-rule/status
{"content":{"error-message":"Non existing rule: my-rule"},"status":{"code":404}} 
```

GCP Endpoints
-------------
```
$ gcloud endpoints services deploy sot-api/swagger/reflex_api_v1.0.0.yaml

$ gcloud endpoints operations describe operations/rollouts.bi-crm-poc.appspot.com:2018-01-24r0

$ gcloud endpoints configs list --service=bi-crm-poc.appspot.com
```

Docker Compose
--------------
Using "sbt docker compose" plugin.

To use locally built images for all services defined in the Docker Compose file instead of pulling from the Docker Registry use the following command:

```
$ sbt "dockerComposeUp skipPull"
```

To shutdown all instances started from the current project with the Plugin enabled run:

```
$  sbt dockerComposeStop
```

Kubernetes
----------
Currently we can deploy to Kubernetes locally via Minikube or GCP, as shown below.

On GCP, one can simply use Google Cloud Shell, or in a local shell the correct context must be set, which is also true when using Minikube.
```
$ kubectl config get-contexts
CURRENT   NAME                                   CLUSTER                                AUTHINFO                               NAMESPACE
          gke_bi-crm-poc_europe-west2-a_pai-np   gke_bi-crm-poc_europe-west2-a_pai-np   gke_bi-crm-poc_europe-west2-a_pai-np   bi-crm-dev
*         minikube                               minikube                               minikube
```

In a local shell, GCP can be your context via:
```
$ kubectl config use-context gke_bi-crm-poc_europe-west2-a_pai-np

$ kubectl config get-contexts
CURRENT   NAME                                   CLUSTER                                AUTHINFO                               NAMESPACE
*         gke_bi-crm-poc_europe-west2-a_pai-np   gke_bi-crm-poc_europe-west2-a_pai-np   gke_bi-crm-poc_europe-west2-a_pai-np
          minikube                               minikube                               minikube
```

Note that the following may have to be run regarding GCP authorization:
```
$ gcloud container clusters get-credentials pai-np --zone=europe-west2-a --project=bi-crm-poc
```

In a local shell, Minikube can be your context via:
```
$ kubectl config use-context minikube

$ kubectl config get-contexts
CURRENT   NAME                                   CLUSTER                                AUTHINFO                               NAMESPACE
          gke_bi-crm-poc_europe-west2-a_pai-np   gke_bi-crm-poc_europe-west2-a_pai-np   gke_bi-crm-poc_europe-west2-a_pai-np
*         minikube                               minikube                               minikube
```

**Command examples**
```
Get pods
$ kubectl --all-namespaces=true get pods

Get services
$ kubectl --all-namespaces=true get services

See logs of API service
$ kubectl logs sot-api-2205670870-d1c3c --namespace=bi-crm-dev -c sot-api
```

Minikube
--------
Deploy locally by running:
```
$ ./scripts/minikube/deploy.sh
```

Services:
```
$ kubectl get services
NAME                   TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)          AGE
kubernetes             ClusterIP   10.96.0.1        <none>        443/TCP          3m
sot-api                NodePort    10.111.199.173   <none>        8880:30591/TCP   3m
sot-api-admin          NodePort    10.108.238.128   <none>        9990:32610/TCP   3m
```

Access API:
```
$ minikube service --url sot-api
http://192.168.99.100:30591
```

```
$ curl -v $(minikube service --url sot-api)
...
< HTTP/1.1 200 OK
...
"Works"
```

```
$ curl -v $(minikube service --url sot-api)/sot-api/2/my-rule/status
...
< HTTP/1.1 404 Not Found
```

Access Admin by acquiring the Admin URL to then be viewed in browser:
```
$ minikube service --url sot-api-admin
http://192.168.99.100:32610
```

GCP
---
If more than one container clusters exist, kubectl needs to be configured for the cluster to be managed.
Using gcloud (command-line tool for Google Cloud Platform), run the following command to configure kubectl to a specific cluster e.g.
```
$ gcloud container clusters get-credentials pai-np --zone europe-west1-d
```

```
$ kubectl --all-namespaces=true get services
NAMESPACE     NAME                   TYPE           CLUSTER-IP     EXTERNAL-IP     PORT(S)          AGE
bi-crm-dev    sot-api                LoadBalancer   10.3.252.65    35.189.68.118   8880:31820/TCP   44d
bi-crm-dev    sot-api-admin          NodePort       10.3.242.133   <none>          9990:31752/TCP   44d
```

```
$ kubectl --namespace=bi-crm-dev get services
sot-api               LoadBalancer   10.3.252.65    35.189.68.118   8880:31820/TCP   44d
sot-api-admin         NodePort       10.3.242.133   <none>          9990:31752/TCP   44d
```

Take a look at the logs:
```
$ kubectl --namespace=bi-crm-dev get pods
NAME                                    READY     STATUS    RESTARTS   AGE
busybox-4158955249-415gt                1/1       Running   0          12d
sot-api-1212597424-j7wtf                1/1       Running   0          2d

$ kubectl --namespace=bi-crm-dev logs -f sot-api-1881222697-2sp7m sot-api
17:39:21.311 [main] DEBUG io.netty.util.internal.logging.InternalLoggerFactory - Using SLF4J as the default logging framework
17:39:21.417 [main] DEBUG io.netty.util.ResourceLeakDetector - -Dio.netty.leakDetection.level: simple
.......
```
Or proxy with kubectl to see logs through dashboard UI:
```
$ kubectl proxy

In browser, go to URL:
http://127.0.0.1:8001/api/v1/proxy/namespaces/kube-system/services/kubernetes-dashboard/#!/node?namespace=default

```

To see the complete deployment script (generated from a given yaml):
```
$ kubectl --namespace=bi-crm-dev get pods <pod name> -o yaml
```