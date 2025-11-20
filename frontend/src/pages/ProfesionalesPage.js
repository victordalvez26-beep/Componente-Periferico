import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { getApiUrl } from '../utils/api';

function ProfesionalesPage() {
  const { tenantId } = useParams();
  const [profesionales, setProfesionales] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    nickname: '',
    password: '',
    nombre: '',
    email: '',
    especialidad: '',
    documento: '',
    telefono: ''
  });
  const [message, setMessage] = useState({ type: '', text: '' });

  // Especialidades médicas (deben coincidir EXACTAMENTE con el enum del backend)
  const especialidades = [
    { value: 'MEDICINA_GENERAL', label: 'Medicina General' },
    { value: 'MEDICINA_INTERNA', label: 'Medicina Interna' },
    { value: 'MEDICINA_FAMILIAR', label: 'Medicina Familiar' },
    { value: 'CARDIOLOGIA', label: 'Cardiología' },
    { value: 'NEUMOLOGIA', label: 'Neumología' },
    { value: 'GASTROENTEROLOGIA', label: 'Gastroenterología' },
    { value: 'NEFROLOGIA', label: 'Nefrología' },
    { value: 'ENDOCRINOLOGIA', label: 'Endocrinología' },
    { value: 'HEMATOLOGIA', label: 'Hematología' },
    { value: 'ONCOLOGIA', label: 'Oncología' },
    { value: 'NEUROLOGIA', label: 'Neurología' },
    { value: 'PSIQUIATRIA', label: 'Psiquiatría' },
    { value: 'PEDIATRIA', label: 'Pediatría' },
    { value: 'GINECOLOGIA', label: 'Ginecología' },
    { value: 'OBSTETRICIA', label: 'Obstetricia' },
    { value: 'DERMATOLOGIA', label: 'Dermatología' },
    { value: 'OFTALMOLOGIA', label: 'Oftalmología' },
    { value: 'OTORRINOLARINGOLOGIA', label: 'Otorrinolaringología' },
    { value: 'UROLOGIA', label: 'Urología' },
    { value: 'ORTOPEDIA', label: 'Ortopedia' },
    { value: 'TRAUMATOLOGIA', label: 'Traumatología' },
    { value: 'CIRUGIA_GENERAL', label: 'Cirugía General' },
    { value: 'CIRUGIA_PLASTICA', label: 'Cirugía Plástica' },
    { value: 'ANESTESIOLOGIA', label: 'Anestesiología' },
    { value: 'RADIOLOGIA', label: 'Radiología' },
    { value: 'ODONTOLOGIA', label: 'Odontología' },
    { value: 'FISIATRIA', label: 'Fisiatría' },
    { value: 'NUTRICION', label: 'Nutrición' },
    { value: 'PSICOLOGIA_CLINICA', label: 'Psicología Clínica' },
    { value: 'ENFERMERIA', label: 'Enfermería' }
  ];

  useEffect(() => {
    loadProfesionales();
  }, [tenantId]);

  const loadProfesionales = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const res = await fetch(getApiUrl(`/hcen-web/api/profesionales?tenantId=${tenantId}`), {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (res.ok) {
        const data = await res.json();
        setProfesionales(data);
      }
    } catch (err) {
      showMessage('error', 'Error al cargar profesionales');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const token = localStorage.getItem('token');
      const url = editingId
        ? getApiUrl(`/hcen-web/api/profesionales/${editingId}`)
        : getApiUrl(`/hcen-web/api/profesionales`);
      
      const method = editingId ? 'PUT' : 'POST';
      
      const res = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          ...formData,
          tenantId,
          role: 'PROFESIONAL'
        })
      });

      if (res.ok) {
        showMessage('success', editingId ? 'Profesional actualizado' : 'Profesional creado');
        resetForm();
        loadProfesionales();
      } else {
        const error = await res.json();
        showMessage('error', error.message || 'Error al guardar');
      }
    } catch (err) {
      showMessage('error', 'Error de conexión');
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (prof) => {
    setFormData({
      nickname: prof.nickname,
      password: '',
      nombre: prof.nombre || '',
      email: prof.email || '',
      especialidad: prof.especialidad || '',
      documento: prof.documento || '',
      telefono: prof.telefono || ''
    });
    setEditingId(prof.id);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('¿Está seguro de eliminar este profesional?')) return;

    try {
      const token = localStorage.getItem('token');
      const res = await fetch(getApiUrl(`/hcen-web/api/profesionales/${id}`), {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (res.ok) {
        showMessage('success', 'Profesional eliminado');
        loadProfesionales();
      } else {
        showMessage('error', 'Error al eliminar');
      }
    } catch (err) {
      showMessage('error', 'Error de conexión');
    }
  };

  const resetForm = () => {
    setFormData({
      nickname: '',
      password: '',
      nombre: '',
      email: '',
      especialidad: '',
      documento: '',
      telefono: ''
    });
    setEditingId(null);
    setShowForm(false);
  };

  const showMessage = (type, text) => {
    setMessage({ type, text });
    setTimeout(() => setMessage({ type: '', text: '' }), 5000);
  };

  return (
    <div>
      {/* Header Actions */}
      <div style={styles.header}>
        <button
          onClick={() => setShowForm(!showForm)}
          style={styles.addButton}
        >
          {showForm ? '❌ Cancelar' : '➕ Nuevo Profesional'}
        </button>
      </div>

      {/* Message */}
      {message.text && (
        <div style={{
          ...styles.message,
          backgroundColor: message.type === 'success' ? '#d1fae5' : '#fee2e2',
          color: message.type === 'success' ? '#065f46' : '#991b1b'
        }}>
          {message.text}
        </div>
      )}

      {/* Form */}
      {showForm && (
        <div style={styles.formCard}>
          <h3 style={styles.formTitle}>
            {editingId ? '✏️ Editar Profesional' : '➕ Nuevo Profesional'}
          </h3>
          <form onSubmit={handleSubmit}>
            <div style={styles.formGrid}>
              <div style={styles.formGroup}>
                <label style={styles.label}>Nombre Completo *</label>
                <input
                  type="text"
                  value={formData.nombre}
                  onChange={(e) => setFormData({...formData, nombre: e.target.value})}
                  style={styles.input}
                  required
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.label}>Usuario (nickname) *</label>
                <input
                  type="text"
                  value={formData.nickname}
                  onChange={(e) => setFormData({...formData, nickname: e.target.value})}
                  style={styles.input}
                  required
                  disabled={!!editingId}
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.label}>
                  Contraseña {editingId ? '(dejar vacío para no cambiar)' : '*'}
                </label>
                <input
                  type="password"
                  value={formData.password}
                  onChange={(e) => setFormData({...formData, password: e.target.value})}
                  style={styles.input}
                  required={!editingId}
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.label}>Especialidad *</label>
                <select
                  value={formData.especialidad}
                  onChange={(e) => setFormData({...formData, especialidad: e.target.value})}
                  style={styles.input}
                  required
                >
                  <option value="">Seleccione...</option>
                  {especialidades.map(esp => (
                    <option key={esp.value} value={esp.value}>{esp.label}</option>
                  ))}
                </select>
              </div>

              <div style={styles.formGroup}>
                <label style={styles.label}>Email *</label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({...formData, email: e.target.value})}
                  style={styles.input}
                  required
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.label}>Documento</label>
                <input
                  type="text"
                  value={formData.documento}
                  onChange={(e) => setFormData({...formData, documento: e.target.value})}
                  style={styles.input}
                  placeholder="CI o Pasaporte"
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.label}>Teléfono</label>
                <input
                  type="tel"
                  value={formData.telefono}
                  onChange={(e) => setFormData({...formData, telefono: e.target.value})}
                  style={styles.input}
                  placeholder="099 123 456"
                />
              </div>
            </div>

            <div style={styles.formActions}>
              <button type="submit" style={styles.saveButton} disabled={loading}>
                {loading ? 'Guardando...' : editingId ? '💾 Actualizar' : '➕ Crear'}
              </button>
              <button type="button" onClick={resetForm} style={styles.cancelButton}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Table */}
      <div style={styles.tableCard}>
        <h3 style={styles.tableTitle}>📋 Profesionales Registrados ({profesionales.length})</h3>
        {loading ? (
          <div style={styles.loading}>Cargando...</div>
        ) : profesionales.length === 0 ? (
          <div style={styles.empty}>
            No hay profesionales registrados. Agregue el primero.
          </div>
        ) : (
          <div style={styles.tableWrapper}>
            <table style={styles.table}>
              <thead>
                <tr>
                  <th style={styles.th}>Nombre</th>
                  <th style={styles.th}>Usuario</th>
                  <th style={styles.th}>Especialidad</th>
                  <th style={styles.th}>Email</th>
                  <th style={styles.th}>Documento</th>
                  <th style={styles.th}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {profesionales.map((prof) => (
                  <tr key={prof.id} style={styles.tr}>
                    <td style={styles.td}>
                      <div style={styles.profName}>
                        <div style={styles.avatar}>
                          {prof.nombre?.charAt(0) || 'P'}
                        </div>
                        <span>{prof.nombre || 'Sin nombre'}</span>
                      </div>
                    </td>
                    <td style={styles.td}>{prof.nickname}</td>
                    <td style={styles.td}>
                      <span style={styles.badge}>{prof.especialidad || 'N/A'}</span>
                    </td>
                    <td style={styles.td}>{prof.email || 'N/A'}</td>
                    <td style={styles.td}>{prof.documento || 'N/A'}</td>
                    <td style={styles.td}>
                      <button
                        onClick={() => handleEdit(prof)}
                        style={styles.editBtn}
                        title="Editar"
                      >
                        ✏️
                      </button>
                      <button
                        onClick={() => handleDelete(prof.id)}
                        style={styles.deleteBtn}
                        title="Eliminar"
                      >
                        🗑️
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

const styles = {
  header: {
    marginBottom: '24px',
    display: 'flex',
    justifyContent: 'flex-end'
  },
  addButton: {
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    padding: '12px 24px',
    borderRadius: '8px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  message: {
    padding: '16px',
    borderRadius: '8px',
    marginBottom: '24px',
    fontSize: '15px',
    fontWeight: '500'
  },
  formCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '32px',
    marginBottom: '24px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  formTitle: {
    margin: '0 0 24px 0',
    fontSize: '20px',
    fontWeight: '700',
    color: '#111827'
  },
  formGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
    gap: '20px',
    marginBottom: '24px'
  },
  formGroup: {
    display: 'flex',
    flexDirection: 'column'
  },
  label: {
    marginBottom: '8px',
    fontSize: '14px',
    fontWeight: '600',
    color: '#374151'
  },
  input: {
    padding: '10px 14px',
    fontSize: '15px',
    border: '2px solid #e5e7eb',
    borderRadius: '8px',
    outline: 'none',
    transition: 'border-color 0.2s'
  },
  formActions: {
    display: 'flex',
    gap: '12px',
    justifyContent: 'flex-end'
  },
  saveButton: {
    backgroundColor: '#10b981',
    color: 'white',
    border: 'none',
    padding: '12px 32px',
    borderRadius: '8px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer'
  },
  cancelButton: {
    backgroundColor: '#e5e7eb',
    color: '#374151',
    border: 'none',
    padding: '12px 32px',
    borderRadius: '8px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer'
  },
  tableCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '24px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  tableTitle: {
    margin: '0 0 20px 0',
    fontSize: '18px',
    fontWeight: '700',
    color: '#111827'
  },
  loading: {
    textAlign: 'center',
    padding: '40px',
    color: '#6b7280'
  },
  empty: {
    textAlign: 'center',
    padding: '40px',
    color: '#9ca3af',
    fontSize: '15px'
  },
  tableWrapper: {
    overflowX: 'auto'
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse'
  },
  th: {
    textAlign: 'left',
    padding: '12px',
    borderBottom: '2px solid #e5e7eb',
    fontSize: '13px',
    fontWeight: '600',
    color: '#6b7280',
    textTransform: 'uppercase'
  },
  tr: {
    borderBottom: '1px solid #f3f4f6',
    transition: 'background-color 0.2s'
  },
  td: {
    padding: '16px 12px',
    fontSize: '14px',
    color: '#374151'
  },
  profName: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px'
  },
  avatar: {
    width: '36px',
    height: '36px',
    borderRadius: '50%',
    backgroundColor: '#3b82f6',
    color: 'white',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: '700',
    fontSize: '14px'
  },
  badge: {
    display: 'inline-block',
    padding: '4px 12px',
    backgroundColor: '#e0e7ff',
    color: '#3730a3',
    borderRadius: '12px',
    fontSize: '13px',
    fontWeight: '500'
  },
  editBtn: {
    backgroundColor: 'transparent',
    border: 'none',
    fontSize: '18px',
    cursor: 'pointer',
    marginRight: '8px'
  },
  deleteBtn: {
    backgroundColor: 'transparent',
    border: 'none',
    fontSize: '18px',
    cursor: 'pointer'
  }
};

export default ProfesionalesPage;

