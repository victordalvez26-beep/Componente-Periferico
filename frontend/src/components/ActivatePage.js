import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';

function ActivatePage() {
  const { tenantId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  
  // Estados para credenciales de usuario
  const [username, setUsername] = useState('admin_c' + tenantId); // Sugerencia editable
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  
  // Estados para datos de la clínica
  const [rut, setRut] = useState('');
  const [departamento, setDepartamento] = useState('');
  const [localidad, setLocalidad] = useState('');
  const [direccion, setDireccion] = useState('');
  const [telefono, setTelefono] = useState('');
  
  // Estados de UI
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const token = searchParams.get('token');

  useEffect(() => {
    if (!token) {
      setError('Token de activación no válido. Verifique el enlace recibido por email.');
    }
    // Health check opcional - no bloquea la activación
    checkBackendHealth();
  }, [token]);

  const checkBackendHealth = async () => {
    try {
      const response = await fetch('/hcen-web/api/config/health', {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' }
      });
      if (!response.ok) {
        console.warn('Backend health check failed:', response.status);
        // No bloquear la activación, solo mostrar un warning
        if (response.status === 503) {
          console.warn('⚠️ Backend no disponible. La activación puede fallar.');
        } else if (response.status === 404) {
          console.warn('⚠️ Endpoint no encontrado. Verifica que el backend esté desplegado correctamente.');
        }
      } else {
        console.log('✅ Backend está disponible');
      }
    } catch (err) {
      console.warn('⚠️ No se pudo verificar el backend:', err.message);
      // No bloquear, solo loguear el warning
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validaciones
    if (!username || username.length < 3) {
      setError('El nombre de usuario debe tener al menos 3 caracteres');
      return;
    }
    
    if (password.length < 8) {
      setError('La contraseña debe tener al menos 8 caracteres');
      return;
    }

    if (password !== confirmPassword) {
      setError('Las contraseñas no coinciden');
      return;
    }
    
    if (!rut || rut.length < 12) {
      setError('El RUT debe tener 12 dígitos');
      return;
    }
    
    if (!departamento) {
      setError('Debe seleccionar un departamento');
      return;
    }
    
    if (!direccion || !telefono) {
      setError('Debe completar dirección y teléfono');
      return;
    }

    setLoading(true);

    try {
      // Usar variable de entorno o URL por defecto
      const backendBase = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8081';
      // Llamada al backend periférico con TODOS los datos de la clínica
      const response = await fetch(`${backendBase}/hcen-web/api/config/activate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          tenantId: tenantId,
          token: token,
          username: username,
          password: password,
          // Datos de la clínica
          rut: rut,
          departamento: departamento,
          localidad: localidad,
          direccion: direccion,
          telefono: telefono
        })
      });

      // Leer el body solo una vez
      const contentType = response.headers.get('content-type');
      let data = null;
      let errorText = null;
      
      if (contentType && contentType.includes('application/json')) {
        try {
          data = await response.json();
        } catch (jsonErr) {
          console.error('Error parsing JSON response:', jsonErr);
          // Si falla el parseo JSON, intentar leer como texto
          const text = await response.text();
          errorText = text;
        }
      } else {
        // Si no es JSON, leer como texto
        errorText = await response.text();
      }

      if (response.ok) {
        if (data) {
          setUsername(data.username || data.nickname || username);
          setSuccess(true);
          
          // Redirigir al login después de 3 segundos (con formato correcto /clinica/ID)
          setTimeout(() => {
            navigate(`/portal/clinica/${tenantId}/login`);
          }, 3000);
        } else {
          setError('Error: Respuesta inválida del servidor');
        }
      } else {
        let errorMessage = 'Error al activar la cuenta';
        if (data) {
          errorMessage = data.error || data.message || errorMessage;
        } else if (errorText) {
          // Intentar parsear el texto como JSON si es posible
          try {
            const parsed = JSON.parse(errorText);
            errorMessage = parsed.error || parsed.message || errorMessage;
          } catch {
            errorMessage = errorText || `Error HTTP ${response.status}`;
          }
        } else {
          errorMessage = `Error HTTP ${response.status}`;
        }
        setError(errorMessage);
      }
    } catch (err) {
      console.error('Error activating account:', err);
      let errorMessage = 'Error al activar la cuenta';
      
      if (err.name === 'TypeError' && err.message.includes('fetch')) {
        errorMessage = 'Error de conexión. Verifique que el backend periférico esté corriendo en el puerto 8081 (o 8080 si está en WildFly local).';
      } else if (err.message && err.message.includes('JSON')) {
        errorMessage = 'Error: Respuesta inválida del servidor. Verifique los logs del backend.';
      } else if (err.message && err.message.includes('timeout')) {
        errorMessage = 'El servidor tardó demasiado en responder. La activación puede estar en proceso. Verifique los logs del backend.';
      } else {
        errorMessage = `Error de conexión: ${err.message || 'Intente nuevamente'}`;
      }
      
      setError(errorMessage);
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

          {/* NUEVOS CAMPOS: Datos de la Clínica */}
          <div style={{
            backgroundColor: '#f8fafc',
            padding: '20px',
            borderRadius: '10px',
            marginBottom: '20px',
            marginTop: '30px',
            border: '1px solid #e5e7eb'
          }}>
            <h5 style={{color: '#1f2937', marginTop: '0', marginBottom: '20px', fontSize: '16px', fontWeight: '600'}}>
              Datos de la Clínica
            </h5>

            <div style={{marginBottom: '15px'}}>
              <label style={{display: 'block', color: '#374151', fontWeight: '600', marginBottom: '8px', fontSize: '14px'}}>
                Nombre de Usuario <span style={{color: '#ef4444'}}>*</span>
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                placeholder="admin_c..."
                style={{width: '100%', padding: '12px 15px', border: '2px solid #e5e7eb', borderRadius: '8px', fontSize: '15px'}}
              />
              <small style={{color: '#6b7280', fontSize: '12px'}}>Usuario para iniciar sesión en el portal</small>
            </div>

            <div style={{marginBottom: '15px'}}>
              <label style={{display: 'block', color: '#374151', fontWeight: '600', marginBottom: '8px', fontSize: '14px'}}>
                RUT de la Clínica <span style={{color: '#ef4444'}}>*</span>
              </label>
              <input
                type="text"
                value={rut}
                onChange={(e) => setRut(e.target.value)}
                required
                maxLength={12}
                placeholder="123456789012"
                style={{width: '100%', padding: '12px 15px', border: '2px solid #e5e7eb', borderRadius: '8px', fontSize: '15px'}}
              />
              <small style={{color: '#6b7280', fontSize: '12px'}}>12 dígitos</small>
            </div>

            <div style={{marginBottom: '15px'}}>
              <label style={{display: 'block', color: '#374151', fontWeight: '600', marginBottom: '8px', fontSize: '14px'}}>
                Departamento <span style={{color: '#ef4444'}}>*</span>
              </label>
              <select
                value={departamento}
                onChange={(e) => setDepartamento(e.target.value)}
                required
                style={{width: '100%', padding: '12px 15px', border: '2px solid #e5e7eb', borderRadius: '8px', fontSize: '15px'}}
              >
                <option value="">Seleccione...</option>
                <option value="MONTEVIDEO">Montevideo</option>
                <option value="CANELONES">Canelones</option>
                <option value="MALDONADO">Maldonado</option>
                <option value="COLONIA">Colonia</option>
                <option value="SALTO">Salto</option>
              </select>
            </div>

            <div style={{marginBottom: '15px'}}>
              <label style={{display: 'block', color: '#374151', fontWeight: '600', marginBottom: '8px', fontSize: '14px'}}>
                Localidad
              </label>
              <input
                type="text"
                value={localidad}
                onChange={(e) => setLocalidad(e.target.value)}
                placeholder="Ej: Ciudad de la Costa"
                style={{width: '100%', padding: '12px 15px', border: '2px solid #e5e7eb', borderRadius: '8px', fontSize: '15px'}}
              />
            </div>

            <div style={{marginBottom: '15px'}}>
              <label style={{display: 'block', color: '#374151', fontWeight: '600', marginBottom: '8px', fontSize: '14px'}}>
                Dirección <span style={{color: '#ef4444'}}>*</span>
              </label>
              <input
                type="text"
                value={direccion}
                onChange={(e) => setDireccion(e.target.value)}
                required
                placeholder="Ej: Av. Italia 2020"
                style={{width: '100%', padding: '12px 15px', border: '2px solid #e5e7eb', borderRadius: '8px', fontSize: '15px'}}
              />
            </div>

            <div style={{marginBottom: '15px'}}>
              <label style={{display: 'block', color: '#374151', fontWeight: '600', marginBottom: '8px', fontSize: '14px'}}>
                Teléfono <span style={{color: '#ef4444'}}>*</span>
              </label>
              <input
                type="tel"
                value={telefono}
                onChange={(e) => setTelefono(e.target.value)}
                required
                placeholder="099123456"
                style={{width: '100%', padding: '12px 15px', border: '2px solid #e5e7eb', borderRadius: '8px', fontSize: '15px'}}
              />
            </div>
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

