output "app_url" {
  description = "LearnFast API URL"
  value       = "http://localhost:8080"
}

output "argocd_url" {
  description = "ArgoCD UI URL"
  value       = "https://localhost:8443"
}

output "argocd_password_command" {
  description = "Command to retrieve the ArgoCD admin password"
  value       = "kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d"
}

output "kubeconfig" {
  description = "Path to kubeconfig"
  value       = kind_cluster.learnfast.kubeconfig_path
}
