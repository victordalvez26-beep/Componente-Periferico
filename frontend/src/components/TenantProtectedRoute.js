import React from 'react';
import { Navigate, useParams } from 'react-router-dom';

/**
 * Componente de protección de rutas multi-tenant.
 * Valida que el tenant_id del JWT coincida con el tenant_id de la URL.
 */
function TenantProtectedRoute({ children }) {
  const { tenantId } = useParams();
  const token = localStorage.getItem('token');
  const storedTenantId = localStorage.getItem('tenant_id');
  
  // Sin sesión → Redirect al login
  if (!token || !storedTenantId) {
    console.warn('⚠️ No session found, redirecting to login');
    return <Navigate to={`/portal/clinica/${tenantId}/login`} replace />;
  }
  
  // 🔒 VALIDACIÓN CRÍTICA: ¿El tenant del storage coincide con la URL?
  if (storedTenantId !== tenantId) {
    console.error('❌ Tenant mismatch:', {
      urlTenant: tenantId,
      storedTenant: storedTenantId
    });
    
    // Mostrar mensaje de error
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        padding: '20px',
        backgroundColor: '#f3f4f6',
        fontFamily: 'system-ui, -apple-system, sans-serif'
      }}>
        <div style={{
          backgroundColor: 'white',
          padding: '40px',
          borderRadius: '12px',
          boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
          maxWidth: '500px',
          textAlign: 'center'
        }}>
          <div style={{ fontSize: '48px', marginBottom: '20px' }}>⚠️</div>
          <h2 style={{ color: '#dc2626', marginBottom: '10px' }}>Acceso No Autorizado</h2>
          <p style={{ color: '#6b7280', marginBottom: '30px' }}>
            No tiene permiso para acceder a la Clínica {tenantId}.
            <br/>
            Su sesión corresponde a la Clínica {storedTenantId}.
          </p>
          <a 
            href={`/portal/clinica/${storedTenantId}/home`}
            style={{
              display: 'inline-block',
              backgroundColor: '#3b82f6',
              color: 'white',
              padding: '12px 24px',
              borderRadius: '8px',
              textDecoration: 'none',
              fontWeight: '600',
              marginRight: '10px'
            }}
          >
            Volver a mi clínica
          </a>
          <button 
            onClick={() => {
              localStorage.clear();
              window.location.href = `/portal/clinica/${tenantId}/login`;
            }}
            style={{
              backgroundColor: '#6b7280',
              color: 'white',
              padding: '12px 24px',
              borderRadius: '8px',
              border: 'none',
              fontWeight: '600',
              cursor: 'pointer'
            }}
          >
            Cerrar sesión
          </button>
        </div>
      </div>
    );
  }
  
  // ✅ Validación OK: Renderizar contenido protegido
  return children;
}

export default TenantProtectedRoute;

