# Public read policy for tenant logos

**Date**: 2026-05-03
**Triggered by**: email header tenant branding feature (see `docs/superpowers/specs/2026-05-03-email-header-tenant-branding-design.md`)

## Why

Tenant logos in transactional emails must use stable, long-lived URLs. Presigned URLs expire (max 7 days) and break for emails opened after expiry. The solution is a public-read bucket policy scoped to the `logos/*` prefix only.

Other S3 prefixes (payment proofs, etc.) remain private.

## Bucket policy to apply

Replace `<BUCKET_NAME>` with the actual bucket name (e.g. `klasio-assets-prod`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadTenantLogos",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::<BUCKET_NAME>/logos/*"
    }
  ]
}
```

## How to apply

```bash
aws s3api put-bucket-policy \
  --bucket <BUCKET_NAME> \
  --policy file://logos-public-read-policy.json
```

Verify:

```bash
aws s3api get-bucket-policy --bucket <BUCKET_NAME> | jq .
```

Confirm a logo is publicly reachable (replace with a real key):

```bash
curl -I "https://<BUCKET_NAME>.s3.<REGION>.amazonaws.com/logos/<TENANT_ID>/<FILE>.png"
# Expect: HTTP/1.1 200 OK
```

## Timing

Merge the email header PR **after** applying this policy, or coordinate so the policy is live within minutes of the merge. Between merge and policy application, tenant logo images in emails render as broken images (the `<img>` tag exists but returns 403). Klasio badge fallback is shown to recipients who open during that window.

## Scope verification

Before applying, confirm the policy Resource does **not** accidentally include other prefixes:

- `logos/*` — tenant logos — PUBLIC (intended)
- `payment-proofs/*` — student payment uploads — PRIVATE (must not be exposed)
- Any other prefix — PRIVATE

The `logos/*` ARN suffix is explicit. Do not use `*` alone as the Resource suffix.
