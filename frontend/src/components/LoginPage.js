import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getApiUrl } from '../utils/api';

function LoginPage() {
  const { tenantId } = useParams();
  const navigate = useNavigate();
  
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [clinicInfo, setClinicInfo] = useState(null);

  // Verificar si ya hay sesión activa
  useEffect(() => {
    const token = localStorage.getItem('token');
    const storedTenantId = localStorage.getItem('tenant_id');
    
    if (token && storedTenantId === tenantId) {
      navigate(`/portal/clinica/${tenantId}/home`, { replace: true });
    }
    
    // Cargar información de la clínica
    fetchClinicInfo();
  }, [tenantId, navigate]);

  const fetchClinicInfo = async () => {
    try {
      const url = getApiUrl(`/hcen-web/api/config/clinic/${tenantId}`);
      const res = await fetch(url);
      if (res.ok) {
        const data = await res.json();
        setClinicInfo(data);
      }
    } catch (err) {
      console.error('Error fetching clinic info:', err);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await fetch(getApiUrl('/hcen-web/api/auth/login'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          nickname: username,
          password,
          tenantId  // Enviar el tenantId de la URL para saber en qué schema buscar
        }),
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        
        // Validar que el tenant_id del backend coincida
        if (data.tenant_id && data.tenant_id !== tenantId) {
          setError('Error: El usuario no pertenece a esta clínica');
          return;
        }

        // Guardar token y tenant_id
        localStorage.setItem('token', data.token);
        localStorage.setItem('tenant_id', tenantId);
        localStorage.setItem('username', username);
        localStorage.setItem('role', data.role || 'PROFESIONAL');

        // Redirect al dashboard
        navigate(`/portal/clinica/${tenantId}/home`, { replace: true });
      } else {
        const errorData = await response.json().catch(() => ({}));
        setError(errorData.message || 'Usuario o contraseña incorrectos');
      }
    } catch (err) {
      console.error('Login error:', err);
      setError('Error de conexión. Intente nuevamente.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        {/* Header */}
        <div style={styles.header}>
          <div style={styles.icon}>🏥</div>
          <h1 style={styles.title}>
            {clinicInfo?.nombre || `Clínica ${tenantId}`}
          </h1>
          <p style={styles.subtitle}>Portal de Administración</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} style={styles.form}>
          <div style={styles.formGroup}>
            <label style={styles.label}>Usuario</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Ingrese su usuario"
              style={styles.input}
              required
              autoFocus
            />
          </div>

          <div style={styles.formGroup}>
            <label style={styles.label}>Contraseña</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Ingrese su contraseña"
              style={styles.input}
              required
            />
          </div>

          {error && (
            <div style={styles.error}>
              ⚠️ {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            style={{
              ...styles.button,
              opacity: loading ? 0.6 : 1,
              cursor: loading ? 'not-allowed' : 'pointer'
            }}
          >
            {loading ? 'Iniciando sesión...' : 'Iniciar Sesión'}
          </button>
        </form>

        {/* Footer */}
        <div style={styles.footer}>
          <p style={styles.footerText}>
            <a href="/" style={styles.link}>¿Olvidó su contraseña?</a>
          </p>
          <p style={styles.footerSmall}>
            © 2025 HCEN - Sistema Multi-Tenant
          </p>
        </div>
      </div>
    </div>
  );
}

const styles = {
  container: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    padding: '20px',
    fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'
  },
  card: {
    backgroundColor: 'white',
    borderRadius: '16px',
    boxShadow: '0 20px 60px rgba(0,0,0,0.3)',
    width: '100%',
    maxWidth: '450px',
    overflow: 'hidden'
  },
  header: {
    background: 'linear-gradient(135deg, #1f2b7b 0%, #3b82f6 100%)',
    padding: '40px 30px',
    textAlign: 'center',
    color: 'white'
  },
  icon: {
    fontSize: '48px',
    marginBottom: '15px'
  },
  title: {
    margin: '0 0 8px 0',
    fontSize: '28px',
    fontWeight: '700'
  },
  subtitle: {
    margin: 0,
    fontSize: '16px',
    opacity: 0.9
  },
  form: {
    padding: '40px 30px'
  },
  formGroup: {
    marginBottom: '24px'
  },
  label: {
    display: 'block',
    marginBottom: '8px',
    fontSize: '14px',
    fontWeight: '600',
    color: '#374151'
  },
  input: {
    width: '100%',
    padding: '12px 16px',
    fontSize: '15px',
    border: '2px solid #e5e7eb',
    borderRadius: '8px',
    outline: 'none',
    transition: 'all 0.2s',
    boxSizing: 'border-box'
  },
  button: {
    width: '100%',
    padding: '14px',
    fontSize: '16px',
    fontWeight: '600',
    color: 'white',
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
    transition: 'transform 0.2s',
    marginTop: '10px'
  },
  error: {
    padding: '12px 16px',
    backgroundColor: '#fee2e2',
    border: '1px solid #fecaca',
    borderRadius: '8px',
    color: '#991b1b',
    fontSize: '14px',
    marginBottom: '20px'
  },
  footer: {
    padding: '20px 30px',
    borderTop: '1px solid #e5e7eb',
    textAlign: 'center'
  },
  footerText: {
    margin: '0 0 10px 0',
    fontSize: '14px',
    color: '#6b7280'
  },
  link: {
    color: '#3b82f6',
    textDecoration: 'none',
    fontWeight: '500'
  },
  footerSmall: {
    margin: 0,
    fontSize: '12px',
    color: '#9ca3af'
  }
};

export default LoginPage;

