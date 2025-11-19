import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';

function ConfiguracionPage() {
  const { tenantId } = useParams();
  // Usar variable de entorno o ruta relativa por defecto
  const backendBase = process.env.REACT_APP_BACKEND_URL || '';
  
  const [config, setConfig] = useState({
    nombrePortal: '',
    colorPrimario: '#3b82f6',
    colorSecundario: '#6b7280',
    logoUrl: ''
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadConfig();
  }, [tenantId]);

  const loadConfig = async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`${backendBase}/hcen-web/api/config/${tenantId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (res.ok) {
        const data = await res.json();
        setConfig(data);
      }
    } catch (err) {
      console.error('Error al cargar configuración:', err);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`${backendBase}/hcen-web/api/config/${tenantId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(config)
      });

      if (res.ok) {
        setMessage('✅ Configuración guardada exitosamente');
        setTimeout(() => setMessage(''), 3000);
        // Recargar la configuración actualizada después de guardar
        loadConfig();
      } else {
        setMessage('❌ Error al guardar configuración');
      }
    } catch (err) {
      setMessage('❌ Error de conexión');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {message && (
        <div style={{
          padding: '16px',
          backgroundColor: message.includes('✅') ? '#d1fae5' : '#fee2e2',
          color: message.includes('✅') ? '#065f46' : '#991b1b',
          borderRadius: '8px',
          marginBottom: '24px',
          fontSize: '15px',
          fontWeight: '500'
        }}>
          {message}
        </div>
      )}

      <div style={{
        backgroundColor: 'white',
        borderRadius: '12px',
        padding: '32px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <h3 style={{ margin: '0 0 8px 0', fontSize: '20px', fontWeight: '700' }}>
          ⚙️ Configuración del Portal
        </h3>
        <p style={{ margin: '0 0 32px 0', color: '#6b7280', fontSize: '15px' }}>
          Personalice la apariencia y configuración de su clínica
        </p>

        <form onSubmit={handleSubmit}>
          <div style={{ display: 'grid', gap: '24px', maxWidth: '600px' }}>
            <div>
              <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '600', color: '#374151' }}>
                Nombre del Portal
              </label>
              <input
                type="text"
                value={config.nombrePortal}
                onChange={(e) => setConfig({...config, nombrePortal: e.target.value})}
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  fontSize: '15px',
                  border: '2px solid #e5e7eb',
                  borderRadius: '8px',
                  outline: 'none'
                }}
                placeholder="Ej: Clínica San José"
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '600', color: '#374151' }}>
                Color Primario
              </label>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <input
                  type="color"
                  value={config.colorPrimario}
                  onChange={(e) => setConfig({...config, colorPrimario: e.target.value})}
                  style={{ width: '60px', height: '50px', border: '2px solid #e5e7eb', borderRadius: '8px', cursor: 'pointer' }}
                />
                <input
                  type="text"
                  value={config.colorPrimario}
                  onChange={(e) => setConfig({...config, colorPrimario: e.target.value})}
                  style={{
                    flex: 1,
                    padding: '12px 16px',
                    fontSize: '15px',
                    border: '2px solid #e5e7eb',
                    borderRadius: '8px'
                  }}
                  placeholder="#3b82f6"
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '600', color: '#374151' }}>
                Color Secundario
              </label>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <input
                  type="color"
                  value={config.colorSecundario}
                  onChange={(e) => setConfig({...config, colorSecundario: e.target.value})}
                  style={{ width: '60px', height: '50px', border: '2px solid #e5e7eb', borderRadius: '8px', cursor: 'pointer' }}
                />
                <input
                  type="text"
                  value={config.colorSecundario}
                  onChange={(e) => setConfig({...config, colorSecundario: e.target.value})}
                  style={{
                    flex: 1,
                    padding: '12px 16px',
                    fontSize: '15px',
                    border: '2px solid #e5e7eb',
                    borderRadius: '8px'
                  }}
                  placeholder="#6b7280"
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '600', color: '#374151' }}>
                URL del Logo
              </label>
              <input
                type="url"
                value={config.logoUrl}
                onChange={(e) => setConfig({...config, logoUrl: e.target.value})}
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  fontSize: '15px',
                  border: '2px solid #e5e7eb',
                  borderRadius: '8px'
                }}
                placeholder="https://ejemplo.com/logo.png"
              />
            </div>

            {/* Preview */}
            <div style={{
              padding: '24px',
              backgroundColor: '#f9fafb',
              borderRadius: '8px',
              border: '2px dashed #e5e7eb'
            }}>
              <h4 style={{ margin: '0 0 16px 0', fontSize: '14px', fontWeight: '600', color: '#6b7280' }}>
                👁️ Vista Previa
              </h4>
              <div style={{
                backgroundColor: config.colorPrimario,
                color: 'white',
                padding: '20px',
                borderRadius: '8px',
                textAlign: 'center'
              }}>
                <div style={{ fontSize: '24px', fontWeight: '700', marginBottom: '8px' }}>
                  {config.nombrePortal || 'Nombre del Portal'}
                </div>
                <div style={{ fontSize: '14px', opacity: 0.9 }}>
                  Clínica {tenantId} - Portal Personalizado
                </div>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              style={{
                backgroundColor: '#10b981',
                color: 'white',
                border: 'none',
                padding: '14px 32px',
                borderRadius: '8px',
                fontSize: '16px',
                fontWeight: '600',
                cursor: 'pointer',
                opacity: loading ? 0.6 : 1
              }}
            >
              {loading ? 'Guardando...' : '💾 Guardar Configuración'}
            </button>
          </div>
        </form>
      </div>

      {/* Integration Section */}
      <div style={{
        backgroundColor: 'white',
        borderRadius: '12px',
        padding: '32px',
        marginTop: '24px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <h3 style={{ margin: '0 0 16px 0', fontSize: '20px', fontWeight: '700' }}>
          🔗 Conexión como Nodo Periférico
        </h3>
        <p style={{ margin: '0 0 20px 0', color: '#6b7280', fontSize: '15px' }}>
          Estado de la integración con HCEN Central
        </p>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          padding: '16px',
          backgroundColor: '#d1fae5',
          borderRadius: '8px'
        }}>
          <span style={{ fontSize: '24px' }}>✅</span>
          <div>
            <div style={{ fontWeight: '600', color: '#065f46' }}>Conectado a HCEN</div>
            <div style={{ fontSize: '14px', color: '#047857' }}>Tenant ID: {tenantId}</div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ConfiguracionPage;

