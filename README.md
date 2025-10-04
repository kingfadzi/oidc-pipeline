# GitLab OIDC Pipeline Validator

Production-ready Spring Boot API that validates GitLab OIDC pipeline identity tokens.

## GitLab Pipeline Setup

Add to your `.gitlab-ci.yml`:

```yaml
id_tokens:
  GITLAB_OIDC_TOKEN:
    aud: https://api.butterflycluster.com
```

The pipeline automatically sends this token when calling the API.

## Spring Boot Validation

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

Update `application.yml` with your audience:

```yaml
gitlab:
  oidc:
    issuer: https://eros.butterflycluster.com
    audience: https://api.butterflycluster.com  # Must match pipeline aud
```

## Running

```bash
mvn spring-boot:run
```

## Testing

```bash
curl -X POST http://localhost:8080/api/v1/deploy \
  -H "Authorization: Bearer <GITLAB_OIDC_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"action": "deploy", "version": "abc123"}'
```

## GitLab OIDC Token Claims

The token includes:
- `namespace_path` - GitLab group/namespace
- `project_path` - Full project path
- `ref` - Branch reference (refs/heads/main)
- `aud` - Audience (your API URL)
- Validated via JWKS from https://eros.butterflycluster.com/.well-known/openid-configuration
