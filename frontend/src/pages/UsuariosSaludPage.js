import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useClinicConfig } from '../hooks/useClinicConfig';
import './UsuariosSaludPage.css';

/**
 * Página para gestionar Usuarios de Salud (pacientes) de una clínica.
 * Permite crear, listar y ver detalles de pacientes.
 */
function UsuariosSaludPage() {
  const { tenantId } = useParams();
  const { config, loading: configLoading } = useClinicConfig(tenantId);
  const [usuarios, setUsuarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [mensaje, setMensaje] = useState({ tipo: '', texto: '' });
  
  // Departamentos de Uruguay (deben coincidir con el enum de HCEN)
  const departamentos = [
    'ARTIGAS', 'CANELONES', 'CERRO_LARGO', 'COLONIA', 'DURAZNO',
    'FLORES', 'FLORIDA', 'LAVALLEJA', 'MALDONADO', 'MONTEVIDEO',
    'PAYSANDU', 'RIO_NEGRO', 'RIVERA', 'ROCHA', 'SALTO',
    'SAN_JOSE', 'SORIANO', 'TACUAREMBO', 'TREINTA_Y_TRES'
  ];
  
  // Función helper para capitalizar palabras
  const capitalizarPalabras = (texto) => {
    return texto.split(' ').map(palabra => 
      palabra.charAt(0).toUpperCase() + palabra.slice(1).toLowerCase()
    ).join(' ');
  };
  
  // Función helper para formatear departamentos
  const formatDepartamento = (dept) => {
    if (!dept) return '-';
    
    const departamentoMap = {
      'ARTIGAS': 'Artigas',
      'CANELONES': 'Canelones',
      'CERRO_LARGO': 'Cerro Largo',
      'COLONIA': 'Colonia',
      'DURAZNO': 'Durazno',
      'FLORES': 'Flores',
      'FLORIDA': 'Florida',
      'LAVALLEJA': 'Lavalleja',
      'MALDONADO': 'Maldonado',
      'MONTEVIDEO': 'Montevideo',
      'PAYSANDU': 'Paysandú',
      'RIO_NEGRO': 'Río Negro',
      'RIVERA': 'Rivera',
      'ROCHA': 'Rocha',
      'SALTO': 'Salto',
      'SAN_JOSE': 'San José',
      'SORIANO': 'Soriano',
      'TACUAREMBO': 'Tacuarembó',
      'TREINTA_Y_TRES': 'Treinta y Tres'
    };
    
    if (departamentoMap[dept]) {
      return departamentoMap[dept];
    }
    const sinGuiones = dept.replaceAll('_', ' ');
    return capitalizarPalabras(sinGuiones);
  };
  
  const [formData, setFormData] = useState({
    ci: '',
    nombre: '',
    apellido: '',
    fechaNacimiento: '',
    direccion: '',
    telefono: '',
    email: '',
    departamento: '',
    localidad: ''
  });
  
  useEffect(() => {
    cargarUsuarios();
  }, [tenantId]);
  
  const cargarUsuarios = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(
        `/hcen-web/api/clinica/${tenantId}/usuarios-salud`,
        {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        }
      );
      
      if (response.ok) {
        const data = await response.json();
        setUsuarios(data);
      } else {
        const errorData = await response.json().catch(() => ({}));
        const errorMsg = errorData.error || 'Error al cargar la lista de pacientes';
        const msg = errorMsg.toLowerCase();
        if (msg.includes('mongo') || msg.includes('database') || msg.includes('connection')) {
          mostrarMensaje('error', 'Error al conectarse con la base de datos. Contacte a su administrador.');
        } else {
          mostrarMensaje('error', errorMsg);
        }
      }
    } catch (error) {
      const errMsg = (error.message || String(error)).toLowerCase();
      if (errMsg.includes('mongo') || errMsg.includes('database') || errMsg.includes('connection')) {
        mostrarMensaje('error', 'Error al conectarse con la base de datos. Contacte a su administrador.');
      } else {
        mostrarMensaje('error', 'Error de conexión al cargar pacientes');
      }
    } finally {
      setLoading(false);
    }
  };
  
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };
  
  const validarFechaNacimiento = (fechaStr) => {
    if (!fechaStr) {
      return null;
    }
    const fecha = new Date(fechaStr);
    if (Number.isNaN(fecha.getTime())) {
      return 'La fecha de nacimiento no es válida';
    }
    if (fecha > new Date()) {
      return 'La fecha de nacimiento no puede ser una fecha futura';
    }
    return null;
  };
  
  const esErrorBaseDatos = (errorMsg) => {
    if (!errorMsg) {
      return false;
    }
    const msg = errorMsg.toLowerCase();
    return msg.includes('mongo') || msg.includes('database') || msg.includes('connection');
  };
  
  const manejarErrorRespuesta = (errorMsg) => {
    if (esErrorBaseDatos(errorMsg)) {
      mostrarMensaje('error', 'Error al conectarse con la base de datos. Contacte a su administrador.');
    } else {
      mostrarMensaje('error', errorMsg || 'Error al guardar el paciente');
    }
  };
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validar CI
    if (!formData.ci || formData.ci.trim() === '') {
      mostrarMensaje('error', 'El CI es obligatorio');
      return;
    }
    
    // Validar fecha de nacimiento si está presente
    const errorFecha = validarFechaNacimiento(formData.fechaNacimiento);
    if (errorFecha) {
      mostrarMensaje('error', errorFecha);
      return;
    }
    
    try {
      const token = localStorage.getItem('token');
      const url = editingId 
        ? `/hcen-web/api/clinica/${tenantId}/usuarios-salud/${editingId}`
        : `/hcen-web/api/clinica/${tenantId}/usuarios-salud`;
      
      const method = editingId ? 'PUT' : 'POST';
      
      const response = await fetch(url, {
        method: method,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(formData)
      });
      
      if (response.ok) {
        const mensaje = editingId 
          ? 'Paciente actualizado correctamente' 
          : 'Paciente registrado correctamente (sincronizado con HCEN)';
        mostrarMensaje('success', mensaje);
        setShowForm(false);
        resetForm();
        cargarUsuarios();
      } else {
        const errorData = await response.json().catch(() => ({}));
        const errorMsg = errorData.error || 'Error al guardar el paciente';
        manejarErrorRespuesta(errorMsg);
      }
    } catch (error) {
      const errMsg = error.message || String(error);
      manejarErrorRespuesta(errMsg);
    }
  };
  
  const handleEditar = (usuario) => {
    setEditingId(usuario.id);
    setFormData({
      ci: usuario.ci,
      nombre: usuario.nombre || '',
      apellido: usuario.apellido || '',
      fechaNacimiento: usuario.fechaNacimiento || '',
      direccion: usuario.direccion || '',
      telefono: usuario.telefono || '',
      email: usuario.email || '',
      departamento: usuario.departamento || '',
      localidad: usuario.localidad || ''
    });
    setShowForm(true);
  };
  
  const handleCancelar = () => {
    setShowForm(false);
    resetForm();
  };
  
  const resetForm = () => {
    setEditingId(null);
    setFormData({
      ci: '',
      nombre: '',
      apellido: '',
      fechaNacimiento: '',
      direccion: '',
      telefono: '',
      email: '',
      departamento: '',
      localidad: ''
    });
  };
  
  const mostrarMensaje = (tipo, texto) => {
    setMensaje({ tipo, texto });
    setTimeout(() => {
      setMensaje({ tipo: '', texto: '' });
    }, 5000);
  };
  
  // No renderizar hasta que la configuración esté cargada
  if (configLoading) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        fontSize: '16px',
        color: '#6b7280'
      }}>
        Cargando...
      </div>
    );
  }

  return (
    <div className="usuarios-salud-page" style={{ width: '100%', boxSizing: 'border-box', overflowX: 'hidden' }}>
      <div className="page-header">
        <h1>👥 Gestión de Pacientes</h1>
        <p className="subtitle">
          Administra los pacientes registrados en esta clínica. 
          Los pacientes se sincronizan automáticamente con el INUS central.
        </p>
      </div>
      
      {mensaje.texto && (
        <div className={`mensaje ${mensaje.tipo}`}>
          {mensaje.texto}
        </div>
      )}
      
      <div className="actions-bar">
        {!showForm && (
          <button 
            className="btn-primary"
            onClick={() => setShowForm(true)}
            style={{
              background: `linear-gradient(135deg, ${config.colorPrimario} 0%, ${config.colorPrimario}dd 100%)`
            }}
          >
            + Agregar Paciente
          </button>
        )}
      </div>
      
      {showForm && (
        <div className="form-container">
          <h2>{editingId ? 'Editar Paciente' : 'Nuevo Paciente'}</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="ci">CI *</label>
                <input
                  id="ci"
                  type="text"
                  name="ci"
                  value={formData.ci}
                  onChange={handleInputChange}
                  placeholder="12345678"
                  required
                  disabled={editingId !== null}
                  maxLength="20"
                />
              </div>
              
              <div className="form-group">
                <label htmlFor="nombre">Nombre</label>
                <input
                  id="nombre"
                  type="text"
                  name="nombre"
                  value={formData.nombre}
                  onChange={handleInputChange}
                  placeholder="Juan"
                />
              </div>
              
              <div className="form-group">
                <label htmlFor="apellido">Apellido</label>
                <input
                  id="apellido"
                  type="text"
                  name="apellido"
                  value={formData.apellido}
                  onChange={handleInputChange}
                  placeholder="Pérez"
                />
              </div>
            </div>
            
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="fechaNacimiento">Fecha de Nacimiento</label>
                <input
                  id="fechaNacimiento"
                  type="date"
                  name="fechaNacimiento"
                  value={formData.fechaNacimiento}
                  onChange={handleInputChange}
                />
              </div>
              
              <div className="form-group">
                <label htmlFor="telefono">Teléfono</label>
                <input
                  id="telefono"
                  type="tel"
                  name="telefono"
                  value={formData.telefono}
                  onChange={handleInputChange}
                  placeholder="099 123 456"
                />
              </div>
              
              <div className="form-group">
                <label htmlFor="email">Email</label>
                <input
                  id="email"
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  placeholder="juan@ejemplo.com"
                />
              </div>
            </div>
            
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="direccion">Dirección</label>
                <input
                  id="direccion"
                  type="text"
                  name="direccion"
                  value={formData.direccion}
                  onChange={handleInputChange}
                  placeholder="Av. Italia 1234"
                />
              </div>
              
              <div className="form-group">
                <label htmlFor="departamento">Departamento</label>
                <select
                  id="departamento"
                  name="departamento"
                  value={formData.departamento}
                  onChange={handleInputChange}
                >
                  <option value="">-- Seleccione --</option>
                  {departamentos.map(dept => (
                    <option key={dept} value={dept}>
                      {formatDepartamento(dept)}
                    </option>
                  ))}
                </select>
              </div>
              
              <div className="form-group">
                <label htmlFor="localidad">Localidad</label>
                <input
                  id="localidad"
                  type="text"
                  name="localidad"
                  value={formData.localidad}
                  onChange={handleInputChange}
                  placeholder="Pocitos"
                />
              </div>
            </div>
            
            <div className="form-actions">
              <button 
                type="submit" 
                className="btn-primary"
                style={{
                  background: `linear-gradient(135deg, ${config.colorPrimario} 0%, ${config.colorPrimario}dd 100%)`
                }}
              >
                {editingId ? 'Actualizar' : 'Guardar'}
              </button>
              <button 
                type="button" 
                className="btn-secondary"
                onClick={handleCancelar}
                style={{
                  color: config.colorPrimario,
                  border: `2px solid ${config.colorPrimario}`
                }}
              >
                Cancelar
              </button>
            </div>
          </form>
        </div>
      )}
      
      <div className="table-container">
        <h2>Listado de Pacientes ({usuarios.length})</h2>
        
        {(() => {
          if (loading) {
            return <div className="loading">Cargando pacientes...</div>;
          }
          if (usuarios.length === 0) {
            return (
              <div className="empty-state">
                <p>No hay pacientes registrados en esta clínica.</p>
                <p>Haz clic en "Agregar Paciente" para registrar el primero.</p>
              </div>
            );
          }
          return (
          <div style={{ overflowX: 'auto', width: '100%' }}>
            <table className="data-table">
            <thead>
              <tr>
                <th>CI</th>
                <th>Nombre Completo</th>
                <th>Fecha Nacimiento</th>
                <th>Teléfono</th>
                <th>Email</th>
                <th>Departamento</th>
                <th>HCEN ID</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map(usuario => (
                <tr key={usuario.id}>
                  <td><strong>{usuario.ci}</strong></td>
                  <td>
                    {(() => {
                      if (usuario.apellido && usuario.nombre) {
                        return `${usuario.apellido}, ${usuario.nombre}`;
                      }
                      if (usuario.apellido) {
                        return usuario.apellido;
                      }
                      if (usuario.nombre) {
                        return usuario.nombre;
                      }
                      return '-';
                    })()}
                  </td>
                  <td>{usuario.fechaNacimiento || '-'}</td>
                  <td>{usuario.telefono || '-'}</td>
                  <td>{usuario.email || '-'}</td>
                  <td>{formatDepartamento(usuario.departamento)}</td>
                  <td>
                    {usuario.hcenUserId ? (
                      <span className="badge badge-success">
                        #{usuario.hcenUserId}
                      </span>
                    ) : (
                      <span className="badge badge-warning">
                        Sin sincronizar
                      </span>
                    )}
                  </td>
                  <td>
                    <button 
                      className="btn-sm btn-edit"
                      onClick={() => handleEditar(usuario)}
                      title="Editar paciente"
                      style={{
                        background: config.colorPrimario
                      }}
                    >
                      ✏️ Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>
          );
        })()}
      </div>
    </div>
  );
}

export default UsuariosSaludPage;

