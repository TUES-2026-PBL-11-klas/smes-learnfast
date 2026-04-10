variable "cluster_name" {
  description = "Name of the Kind cluster"
  type        = string
  default     = "learnfast"
}

variable "app_image" {
  description = "Container image for the LearnFast API"
  type        = string
  default     = "ghcr.io/TUES-2026-PBL-11-klas/smes-learnfast:latest"
}

variable "argocd_repo_url" {
  description = "Git repository URL for ArgoCD to watch"
  type        = string
  default     = "https://github.com/TUES-2026-PBL-11-klas/smes-learnfast.git"
}
