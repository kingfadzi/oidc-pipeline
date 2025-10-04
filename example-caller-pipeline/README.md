# Example Caller Pipeline

This is an example GitLab CI pipeline that calls the OIDC validator API.

## Usage

Copy this `.gitlab-ci.yml` to your project that needs to call the OIDC validator API.

## Requirements

1. Your pipeline must run from the `main` branch
2. Your GitLab namespace must be in the allowed workspaces list
3. The `aud` (audience) must match the API configuration: `https://api.butterflycluster.com`

## How It Works

The pipeline uses GitLab's OIDC `id_tokens` feature to automatically generate a JWT token that includes:
- `namespace_path` - Your GitLab group/project namespace
- `project_path` - Full project path
- `ref` - Branch reference (refs/heads/main)
- `aud` - Audience (API URL)

The API validates all these claims before allowing the request.

## Customization

Update the API endpoint and payload in the `script` section to match your needs:

```yaml
script:
  - |
    curl -X POST https://api.butterflycluster.com/api/v1/deploy \
      -H "Authorization: Bearer ${GITLAB_OIDC_TOKEN}" \
      -H "Content-Type: application/json" \
      -d '{"action": "deploy", "version": "'"${CI_COMMIT_SHA}"'"}'
```
