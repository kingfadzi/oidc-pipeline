# GitLab OIDC Pipeline Validator

Production-ready Spring Boot API that validates GitLab OIDC pipeline identity tokens.

## Overview

This project provides:
1. **OIDC Validator API** - Spring Boot app that validates GitLab OIDC tokens
2. **Deployment Pipeline** - GitLab CI that builds and deploys the API via Docker
3. **Example Caller Pipeline** - Sample pipeline for projects calling the API (see `example-caller-pipeline/`)

## Validation

The API validates:
1. **JWT signature** - Via GitLab's OIDC issuer (https://eros.butterflycluster.com) and JWKS
2. **Audience** - Must match `https://api.butterflycluster.com`
3. **Branch** - Must be `main` (from `ref` claim)
4. **Workspace** - `namespace_path` must be in `allowed-workspaces.yml`

## Configuration

Edit `src/main/resources/allowed-workspaces.yml`:

```yaml
allowed:
  workspaces:
    - namespace: myorg/platform
      product: core-api
```

Update `src/main/resources/application.yml`:

```yaml
gitlab:
  oidc:
    issuer: https://eros.butterflycluster.com
    audience: https://api.butterflycluster.com
```

## Deployment

### Using Docker Compose

```bash
docker-compose up -d
```

### Using GitLab CI

The `.gitlab-ci.yml` automatically:
1. Builds Docker image
2. Pushes to GitLab Container Registry
3. Deploys to server via SSH

Required CI/CD variables:
- `SSH_PRIVATE_KEY` - SSH key for deployment server
- `DEPLOY_HOST` - Deployment server hostname
- `DEPLOY_USER` - SSH user for deployment

## For Calling Projects

See `example-caller-pipeline/` for a complete example of how to call this API from your GitLab pipeline.

Copy `example-caller-pipeline/.gitlab-ci.yml` to your project and ensure:
1. Your namespace is in `allowed-workspaces.yml`
2. Pipeline runs from `main` branch
3. `aud` matches: `https://api.butterflycluster.com`

## Local Development

```bash
mvn spring-boot:run
```

## GitLab OIDC Token Claims

The token includes:
- `namespace_path` - GitLab group/namespace
- `project_path` - Full project path
- `ref` - Branch reference (refs/heads/main)
- `aud` - Audience (your API URL)
- Validated via JWKS from https://eros.butterflycluster.com/.well-known/openid-configuration
