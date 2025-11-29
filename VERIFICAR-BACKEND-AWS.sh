#!/bin/bash
# Script para verificar el estado del backend en AWS EC2

echo "🔍 Verificando estado del backend en AWS EC2..."
echo ""

# 1. Verificar contenedores Docker
echo "📦 Contenedores Docker corriendo:"
docker ps | grep -E "wildfly|backend|hcen" || echo "⚠️  No se encontraron contenedores del backend"
echo ""

# 2. Verificar puerto 8081
echo "🔌 Verificando puerto 8081:"
if netstat -tlnp 2>/dev/null | grep 8081 > /dev/null; then
    echo "✅ Puerto 8081 está escuchando"
    netstat -tlnp | grep 8081
else
    echo "❌ Puerto 8081 NO está escuchando"
    # Intentar con ss si netstat no está disponible
    if ss -tlnp 2>/dev/null | grep 8081 > /dev/null; then
        echo "✅ Puerto 8081 está escuchando (verificado con ss)"
        ss -tlnp | grep 8081
    else
        echo "❌ Puerto 8081 NO está escuchando"
    fi
fi
echo ""

# 3. Probar health check local
echo "🏥 Health check local (localhost:8081):"
if curl -s -f -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:8081/hcen-web/api/config/health; then
    echo "✅ Backend responde correctamente en localhost:8081"
else
    echo "❌ Backend NO responde en localhost:8081"
    echo "   Verifica los logs: docker logs hcen-wildfly-app"
fi
echo ""

# 4. Verificar logs recientes
echo "📋 Últimas líneas de logs del backend:"
docker logs --tail 20 hcen-wildfly-app 2>/dev/null || echo "⚠️  No se pudieron obtener logs (¿está corriendo el contenedor?)"
echo ""

# 5. Verificar variables de entorno
echo "🔧 Variables de entorno del contenedor:"
docker exec hcen-wildfly-app env | grep -E "PORT|JWT|MONGO" || echo "⚠️  No se pudo acceder al contenedor"
echo ""

echo "✅ Verificación completada"
echo ""
echo "📝 Próximos pasos:"
echo "   1. Si el backend no responde, revisa los logs: docker logs -f hcen-wildfly-app"
echo "   2. Si responde localmente pero no desde fuera, verifica Security Group en AWS"
echo "   3. Verifica que el puerto esté abierto: netstat -tlnp | grep 8081"

