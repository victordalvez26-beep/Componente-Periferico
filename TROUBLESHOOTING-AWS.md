# 🔧 Troubleshooting - Timeout en AWS EC2

## ❌ Problema

Timeout al acceder a:
```
http://ec2-3-144-76-133.us-east-2.compute.amazonaws.com:3001/hcen-web/api/auth/login
```

## 🔍 Análisis

### Problema 1: Puerto Incorrecto

- **Puerto 3001** = Frontend React (no responde a `/hcen-web/api/...`)
- **Backend** debería estar en puerto **8081** o **8080**

### Problema 2: Posibles Causas del Timeout

1. **Security Group de AWS no permite el puerto**
   - El Security Group debe tener reglas para permitir tráfico entrante en el puerto del backend (8081 o 8080)

2. **Backend no está corriendo**
   - Verificar que WildFly esté corriendo en la instancia EC2

3. **Puerto no está abierto en el firewall**
   - Verificar reglas de firewall en la instancia EC2

4. **URL incorrecta**
   - El path `/hcen-web/api/auth/login` solo existe en el backend

---

## ✅ Soluciones

### 1. Verificar Puerto del Backend

El backend debería estar en puerto **8081** (según docker-compose.yml) o **8080**.

**URL correcta debería ser:**
```
http://ec2-3-144-76-133.us-east-2.compute.amazonaws.com:8081/hcen-web/api/auth/login
```

### 2. Verificar Security Group de AWS

1. Ve a **EC2 Console** → **Security Groups**
2. Selecciona el Security Group de tu instancia
3. **Inbound Rules** debe tener:
   - Puerto **8081** (o 8080) → TCP → **0.0.0.0/0** (o tu IP)
   - Puerto **3001** → TCP → **0.0.0.0/0** (para el frontend)

### 3. Verificar que el Backend está Corriendo

**SSH a tu instancia EC2:**
```bash
ssh -i tu-key.pem ec2-user@ec2-3-144-76-133.us-east-2.compute.amazonaws.com
```

**Verificar contenedores Docker:**
```bash
docker ps
# Deberías ver el contenedor hcen-wildfly-app corriendo
```

**Verificar logs del backend:**
```bash
docker logs hcen-wildfly-app
# O si usas docker-compose:
docker-compose logs wildfly
```

**Verificar que el puerto está escuchando:**
```bash
netstat -tlnp | grep 8081
# O
ss -tlnp | grep 8081
```

### 4. Probar el Endpoint Directamente

Desde dentro de la instancia EC2:
```bash
# Health check
curl http://localhost:8081/hcen-web/api/config/health

# Login
curl -X POST http://localhost:8081/hcen-web/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"nickname":"admin_c1","password":"password123","tenantId":"101"}'
```

Si funciona desde dentro de la instancia pero no desde fuera:
- **Problema de Security Group** → Abre el puerto en AWS

Si no funciona ni desde dentro:
- **Problema del backend** → Revisar logs y configuración

---

## 🔧 Comandos Útiles para Diagnóstico

### Verificar procesos corriendo:
```bash
docker ps
docker ps -a  # Ver todos los contenedores (incluidos detenidos)
```

### Ver logs en tiempo real:
```bash
docker logs -f hcen-wildfly-app
```

### Verificar puertos abiertos:
```bash
# Desde dentro de EC2
sudo netstat -tlnp
sudo ss -tlnp

# Verificar que WildFly está escuchando
curl http://localhost:8081/hcen-web/api/config/health
```

### Verificar Security Group:
```bash
# Desde AWS CLI (si lo tienes instalado)
aws ec2 describe-security-groups --group-ids sg-xxxxx
```

---

## 📋 Checklist de Verificación

- [ ] Backend está corriendo en EC2 (`docker ps`)
- [ ] Puerto 8081 está abierto en Security Group (Inbound Rules)
- [ ] Firewall de EC2 permite tráfico en puerto 8081
- [ ] Health check funciona desde dentro de EC2: `curl http://localhost:8081/hcen-web/api/config/health`
- [ ] URL correcta: `http://ec2-3-144-76-133.us-east-2.compute.amazonaws.com:8081/hcen-web/api/auth/login`

---

## 🌐 URLs Correctas

### Backend (WildFly):
```
http://ec2-3-144-76-133.us-east-2.compute.amazonaws.com:8081/hcen-web/api/auth/login
http://ec2-3-144-76-133.us-east-2.compute.amazonaws.com:8081/hcen-web/api/config/health
```

### Frontend (React):
```
http://ec2-3-144-76-133.us-east-2.compute.amazonaws.com:3001
```

**El frontend debe hacer requests al BACKEND, no a sí mismo.**

---

## 🔐 Configuración de Security Group (Ejemplo)

**Inbound Rules:**
- Type: **Custom TCP**
- Port: **8081**
- Source: **0.0.0.0/0** (o restringir a IPs específicas)
- Description: "Backend WildFly"

- Type: **Custom TCP**
- Port: **3001**
- Source: **0.0.0.0/0**
- Description: "Frontend React"

- Type: **SSH**
- Port: **22**
- Source: **Tu IP** (para seguridad)

---

## 🚨 Solución Rápida

1. **Verifica el puerto del backend:**
   ```bash
   # SSH a EC2
   docker ps
   # Busca el contenedor del backend y el puerto mapeado
   ```

2. **Abre el puerto en Security Group:**
   - EC2 Console → Security Groups → Edit Inbound Rules
   - Agrega: TCP, 8081, 0.0.0.0/0

3. **Prueba la URL correcta:**
   ```
   http://ec2-3-144-76-133.us-east-2.compute.amazonaws.com:8081/hcen-web/api/auth/login
   ```

