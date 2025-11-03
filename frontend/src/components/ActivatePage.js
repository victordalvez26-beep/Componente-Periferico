import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';

function ActivatePage() {
  const { tenantId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [username, setUsername] = useState('');

  const token = searchParams.get('token');

  useEffect(() => {
    if (!token) {
      setError('Token de activación no válido. Verifique el enlace recibido por email.');
    }
  }, [token]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validaciones
    if (password.length < 8) {
      setError('La contraseña debe tener al menos 8 caracteres');
      return;
    }

    if (password !== confirmPassword) {
      setError('Las contraseñas no coinciden');
      return;
    }

    setLoading(true);

    try {
      const response = await fetch('/config/activate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          tenantId: tenantId,
          token: token,
          password: password
        })
      });

      if (response.ok) {
        const data = await response.json();
        setUsername(data.username);
        setSuccess(true);
        
        // Redirigir al login después de 3 segundos
        setTimeout(() => {
          navigate(`/portal/clinica-${tenantId}/login`);
        }, 3000);
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Error al activar la cuenta');
      }
    } catch (err) {
      console.error('Error activating account:', err);
      setError('Error de conexión. Intente nuevamente.');
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#f3f4f6'
      }}>
        <div style={{
          backgroundColor: '#ffffff',
          padding: '40px',
          borderRadius: '15px',
          boxShadow: '0 10px 30px rgba(0,0,0,0.1)',
          maxWidth: '500px',
          textAlign: 'center'
        }}>
          <i className="fa fa-exclamation-triangle" style={{fontSize: '48px', color: '#ef4444', marginBottom: '20px'}}></i>
          <h3 style={{color: '#1f2937', marginBottom: '15px'}}>Enlace Inválido</h3>
          <p style={{color: '#6b7280', marginBottom: '0'}}>
            El enlace de activación no es válido. Por favor, verifique el enlace recibido por email.
          </p>
        </div>
      </div>
    );
  }

  if (success) {
    return (
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#f0fdf4'
      }}>
        <div style={{
          backgroundColor: '#ffffff',
          padding: '40px',
          borderRadius: '15px',
          boxShadow: '0 10px 30px rgba(0,0,0,0.1)',
          maxWidth: '500px',
          textAlign: 'center'
        }}>
          <i className="fa fa-check-circle" style={{fontSize: '64px', color: '#10b981', marginBottom: '20px'}}></i>
          <h3 style={{color: '#1f2937', marginBottom: '15px'}}>¡Cuenta Activada!</h3>
          <p style={{color: '#6b7280', marginBottom: '20px'}}>
            Su cuenta ha sido activada exitosamente.
          </p>
          <div style={{
            backgroundColor: '#f8fafc',
            padding: '15px',
            borderRadius: '8px',
            marginBottom: '20px',
            border: '1px solid #e5e7eb'
          }}>
            <p style={{color: '#374151', marginBottom: '5px', fontSize: '14px'}}>
              <strong>Usuario:</strong> <code>{username}</code>
            </p>
            <p style={{color: '#6b7280', fontSize: '13px', marginBottom: '0'}}>
              Redirigiendo al login en 3 segundos...
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: '#f3f4f6'
    }}>
      <div style={{
        backgroundColor: '#ffffff',
        padding: '40px',
        borderRadius: '15px',
        boxShadow: '0 10px 30px rgba(0,0,0,0.1)',
        maxWidth: '500px',
        width: '100%'
      }}>
        <div style={{textAlign: 'center', marginBottom: '30px'}}>
          <i className="fa fa-user-check" style={{fontSize: '48px', color: '#3b82f6', marginBottom: '15px'}}></i>
          <h3 style={{color: '#1f2937', marginBottom: '10px'}}>Activar Cuenta</h3>
          <p style={{color: '#6b7280', fontSize: '14px', marginBottom: '0'}}>
            Clínica {tenantId} - Cree su contraseña de administrador
          </p>
        </div>

        {error && (
          <div className="alert alert-danger" style={{
            backgroundColor: '#fef2f2',
            color: '#dc2626',
            padding: '12px 15px',
            borderRadius: '8px',
            marginBottom: '20px',
            border: '1px solid #fecaca',
            fontSize: '14px'
          }}>
            <i className="fa fa-exclamation-circle" style={{marginRight: '5px'}}></i>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{marginBottom: '20px'}}>
            <label style={{
              display: 'block',
              color: '#374151',
              fontWeight: '600',
              marginBottom: '8px',
              fontSize: '14px'
            }}>
              Contraseña <span style={{color: '#ef4444'}}>*</span>
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
              placeholder="Mínimo 8 caracteres"
              style={{
                width: '100%',
                padding: '12px 15px',
                border: '2px solid #e5e7eb',
                borderRadius: '8px',
                fontSize: '15px',
                outline: 'none',
                transition: 'border-color 0.3s'
              }}
              onFocus={(e) => e.target.style.borderColor = '#3b82f6'}
              onBlur={(e) => e.target.style.borderColor = '#e5e7eb'}
            />
          </div>

          <div style={{marginBottom: '25px'}}>
            <label style={{
              display: 'block',
              color: '#374151',
              fontWeight: '600',
              marginBottom: '8px',
              fontSize: '14px'
            }}>
              Confirmar Contraseña <span style={{color: '#ef4444'}}>*</span>
            </label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              minLength={8}
              placeholder="Repita la contraseña"
              style={{
                width: '100%',
                padding: '12px 15px',
                border: '2px solid #e5e7eb',
                borderRadius: '8px',
                fontSize: '15px',
                outline: 'none',
                transition: 'border-color 0.3s'
              }}
              onFocus={(e) => e.target.style.borderColor = '#3b82f6'}
              onBlur={(e) => e.target.style.borderColor = '#e5e7eb'}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              padding: '14px',
              backgroundColor: loading ? '#9ca3af' : '#3b82f6',
              color: '#ffffff',
              border: 'none',
              borderRadius: '8px',
              fontSize: '16px',
              fontWeight: '600',
              cursor: loading ? 'not-allowed' : 'pointer',
              transition: 'background-color 0.3s'
            }}
            onMouseEnter={(e) => !loading && (e.target.style.backgroundColor = '#2563eb')}
            onMouseLeave={(e) => !loading && (e.target.style.backgroundColor = '#3b82f6')}
          >
            {loading ? (
              <>
                <i className="fa fa-spinner fa-spin" style={{marginRight: '8px'}}></i>
                Activando...
              </>
            ) : (
              <>
                <i className="fa fa-check" style={{marginRight: '8px'}}></i>
                Activar Cuenta
              </>
            )}
          </button>
        </form>

        <div style={{
          marginTop: '25px',
          padding: '15px',
          backgroundColor: '#f8fafc',
          borderRadius: '8px',
          border: '1px solid #e5e7eb'
        }}>
          <h6 style={{color: '#374151', fontSize: '13px', fontWeight: '600', marginBottom: '8px'}}>
            <i className="fa fa-info-circle" style={{marginRight: '5px', color: '#3b82f6'}}></i>
            Requisitos de Contraseña
          </h6>
          <ul style={{marginBottom: '0', paddingLeft: '20px', color: '#6b7280', fontSize: '12px'}}>
            <li>Mínimo 8 caracteres</li>
            <li>Se recomienda usar letras, números y símbolos</li>
            <li>Evite contraseñas obvias o información personal</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

export default ActivatePage;

