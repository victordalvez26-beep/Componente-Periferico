import { useState, useEffect } from 'react';

/**
 * Hook personalizado para cargar y usar la configuración de la clínica
 * @param {string} tenantId - ID del tenant/clínica
 * @returns {object} - Configuración de la clínica y estado de carga
 */
export function useClinicConfig(tenantId) {
  const [config, setConfig] = useState({
    nombrePortal: '',
    colorPrimario: '#3b82f6',
    colorSecundario: '#6b7280',
    logoUrl: ''
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!tenantId) {
      setLoading(false);
      return;
    }

    const loadConfig = async () => {
      try {
        setLoading(true);
        setError(null);
        const token = localStorage.getItem('token');
        const backendBase = process.env.REACT_APP_BACKEND_URL || '';
        
        const res = await fetch(`${backendBase}/hcen-web/api/config/${tenantId}`, {
          headers: { 
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        
        if (res.ok) {
          const data = await res.json();
          setConfig({
            nombrePortal: data.nombrePortal || '',
            colorPrimario: data.colorPrimario || '#3b82f6',
            colorSecundario: data.colorSecundario || '#6b7280',
            logoUrl: data.logoUrl || ''
          });
        } else if (res.status === 404) {
          // Configuración no existe aún, usar valores por defecto
          setConfig({
            nombrePortal: '',
            colorPrimario: '#3b82f6',
            colorSecundario: '#6b7280',
            logoUrl: ''
          });
        } else {
          throw new Error(`Error ${res.status}`);
        }
      } catch (err) {
        console.error('Error al cargar configuración:', err);
        setError(err.message);
        // Usar valores por defecto en caso de error
        setConfig({
          nombrePortal: '',
          colorPrimario: '#3b82f6',
          colorSecundario: '#6b7280',
          logoUrl: ''
        });
      } finally {
        setLoading(false);
      }
    };

    loadConfig();
  }, [tenantId]);

  return { config, loading, error };
}

