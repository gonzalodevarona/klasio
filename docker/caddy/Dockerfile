# Caddy with Cloudflare DNS plugin baked in (required for wildcard certs via DNS-01).
# Builds a custom caddy binary, then ships it on the standard alpine runtime image.

FROM caddy:2-builder-alpine AS builder
RUN xcaddy build \
    --with github.com/caddy-dns/cloudflare

FROM caddy:2-alpine
COPY --from=builder /usr/bin/caddy /usr/bin/caddy
