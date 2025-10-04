# GitLab OIDC Pipeline Validator

Production-ready Spring Boot API that validates GitLab OIDC pipeline identity tokens.

## Validation (Blocking)

The API validates these JWT claims and rejects if any fail:

1. **JWT Signature** - Cryptographic verification via GitLab's JWKS
2. **Audience (`aud`)** - Must match `https://api.butterflycluster.com`
3. **Namespace (`namespace_path`)** - Must be in allowed workspaces
4. **Project (`project_path`)** - Must match allowed project for namespace
5. **Branch (`ref`)** - Must match allowed branches for workspace
6. **Pipeline Source (`pipeline_source`)** - Only allows `push`, `web` (blocks `api`, `trigger`, `schedule`)
7. **Environment (`environment`)** - Must match allowed environments for workspace
8. **Protected Branch (`ref_protected`)** - Must be `true`

## Audit Logging (Non-blocking)

These fields are logged for audit trail but don't block requests:

- User/Actor (`sub`, `user_login`)
- Pipeline ID (`pipeline_id`)
- Job ID (`job_id`)
- Commit SHA (`sha`)
- Token timestamps (`iat`, `exp`)
- IP address (from HTTP headers)

## Configuration

Edit `k8s/configmap.yaml` (for k8s) or `src/main/resources/allowed-workspaces.yml` (for local):

```yaml
allowed:
  workspaces:
    - namespace: myorg/platform
      project: myorg/platform/core-api
      product: core-api
      branches:
        - main
        - production
      environments:
        - production
        - staging
```

**Note:** If `branches` or `environments` are omitted, any value is allowed.

## Response Format

Success response includes validated fields and audit trail:

```json
{
  "status": "success",
  "message": "Deployment accepted",
  "validated": {
    "audience": "https://api.butterflycluster.com",
    "namespace_path": "myorg/platform",
    "project_path": "myorg/platform/core-api",
    "branch": "main",
    "pipeline_source": "push",
    "environment": "production",
    "ref_protected": true
  },
  "audit": {
    "user": "fadzi",
    "pipeline_id": "12345",
    "job_id": "67890",
    "commit_sha": "abc123def456",
    "issued_at": "2025-10-04T19:00:00Z",
    "expires_at": "2025-10-04T20:00:00Z",
    "ip_address": "10.0.0.1"
  },
  "request": {
    "action": "deploy",
    "version": "v1.2.3"
  }
}
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
  environment: production  # Will be validated
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

Validated via JWKS from https://eros.butterflycluster.com/.well-known/openid-configuration
