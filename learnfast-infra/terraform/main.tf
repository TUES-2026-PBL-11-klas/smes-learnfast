# ──────────────────────────────────────────────
# 1. Kind Cluster
# ──────────────────────────────────────────────
resource "kind_cluster" "learnfast" {
  name           = var.cluster_name
  wait_for_ready = true

  kind_config {
    kind        = "Cluster"
    api_version = "kind.x-k8s.io/v1alpha4"

    node {
      role = "control-plane"
      kubeadm_config_patches = [
        <<-PATCH
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
        PATCH
      ]
      extra_port_mappings {
        container_port = 30080
        host_port      = 8080
        protocol       = "TCP"
      }
      extra_port_mappings {
        container_port = 30443
        host_port      = 8443
        protocol       = "TCP"
      }
    }

    node {
      role = "worker"
    }
  }
}

# ──────────────────────────────────────────────
# 2. Provider configuration (uses Kind kubeconfig)
# ──────────────────────────────────────────────
provider "kubernetes" {
  host                   = kind_cluster.learnfast.endpoint
  cluster_ca_certificate = kind_cluster.learnfast.cluster_ca_certificate
  client_certificate     = kind_cluster.learnfast.client_certificate
  client_key             = kind_cluster.learnfast.client_key
}

provider "kubectl" {
  host                   = kind_cluster.learnfast.endpoint
  cluster_ca_certificate = kind_cluster.learnfast.cluster_ca_certificate
  client_certificate     = kind_cluster.learnfast.client_certificate
  client_key             = kind_cluster.learnfast.client_key
  load_config_file       = false
}

provider "helm" {
  kubernetes {
    host                   = kind_cluster.learnfast.endpoint
    cluster_ca_certificate = kind_cluster.learnfast.cluster_ca_certificate
    client_certificate     = kind_cluster.learnfast.client_certificate
    client_key             = kind_cluster.learnfast.client_key
  }
}

# ──────────────────────────────────────────────
# 3. Application namespace + deployment + service
# ──────────────────────────────────────────────
resource "kubernetes_namespace" "learnfast" {
  metadata {
    name = "learnfast"
  }
  depends_on = [kind_cluster.learnfast]
}

resource "kubernetes_deployment" "learnfast_api" {
  metadata {
    name      = "learnfast-api"
    namespace = kubernetes_namespace.learnfast.metadata[0].name
    labels = {
      app = "learnfast-api"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "learnfast-api"
      }
    }

    template {
      metadata {
        labels = {
          app = "learnfast-api"
        }
      }

      spec {
        container {
          name  = "learnfast-api"
          image = var.app_image

          port {
            container_port = 8080
          }

          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = "prod"
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = 8080
            }
            initial_delay_seconds = 30
            period_seconds        = 10
          }

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = 8080
            }
            initial_delay_seconds = 60
            period_seconds        = 15
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "learnfast_api" {
  metadata {
    name      = "learnfast-api"
    namespace = kubernetes_namespace.learnfast.metadata[0].name
  }

  spec {
    type = "NodePort"

    selector = {
      app = "learnfast-api"
    }

    port {
      port        = 8080
      target_port = 8080
      node_port   = 30080
    }
  }
}

# ──────────────────────────────────────────────
# 4. ArgoCD via Helm
# ──────────────────────────────────────────────
resource "kubernetes_namespace" "argocd" {
  metadata {
    name = "argocd"
  }
  depends_on = [kind_cluster.learnfast]
}

resource "helm_release" "argocd" {
  name       = "argocd"
  repository = "https://argoproj.github.io/argo-helm"
  chart      = "argo-cd"
  version    = "7.3.4"
  namespace  = kubernetes_namespace.argocd.metadata[0].name

  set {
    name  = "server.service.type"
    value = "NodePort"
  }

  set {
    name  = "server.service.nodePortHttps"
    value = "30443"
  }

  set {
    name  = "configs.params.server\\.insecure"
    value = "true"
  }

  wait    = true
  timeout = 600
}

# ──────────────────────────────────────────────
# 5. ArgoCD Application (watches your Git repo)
# ──────────────────────────────────────────────
resource "kubectl_manifest" "argocd_app" {
  yaml_body = <<-YAML
    apiVersion: argoproj.io/v1alpha1
    kind: Application
    metadata:
      name: learnfast
      namespace: argocd
    spec:
      project: default
      source:
        repoURL: ${var.argocd_repo_url}
        targetRevision: main
        path: learnfast-infra/k8s/base
      destination:
        server: https://kubernetes.default.svc
        namespace: learnfast
      syncPolicy:
        automated:
          prune: true
          selfHeal: true
        syncOptions:
          - CreateNamespace=true
  YAML

  depends_on = [helm_release.argocd]
}
