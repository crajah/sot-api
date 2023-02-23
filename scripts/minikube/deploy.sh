#!/usr/bin/env bash

minikube start

minikube addons enable dashboard

minikube addons enable ingress

sleep 10s

kubectl create secret docker-registry gcr --docker-server=https://gcr.io --docker-username=oauth2accesstoken --docker-password="$(gcloud auth print-access-token)" --docker-email=dainslie@gmail.com

sleep 10s

kubectl patch serviceaccount default -p '{"imagePullSecrets": [{"name": "gcr"}]}'

kubectl create -f deployment.yaml

kubectl create -f service.yaml

kubectl create -f service-management.yaml

sleep 20s

kubectl get pods

minikube dashboard