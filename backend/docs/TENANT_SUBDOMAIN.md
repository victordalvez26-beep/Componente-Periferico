Tenant resolution via subdomain (tenant.example.com)
===============================================

Overview
--------
This project supports multi-tenancy. One recommended production mode is "subdomain-based tenancy": each tenant has its own subdomain, for example:

- alice.example.com -> tenant id: `alice`
- clinic-1.example.com -> tenant id: `clinic-1`

When the application receives a request, it extracts the tenant id from the host subdomain and uses it as the `currentTenantId` in the per-request `TenantContext`.

Why use subdomains?
- Clean separation per tenant in URLs and cookies.
- Easy routing at the DNS / reverse-proxy / API gateway level.
- Works well with provisioning pipelines and SSL certs (wildcard certs or ACME per subdomain).

How the code resolves tenantId (priority order)
---------------------------------------------
The `TenantInitializerFilter` resolves tenantId using the following priority:

1. Host subdomain (highest priority). Example: `clinic-1.example.com` -> `clinic-1`.
2. JWT claim `tenantId` (when requests are authenticated via MP-JWT).
3. HTTP header configured (default `X-Tenant-Id`, next fallback `X-Clinic-Id`). This is useful when an API gateway injects the tenant.
4. Query parameters `tenantId` or `clinicId` (useful for testing or simple integrations).
5. Configured fallback (via MicroProfile config or environment variable) — only used as a last resort.

Configuration
-------------
Settings are read via MicroProfile Config. Place these values in `microprofile-config.properties` or provide them as environment variables or JVM system properties.

microprofile-config.properties (example placed in the EJB resources):

```
clinica.tenancy.header=X-Tenant-Id
clinica.tenancy.require=false
clinica.tenancy.fallback=
```

- `clinica.tenancy.header` - name of header to use as tenant id (default `X-Tenant-Id`).
- `clinica.tenancy.require` - when `true`, the application will reject requests without a resolved tenant (HTTP 400). Useful in strictly multi-tenant deployments.
- `clinica.tenancy.fallback` - optional fallback tenant id. Prefer not to set this in production.

Environment variables / system properties
----------------------------------------
- `CLINICA_TENANT_FALLBACK` - alternative way to provide a fallback tenant id via environment variable.
- `-Dclinica.tenant.fallback=...` - system property alternative.

Reverse proxy / DNS setup
-------------------------
1. Point a wildcard DNS record to your load balancer or reverse proxy (e.g. `*.example.com` → proxy IP).
2. Configure your reverse proxy (Nginx / HAProxy / Traefik) to forward requests to the application while preserving the Host header (so the app can read the subdomain).
3. If you use SSL, either use a wildcard certificate (*.example.com) or configure automatic certificate issuance per subdomain via ACME.

Example Nginx snippet (preserve Host header):

```
server {
    listen 443 ssl;
    server_name  ~^(?<tenant>[^.]+)\.example\.com$;

    ssl_certificate /path/to/wildcard.crt;
    ssl_certificate_key /path/to/wildcard.key;

    location / {
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_pass http://backend:8080;
    }
}
```

Testing locally
---------------
- Use your OS hosts file to map a test subdomain to localhost:

  Windows (as Admin) edit `C:\Windows\System32\drivers\etc\hosts` and add:

```
127.0.0.1    clinic-1.localtest
127.0.0.1    alice.localtest
```

- Start WildFly and access `http://clinic-1.localtest:8080/nodo-periferico/` — the application will extract `clinic-1` as tenant.

- If you're running the frontend on a separate dev-server, configure its proxy (or use the same host header) so backend receives Host=clinic-1.localtest.

Security notes
--------------
- Prefer JWT/header/host resolution over fallback. Fallback is only for local dev or transition phases.
- When enabling `clinica.tenancy.require=true`, ensure your API gateway or auth layer always sets the tenant (via host or header) to avoid legitimate traffic being blocked.

Support and troubleshooting
-------------------------
- If tenant is not being detected, check:
  - Does the Host header preserve the original host? (Reverse proxies often replace it unless configured.)
  - Is the header `X-Tenant-Id` present? (Check using `curl -v`.)
  - Logs: `TenantInitializerFilter` logs resolved tenant id at INFO level.

Contact
-------
For deeper integration with your infra (DNS, proxy, certs), provide your reverse-proxy configuration and I can produce a tailored guide.
