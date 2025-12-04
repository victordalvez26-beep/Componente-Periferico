import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

function LoginPage() {
  const { tenantId } = useParams();
  const navigate = useNavigate();
  
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [clinicInfo, setClinicInfo] = useState(null);
  const [textColor, setTextColor] = useState('#ffffff');
  const [backgroundColor, setBackgroundColor] = useState('#f4f6fb');
  const [headerColor, setHeaderColor] = useState('#000000');
  const [buttonColor, setButtonColor] = useState('#3b82f6');
  const [buttonTextColor, setButtonTextColor] = useState('#ffffff');
  const [buttonHoverColor, setButtonHoverColor] = useState('#315fde');
  const [cardBorderColor, setCardBorderColor] = useState('#e5e7eb');

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
      const backendBase = process.env.REACT_APP_BACKEND_URL || '';
      const res = await fetch(`${backendBase}/hcen-web/api/config/clinic/${tenantId}`);
      if (res.ok) {
        const data = await res.json();
        setClinicInfo(data);

        const primaryColor = data.colorPrimario && data.colorPrimario.trim() !== ''
          ? ensureReadableColor(data.colorPrimario)
          : '#667eea';

        const secondaryColor = data.colorSecundario && data.colorSecundario.trim() !== ''
          ? ensureReadableColor(data.colorSecundario)
          : '#6b7280';

        setBackgroundColor(lightenColor(secondaryColor, 0.65));
        setHeaderColor(primaryColor);
        setButtonColor(primaryColor);
        setButtonTextColor(getReadableTextColor(primaryColor));
        setButtonHoverColor(darkenColor(primaryColor, 0.1));
        setCardBorderColor(lightenColor(primaryColor, 0.4));

        setTextColor(getReadableTextColor(primaryColor));

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
      const response = await fetch('/hcen-web/api/auth/login', {
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

  const ensureReadableColor = (hexColor) => {
    if (!hexColor) return '#667eea';
    const luminance = getLuminance(hexColor);
    if (luminance < 0.15) {
      return lightenColor(hexColor, 0.35);
    }
    return hexColor;
  };

  const getReadableTextColor = (hexColor) => {
    const luminance = getLuminance(hexColor);
    return luminance > 0.55 ? '#111827' : '#ffffff';
  };

  const getLuminance = (hexColor) => {
    if (!hexColor) {
      return 1;
    }
    let hex = hexColor.replace('#', '');
    if (hex.length === 3) {
      hex = hex.split('').map((c) => c + c).join('');
    }
    const r = parseInt(hex.substring(0, 2), 16) / 255;
    const g = parseInt(hex.substring(2, 4), 16) / 255;
    const b = parseInt(hex.substring(4, 6), 16) / 255;
    return (0.299 * r + 0.587 * g + 0.114 * b);
  };

  const lightenColor = (hexColor, amount = 0.2) => {
    if (!hexColor) return '#667eea';
    let hex = hexColor.replace('#', '');
    if (hex.length === 3) {
      hex = hex.split('').map((c) => c + c).join('');
    }
    const num = parseInt(hex, 16);
    let r = (num >> 16) + Math.round(255 * amount);
    let g = ((num >> 8) & 0x00ff) + Math.round(255 * amount);
    let b = (num & 0x0000ff) + Math.round(255 * amount);
    r = Math.min(255, r);
    g = Math.min(255, g);
    b = Math.min(255, b);
    const newHex = (r << 16) | (g << 8) | b;
    return `#${newHex.toString(16).padStart(6, '0')}`;
  };

  const darkenColor = (hexColor, amount = 0.15) => {
    if (!hexColor) return '#315fde';
    let hex = hexColor.replace('#', '');
    if (hex.length === 3) {
      hex = hex.split('').map((c) => c + c).join('');
    }
    const num = parseInt(hex, 16);
    let r = (num >> 16) - Math.round(255 * amount);
    let g = ((num >> 8) & 0x00ff) - Math.round(255 * amount);
    let b = (num & 0x0000ff) - Math.round(255 * amount);
    r = Math.max(0, r);
    g = Math.max(0, g);
    b = Math.max(0, b);
    const newHex = (r << 16) | (g << 8) | b;
    return `#${newHex.toString(16).padStart(6, '0')}`;
  };

  const handleButtonMouseEnter = (e) => {
    e.currentTarget.style.backgroundColor = buttonHoverColor;
  };

  const handleButtonMouseLeave = (e) => {
    e.currentTarget.style.backgroundColor = buttonColor;
  };

  const cardStyle = {
    ...styles.card,
    borderTop: `4px solid ${headerColor}`,
    border: `1px solid ${cardBorderColor}`,
    boxShadow: '0 18px 45px rgba(15, 23, 42, 0.25)'
  };

  return (
    <div
      style={{
        ...styles.container,
        background: backgroundColor
      }}
    >
      <div style={cardStyle}>
        {/* Header */}
        <div
          style={{
            ...styles.header,
            backgroundColor: headerColor,
            color: textColor
          }}
        >
          {clinicInfo?.logoUrl ? (
            <div style={styles.logoWrapper}>
              <img
                src={clinicInfo.logoUrl}
                alt="Logo de la clínica"
                style={styles.logoImage}
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.style.display = 'none';
                }}
              />
            </div>
          ) : (
            <div style={{ ...styles.icon, color: textColor }}>🏥</div>
          )}
          <h1 style={{ ...styles.title, color: textColor }}>
            {clinicInfo?.nombrePortal || clinicInfo?.nombre || `Clínica ${tenantId}`}
          </h1>
          <p style={{ ...styles.subtitle, color: textColor === '#ffffff' ? 'rgba(255,255,255,0.85)' : '#4b5563' }}>
            Portal de Administración
          </p>
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
              backgroundColor: "#000000",
              color: buttonTextColor,
              opacity: loading ? 0.6 : 1,
              cursor: loading ? 'not-allowed' : 'pointer'
            }}
            onMouseEnter={handleButtonMouseEnter}
            onMouseLeave={handleButtonMouseLeave}
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
  logoWrapper: {
    display: 'flex',
    justifyContent: 'center',
    marginBottom: '15px'
  },
  logoImage: {
    maxHeight: '64px',
    maxWidth: '200px',
    objectFit: 'contain'
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

