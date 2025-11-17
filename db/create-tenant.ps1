param(
    [Parameter(Mandatory=$true)] [string] $tenantId,
    [Parameter(Mandatory=$true)] [string] $tenantName,
    [string] $primaryColor = '#007bff',
    [string] $containerName = 'hcen-postgres-db',
    [switch] $createGlobalAdmin,
    [string] $adminNickname = 'admin_auto',
    [string] $adminPassword = 'password123'
)

# Paths
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$tpl = Join-Path $root 'create-tenant.sql.tpl'
$tmpSql = Join-Path $root 'tmp-create-tenant.sql'

if (-not (Test-Path $tpl)) {
    Write-Error "Template $tpl not found"
    exit 1
}

$schema = "schema_clinica_$tenantId"

# read template and replace placeholders
$sql = Get-Content $tpl -Raw
$sql = $sql -replace '\{\{TENANT_SCHEMA\}\}', $schema
$sql = $sql -replace '\{\{TENANT_ID\}\}', $tenantId
$sql = $sql -replace '\{\{COLOR_PRIMARIO\}\}', $primaryColor
$sql = $sql -replace '\{\{NOMBRE_PORTAL\}\}', ($tenantName -replace "'","''")

Set-Content -Path $tmpSql -Value $sql -Encoding UTF8

Write-Host "Copying SQL to container $containerName:/tmp/create-tenant.sql"
docker cp $tmpSql "$containerName:/tmp/create-tenant.sql"

Write-Host "Executing SQL inside Postgres container"
docker exec -i $containerName psql -U postgres -d hcen_db -f /tmp/create-tenant.sql

if ($createGlobalAdmin) {
    # create a global admin user row in public. Use a deterministic bcrypt hash used by tests.
    $bcrypt = '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO'
    $adminSql = @"
BEGIN;
INSERT INTO public.nodoperiferico (id, nombre, rut) VALUES ($tenantId, 'Tenant $tenantId - $tenantName', '210000${tenantId}000') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.usuario (id, nombre, email) VALUES (5000 + $tenantId::int, 'Admin Global $tenantId', 'admin.$tenantId@global') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.usuarioperiferico (id, nickname, password_hash, dtype, tenant_id, role)
  VALUES (5000 + $tenantId::int, '$adminNickname', '$bcrypt', 'AdministradorClinica', '$tenantId', 'ADMINISTRADOR') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.administradorclinica (id, nodo_periferico_id) VALUES (5000 + $tenantId::int, $tenantId) ON CONFLICT (id) DO NOTHING;
COMMIT;
"@

    # write admin sql to temp file and execute
    $tmpAdmin = Join-Path $root 'tmp-create-tenant-admin.sql'
    Set-Content -Path $tmpAdmin -Value $adminSql -Encoding UTF8
    docker cp $tmpAdmin "$containerName:/tmp/create-tenant-admin.sql"
    docker exec -i $containerName psql -U postgres -d hcen_db -f /tmp/create-tenant-admin.sql
}

Remove-Item -Path $tmpSql -ErrorAction SilentlyContinue
if ($createGlobalAdmin) { Remove-Item -Path $tmpAdmin -ErrorAction SilentlyContinue }

Write-Host "Tenant $tenantId ($schema) created."
