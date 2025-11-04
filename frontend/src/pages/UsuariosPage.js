import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';

function UsuariosPage() {
  const { tenantId } = useParams();
  const [usuarios, setUsuarios] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    ci: '',
    nombre: '',
    apellido: '',
    email: '',
    fechaNacimiento: '',
    telefono: '',
    direccion: ''
  });

  useEffect(() => {
    loadUsuarios();
  }, [tenantId]);

  const loadUsuarios = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`/hcen-web/api/usuarios?tenantId=${tenantId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (res.ok) {
        const data = await res.json();
        setUsuarios(data);
      }
    } catch (err) {
      console.error('Error al cargar usuarios:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`/hcen-web/api/usuarios`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ ...formData, tenantId })
      });

      if (res.ok) {
        alert('Usuario registrado exitosamente en INUS');
        setShowForm(false);
        setFormData({ci: '', nombre: '', apellido: '', email: '', fechaNacimiento: '', telefono: '', direccion: ''});
        loadUsuarios();
      } else {
        alert('Error al registrar usuario');
      }
    } catch (err) {
      alert('Error de conexión');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h3 style={{ margin: 0, fontSize: '16px', color: '#6b7280' }}>
            Gestión de Usuarios de Salud (INUS)
          </h3>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          style={{
            backgroundColor: '#3b82f6',
            color: 'white',
            border: 'none',
            padding: '12px 24px',
            borderRadius: '8px',
            fontSize: '15px',
            fontWeight: '600',
            cursor: 'pointer'
          }}
        >
          {showForm ? '❌ Cancelar' : '➕ Registrar en INUS'}
        </button>
      </div>

      {showForm && (
        <div style={{
          backgroundColor: 'white',
          borderRadius: '12px',
          padding: '32px',
          marginBottom: '24px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
        }}>
          <h3 style={{ margin: '0 0 24px 0', fontSize: '20px', fontWeight: '700' }}>
            ➕ Registrar Usuario en INUS
          </h3>
          <form onSubmit={handleSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px', marginBottom: '24px' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '600' }}>
                  Cédula de Identidad *
                </label>
                <input
                  type="text"
                  value={formData.ci}
                  onChange={(e) => setFormData({...formData, ci: e.target.value})}
                  style={{ width: '100%', padding: '10px 14px', fontSize: '15px', border: '2px solid #e5e7eb', borderRadius: '8px' }}
                  placeholder="12345678"
                  required
                />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '600' }}>Nombre *</label>
                <input
                  type="text"
                  value={formData.nombre}
                  onChange={(e) => setFormData({...formData, nombre: e.target.value})}
                  style={{ width: '100%', padding: '10px 14px', fontSize: '15px', border: '2px solid #e5e7eb', borderRadius: '8px' }}
                  required
                />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '600' }}>Apellido *</label>
                <input
                  type="text"
                  value={formData.apellido}
                  onChange={(e) => setFormData({...formData, apellido: e.target.value})}
                  style={{ width: '100%', padding: '10px 14px', fontSize: '15px', border: '2px solid #e5e7eb', borderRadius: '8px' }}
                  required
                />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '600' }}>Email</label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({...formData, email: e.target.value})}
                  style={{ width: '100%', padding: '10px 14px', fontSize: '15px', border: '2px solid #e5e7eb', borderRadius: '8px' }}
                />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '600' }}>Fecha de Nacimiento *</label>
                <input
                  type="date"
                  value={formData.fechaNacimiento}
                  onChange={(e) => setFormData({...formData, fechaNacimiento: e.target.value})}
                  style={{ width: '100%', padding: '10px 14px', fontSize: '15px', border: '2px solid #e5e7eb', borderRadius: '8px' }}
                  required
                />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '600' }}>Teléfono</label>
                <input
                  type="tel"
                  value={formData.telefono}
                  onChange={(e) => setFormData({...formData, telefono: e.target.value})}
                  style={{ width: '100%', padding: '10px 14px', fontSize: '15px', border: '2px solid #e5e7eb', borderRadius: '8px' }}
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={loading}
              style={{
                backgroundColor: '#10b981',
                color: 'white',
                border: 'none',
                padding: '12px 32px',
                borderRadius: '8px',
                fontSize: '15px',
                fontWeight: '600',
                cursor: 'pointer'
              }}
            >
              {loading ? 'Registrando...' : '💾 Registrar en INUS'}
            </button>
          </form>
        </div>
      )}

      <div style={{ backgroundColor: 'white', borderRadius: '12px', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
        <h3 style={{ margin: '0 0 20px 0', fontSize: '18px', fontWeight: '700' }}>
          📋 Usuarios Registrados ({usuarios.length})
        </h3>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#6b7280' }}>Cargando...</div>
        ) : usuarios.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#9ca3af' }}>
            No hay usuarios registrados. Agregue el primero.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr>
                <th style={{ textAlign: 'left', padding: '12px', borderBottom: '2px solid #e5e7eb', fontSize: '13px', fontWeight: '600', color: '#6b7280' }}>CI</th>
                <th style={{ textAlign: 'left', padding: '12px', borderBottom: '2px solid #e5e7eb', fontSize: '13px', fontWeight: '600', color: '#6b7280' }}>Nombre</th>
                <th style={{ textAlign: 'left', padding: '12px', borderBottom: '2px solid #e5e7eb', fontSize: '13px', fontWeight: '600', color: '#6b7280' }}>Email</th>
                <th style={{ textAlign: 'left', padding: '12px', borderBottom: '2px solid #e5e7eb', fontSize: '13px', fontWeight: '600', color: '#6b7280' }}>Teléfono</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((user) => (
                <tr key={user.ci} style={{ borderBottom: '1px solid #f3f4f6' }}>
                  <td style={{ padding: '16px 12px', fontSize: '14px' }}>{user.ci}</td>
                  <td style={{ padding: '16px 12px', fontSize: '14px' }}>{user.nombre} {user.apellido}</td>
                  <td style={{ padding: '16px 12px', fontSize: '14px' }}>{user.email || 'N/A'}</td>
                  <td style={{ padding: '16px 12px', fontSize: '14px' }}>{user.telefono || 'N/A'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default UsuariosPage;

