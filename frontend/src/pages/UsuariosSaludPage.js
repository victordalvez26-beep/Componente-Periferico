import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import './UsuariosSaludPage.css';

/**
 * Página para gestionar Usuarios de Salud (pacientes) de una clínica.
 * Permite crear, listar y ver detalles de pacientes.
 */
function UsuariosSaludPage() {
  const { tenantId } = useParams();
  const [usuarios, setUsuarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [mensaje, setMensaje] = useState({ tipo: '', texto: '' });
  
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
      const token = localStorage.getItem('jwt');
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
        mostrarMensaje('error', 'Error al cargar la lista de pacientes');
      }
    } catch (error) {
      console.error('Error:', error);
      mostrarMensaje('error', 'Error de conexión al cargar pacientes');
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
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validar CI
    if (!formData.ci || formData.ci.trim() === '') {
      mostrarMensaje('error', 'El CI es obligatorio');
      return;
    }
    
    try {
      const token = localStorage.getItem('jwt');
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
        mostrarMensaje('success', 
          editingId 
            ? 'Paciente actualizado correctamente' 
            : 'Paciente registrado correctamente (sincronizado con HCEN)'
        );
        setShowForm(false);
        resetForm();
        cargarUsuarios();
      } else {
        const errorData = await response.json();
        mostrarMensaje('error', errorData.error || 'Error al guardar el paciente');
      }
    } catch (error) {
      console.error('Error:', error);
      mostrarMensaje('error', 'Error de conexión al guardar');
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
  
  return (
    <div className="usuarios-salud-page">
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
                <label>CI *</label>
                <input
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
                <label>Nombre</label>
                <input
                  type="text"
                  name="nombre"
                  value={formData.nombre}
                  onChange={handleInputChange}
                  placeholder="Juan"
                />
              </div>
              
              <div className="form-group">
                <label>Apellido</label>
                <input
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
                <label>Fecha de Nacimiento</label>
                <input
                  type="date"
                  name="fechaNacimiento"
                  value={formData.fechaNacimiento}
                  onChange={handleInputChange}
                />
              </div>
              
              <div className="form-group">
                <label>Teléfono</label>
                <input
                  type="tel"
                  name="telefono"
                  value={formData.telefono}
                  onChange={handleInputChange}
                  placeholder="099 123 456"
                />
              </div>
              
              <div className="form-group">
                <label>Email</label>
                <input
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
                <label>Dirección</label>
                <input
                  type="text"
                  name="direccion"
                  value={formData.direccion}
                  onChange={handleInputChange}
                  placeholder="Av. Italia 1234"
                />
              </div>
              
              <div className="form-group">
                <label>Departamento</label>
                <input
                  type="text"
                  name="departamento"
                  value={formData.departamento}
                  onChange={handleInputChange}
                  placeholder="Montevideo"
                />
              </div>
              
              <div className="form-group">
                <label>Localidad</label>
                <input
                  type="text"
                  name="localidad"
                  value={formData.localidad}
                  onChange={handleInputChange}
                  placeholder="Pocitos"
                />
              </div>
            </div>
            
            <div className="form-actions">
              <button type="submit" className="btn-primary">
                {editingId ? 'Actualizar' : 'Guardar'}
              </button>
              <button 
                type="button" 
                className="btn-secondary"
                onClick={handleCancelar}
              >
                Cancelar
              </button>
            </div>
          </form>
        </div>
      )}
      
      <div className="table-container">
        <h2>Listado de Pacientes ({usuarios.length})</h2>
        
        {loading ? (
          <div className="loading">Cargando pacientes...</div>
        ) : usuarios.length === 0 ? (
          <div className="empty-state">
            <p>No hay pacientes registrados en esta clínica.</p>
            <p>Haz clic en "Agregar Paciente" para registrar el primero.</p>
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>CI</th>
                <th>Nombre Completo</th>
                <th>Fecha Nacimiento</th>
                <th>Teléfono</th>
                <th>Email</th>
                <th>HCEN ID</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map(usuario => (
                <tr key={usuario.id}>
                  <td><strong>{usuario.ci}</strong></td>
                  <td>
                    {usuario.apellido && usuario.nombre 
                      ? `${usuario.apellido}, ${usuario.nombre}`
                      : usuario.apellido || usuario.nombre || '-'}
                  </td>
                  <td>{usuario.fechaNacimiento || '-'}</td>
                  <td>{usuario.telefono || '-'}</td>
                  <td>{usuario.email || '-'}</td>
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
                    >
                      ✏️ Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default UsuariosSaludPage;

