# GitLab OIDC Pipeline Validator

Production-ready Spring Boot API that validates GitLab OIDC pipeline identity tokens.

## Validation

The API validates:
1. **JWT signature** - Via GitLab's OIDC issuer (https://eros.butterflycluster.com) and JWKS
2. **Audience** - Must match `https://api.butterflycluster.com`
3. **Branch** - Must be `main` (from `ref` claim)
4. **Workspace** - `namespace_path` must be in `allowed-workspaces.yml`

## Configuration

Edit allowed workspaces in `k8s/configmap.yaml` (for k8s) or `src/main/resources/allowed-workspaces.yml` (for local):

```yaml
allowed:
  workspaces:
    - namespace: myorg/platform
      product: core-api
```

## Deployment

### Kubernetes (Production)

1. Update image in `k8s/deployment.yaml`:
   ```yaml
   image: registry.gitlab.com/yourorg/oidc-pipeline:latest
   ```

2. Apply manifests:
   ```bash
   kubectl apply -f k8s/
   ```

3. GitLab CI automatically deploys on push to `main` branch

Required GitLab CI/CD variables:
- `KUBE_CONTEXT` - Kubernetes context for deployment

### Local (Docker Compose)

```bash
docker-compose up -d
```

Access at http://localhost:8080

## Example: Calling from GitLab Pipeline

```yaml
deploy:
  stage: deploy
  image: curlimages/curl:latest
  id_tokens:
    GITLAB_OIDC_TOKEN:
      aud: https://api.butterflycluster.com
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  script:
    - |
      curl -X POST https://api.butterflycluster.com/api/v1/deploy \
        -H "Authorization: Bearer ${GITLAB_OIDC_TOKEN}" \
        -H "Content-Type: application/json" \
        -d '{"action": "deploy", "version": "'"${CI_COMMIT_SHA}"'"}'
```

## Local Development

```bash
mvn spring-boot:run
```

## GitLab OIDC Token Claims

- `namespace_path` - GitLab group/namespace
- `project_path` - Full project path
- `ref` - Branch reference (refs/heads/main)
- `aud` - Audience (your API URL)
- Validated via JWKS from https://eros.butterflycluster.com/.well-known/openid-configuration
