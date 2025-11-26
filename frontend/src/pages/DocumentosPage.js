import React, { useState } from 'react';
import { useParams } from 'react-router-dom';

function DocumentosPage() {
  const { tenantId } = useParams();
  const [ciPaciente, setCiPaciente] = useState('');
  const [documentos, setDocumentos] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [showSolicitarAccesoModal, setShowSolicitarAccesoModal] = useState(false);
  const [solicitarAccesoForm, setSolicitarAccesoForm] = useState({
    ciPaciente: '',
    motivo: ''
  });
  const [uploadForm, setUploadForm] = useState({
    archivo: null,
    ciPaciente: '',
    tipoDocumento: 'EVALUACION',
    descripcion: ''
  });
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createForm, setCreateForm] = useState({
    ciPaciente: '',
    contenido: '',
    tipoDocumento: 'EVALUACION',
    descripcion: '',
    titulo: '',
    autor: ''
  });

  const buscarDocumentos = async () => {
    if (!ciPaciente.trim()) {
      setError('Por favor ingrese un CI');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/hcen-web/api/documentos-pdf/paciente/${ciPaciente}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        setDocumentos(data);
      } else if (response.status === 404) {
        setDocumentos([]);
        setError('No se encontraron documentos para este paciente');
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Error al buscar documentos');
      }
    } catch (err) {
      setError('Error de conexión. Verifique que el servidor esté disponible.');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const descargarDocumento = async (documentoId) => {
    try {
      // Usar el mismo flujo que el frontend HCEN: llamar al backend HCEN con el ID de la metadata
      // El backend HCEN hará proxy al componente periférico usando la URI almacenada
      const backendUrl = process.env.REACT_APP_HCEN_BACKEND_URL || 'http://localhost:8080';
      const response = await fetch(`${backendUrl}/api/metadatos-documento/${documentoId}/descargar`, {
        method: 'GET',
        credentials: 'include', // Incluir cookies para autenticación
      });

      if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `documento-${documentoId}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      } else if (response.status === 403) {
        const errorText = await response.text();
        alert(`No tiene permiso para acceder a este documento. Use el botón "Solicitar Acceso" para solicitar acceso.`);
      } else {
        const errorText = await response.text();
        alert(`Error al descargar el documento: ${errorText || 'Error desconocido'}`);
      }
    } catch (err) {
      alert('Error al descargar el documento: ' + err.message);
      console.error('Error:', err);
    }
  };

  const handleSolicitarAccesoSubmit = async (e) => {
    e.preventDefault();

    if (!solicitarAccesoForm.ciPaciente.trim()) {
      setError('Por favor ingrese el CI del paciente');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      // Usar el backend del componente periférico (que hará proxy al backend HCEN Central)
      const backendBase = process.env.REACT_APP_BACKEND_URL || '';
      const body = {
        pacienteCI: solicitarAccesoForm.ciPaciente.trim(),
        // No enviar documentoId ni tipoDocumento - es para TODOS los documentos del paciente
        motivo: solicitarAccesoForm.motivo || `Solicitud de acceso a todos los documentos del paciente ${solicitarAccesoForm.ciPaciente.trim()}`
      };

      const token = localStorage.getItem('token');
      const response = await fetch(`${backendBase}/hcen-web/api/documentos/solicitar-acceso`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      });

      if (response.ok) {
        alert('Solicitud de acceso enviada exitosamente. El paciente podrá aprobarla desde su perfil en HCEN Central.');
        setShowSolicitarAccesoModal(false);
        setSolicitarAccesoForm({ ciPaciente: '', motivo: '' });
        setError(null);
      } else {
        const errorData = await response.json().catch(() => ({ error: 'Error desconocido' }));
        setError(`Error al enviar solicitud: ${errorData.error || 'Error desconocido'}`);
      }
    } catch (err) {
      setError('Error al enviar solicitud de acceso: ' + err.message);
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleUploadSubmit = async (e) => {
    e.preventDefault();

    if (!uploadForm.archivo || !uploadForm.ciPaciente.trim()) {
      setError('Por favor complete todos los campos requeridos');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('token');
      const formData = new FormData();
      formData.append('archivo', uploadForm.archivo);
      formData.append('ciPaciente', uploadForm.ciPaciente);
      formData.append('tipoDocumento', uploadForm.tipoDocumento);
      if (uploadForm.descripcion) {
        formData.append('descripcion', uploadForm.descripcion);
      }

      const response = await fetch(`/hcen-web/api/documentos-pdf/upload`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
          // NO incluir 'Content-Type' - el navegador lo establece automáticamente con el boundary correcto para FormData
        },
        body: formData
      });

      if (response.ok) {
        const data = await response.json();
        alert('Documento subido exitosamente');
        setShowUploadModal(false);
        setUploadForm({
          archivo: null,
          ciPaciente: '',
          tipoDocumento: 'EVALUACION',
          descripcion: ''
        });
        // Si el CI coincide, actualizar la lista
        if (uploadForm.ciPaciente === ciPaciente) {
          buscarDocumentos();
        }
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Error al subir el documento');
      }
    } catch (err) {
      setError('Error de conexión al subir el documento');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();

    if (!createForm.ciPaciente.trim() || !createForm.contenido.trim()) {
      setError('Por favor complete todos los campos requeridos');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('token');
      const body = {
        ciPaciente: createForm.ciPaciente,
        contenido: createForm.contenido,
        tipoDocumento: createForm.tipoDocumento || 'EVALUACION'
      };

      // Agregar campos opcionales solo si tienen valor
      if (createForm.descripcion && createForm.descripcion.trim()) {
        body.descripcion = createForm.descripcion;
      }
      if (createForm.titulo && createForm.titulo.trim()) {
        body.titulo = createForm.titulo;
      }
      if (createForm.autor && createForm.autor.trim()) {
        body.autor = createForm.autor;
      }

      const response = await fetch(`/hcen-web/api/documentos/completo`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      });

      if (response.ok) {
        const data = await response.json();
        alert('Documento creado exitosamente. El contenido se ha convertido automáticamente a PDF.');
        setShowCreateModal(false);
        setCreateForm({
          ciPaciente: '',
          contenido: '',
          tipoDocumento: 'EVALUACION',
          descripcion: '',
          titulo: '',
          autor: ''
        });
        // Si el CI coincide, actualizar la lista
        if (createForm.ciPaciente === ciPaciente) {
          buscarDocumentos();
        }
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Error al crear el documento');
      }
    } catch (err) {
      setError('Error de conexión al crear el documento: ' + err.message);
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('es-UY', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateString;
    }
  };

  return (
    <div>
      {/* Header con búsqueda */}
      <div style={styles.headerCard}>
        <div style={styles.headerContent}>
          <div>
            <h2 style={styles.headerTitle}>Documentos Clínicos</h2>
            <p style={styles.headerSubtitle}>
              Busque y gestione documentos PDF de pacientes
            </p>
          </div>
          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={() => {
                setShowCreateModal(true);
                setCreateForm(prev => ({ ...prev, ciPaciente: ciPaciente }));
              }}
              style={styles.createButton}
            >
              ✏️ Crear Documento
            </button>
          <button
            onClick={() => {
              setShowUploadModal(true);
              setUploadForm(prev => ({ ...prev, ciPaciente: ciPaciente }));
            }}
            style={styles.uploadButton}
          >
              📤 Subir PDF
            </button>
            <button
              onClick={() => setShowSolicitarAccesoModal(true)}
              style={styles.solicitarAccesoButton}
            >
              🔓 Solicitar Acceso
          </button>
          </div>
        </div>

        {/* Búsqueda por CI */}
        <div style={styles.searchSection}>
          <div style={styles.searchInputGroup}>
            <input
              type="text"
              placeholder="Ingrese CI del paciente"
              value={ciPaciente}
              onChange={(e) => setCiPaciente(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && buscarDocumentos()}
              style={styles.searchInput}
            />
            <button
              onClick={buscarDocumentos}
              disabled={loading}
              style={styles.searchButton}
            >
              {loading ? '🔍 Buscando...' : '🔍 Buscar'}
            </button>
          </div>
        </div>
      </div>

      {/* Error message */}
      {error && (
        <div style={styles.errorCard}>
          <span style={styles.errorIcon}>⚠️</span>
          <span>{error}</span>
        </div>
      )}

      {/* Lista de documentos */}
      {documentos.length > 0 && (
        <div style={styles.documentosCard}>
          <h3 style={styles.documentosTitle}>
            Documentos encontrados ({documentos.length})
          </h3>
          <div style={styles.documentosList}>
            {documentos.map((doc, index) => (
              <div key={doc.id || index} style={styles.documentoItem}>
                <div style={styles.documentoIcon}>📄</div>
                <div style={styles.documentoInfo}>
                  <div style={styles.documentoHeader}>
                    <span style={styles.documentoTipo}>{doc.tipoDocumento || 'EVALUACION'}</span>
                    <span style={styles.documentoFecha}>
                      {formatDate(doc.fechaCreacion)}
                    </span>
                  </div>
                  {doc.descripcion && (
                    <div style={styles.documentoDescripcion}>{doc.descripcion}</div>
                  )}
                  <div style={styles.documentoMeta}>
                    <span>CI: {doc.ciPaciente}</span>
                    {doc.profesionalId && (
                      <span>• Profesional: {doc.profesionalId}</span>
                    )}
                  </div>
                </div>
                <button
                  onClick={() => descargarDocumento(doc.id)}
                  style={styles.downloadButton}
                  title="Descargar PDF"
                  disabled={!doc.id}
                >
                  ⬇️ Descargar
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {documentos.length === 0 && !loading && ciPaciente && !error && (
        <div style={styles.emptyCard}>
          <div style={styles.emptyIcon}>📭</div>
          <div style={styles.emptyText}>
            No se encontraron documentos para el CI: {ciPaciente}
          </div>
        </div>
      )}

      {/* Modal de creación de documento completo */}
      {showCreateModal && (
        <div style={styles.modalOverlay} onClick={() => setShowCreateModal(false)}>
          <div style={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <div style={styles.modalHeader}>
              <h3 style={styles.modalTitle}>Crear Documento Clínico</h3>
              <button
                onClick={() => setShowCreateModal(false)}
                style={styles.modalClose}
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreateSubmit} style={styles.modalForm}>
              <div style={styles.formGroup}>
                <label style={styles.formLabel}>CI del Paciente *</label>
                <input
                  type="text"
                  value={createForm.ciPaciente}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, ciPaciente: e.target.value }))}
                  style={styles.formInput}
                  required
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Título (opcional)</label>
                <input
                  type="text"
                  value={createForm.titulo}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, titulo: e.target.value }))}
                  style={styles.formInput}
                  placeholder="Título del documento"
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Contenido del Documento *</label>
                <textarea
                  value={createForm.contenido}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, contenido: e.target.value }))}
                  style={{ ...styles.formTextarea, minHeight: '200px' }}
                  placeholder="Escriba el contenido del documento clínico aquí. Este contenido se convertirá automáticamente a PDF..."
                  required
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Tipo de Documento</label>
                <select
                  value={createForm.tipoDocumento}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, tipoDocumento: e.target.value }))}
                  style={styles.formInput}
                >
                  <option value="EVALUACION">Evaluación</option>
                  <option value="INFORME">Informe</option>
                  <option value="RECETA">Receta</option>
                  <option value="OTRO">Otro</option>
                </select>
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Descripción (opcional)</label>
                <textarea
                  value={createForm.descripcion}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, descripcion: e.target.value }))}
                  style={styles.formTextarea}
                  rows="3"
                  placeholder="Descripción breve del documento"
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Autor (opcional)</label>
                <input
                  type="text"
                  value={createForm.autor}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, autor: e.target.value }))}
                  style={styles.formInput}
                  placeholder="Nombre del autor (por defecto se usará su nombre de profesional)"
                />
              </div>

              <div style={styles.modalActions}>
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  style={styles.cancelButton}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  style={styles.submitButton}
                >
                  {loading ? 'Creando...' : 'Crear Documento'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal de subida */}
      {showUploadModal && (
        <div style={styles.modalOverlay} onClick={() => setShowUploadModal(false)}>
          <div style={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <div style={styles.modalHeader}>
              <h3 style={styles.modalTitle}>Subir Documento PDF</h3>
              <button
                onClick={() => setShowUploadModal(false)}
                style={styles.modalClose}
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleUploadSubmit} style={styles.modalForm}>
              <div style={styles.formGroup}>
                <label style={styles.formLabel}>CI del Paciente *</label>
                <input
                  type="text"
                  value={uploadForm.ciPaciente}
                  onChange={(e) => setUploadForm(prev => ({ ...prev, ciPaciente: e.target.value }))}
                  style={styles.formInput}
                  required
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Archivo PDF *</label>
                <input
                  type="file"
                  accept="application/pdf"
                  onChange={(e) => setUploadForm(prev => ({ ...prev, archivo: e.target.files[0] }))}
                  style={styles.formInput}
                  required
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Tipo de Documento</label>
                <select
                  value={uploadForm.tipoDocumento}
                  onChange={(e) => setUploadForm(prev => ({ ...prev, tipoDocumento: e.target.value }))}
                  style={styles.formInput}
                >
                  <option value="EVALUACION">Evaluación</option>
                  <option value="INFORME">Informe</option>
                  <option value="RECETA">Receta</option>
                  <option value="OTRO">Otro</option>
                </select>
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Descripción (opcional)</label>
                <textarea
                  value={uploadForm.descripcion}
                  onChange={(e) => setUploadForm(prev => ({ ...prev, descripcion: e.target.value }))}
                  style={styles.formTextarea}
                  rows="3"
                />
              </div>

              <div style={styles.modalActions}>
                <button
                  type="button"
                  onClick={() => setShowUploadModal(false)}
                  style={styles.cancelButton}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  style={styles.submitButton}
                >
                  {loading ? 'Subiendo...' : 'Subir Documento'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal para solicitar acceso */}
      {showSolicitarAccesoModal && (
        <div style={styles.modalOverlay} onClick={() => setShowSolicitarAccesoModal(false)}>
          <div style={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <div style={styles.modalHeader}>
              <h3 style={styles.modalTitle}>Solicitar Acceso a Documentos</h3>
              <button
                onClick={() => setShowSolicitarAccesoModal(false)}
                style={styles.modalClose}
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleSolicitarAccesoSubmit} style={styles.modalForm}>
              <div style={styles.formGroup}>
                <label style={styles.formLabel}>CI del Paciente *</label>
                <input
                  type="text"
                  value={solicitarAccesoForm.ciPaciente}
                  onChange={(e) => setSolicitarAccesoForm(prev => ({ ...prev, ciPaciente: e.target.value }))}
                  style={styles.formInput}
                  placeholder="Ingrese el CI del paciente"
                  required
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Motivo (opcional)</label>
                <textarea
                  value={solicitarAccesoForm.motivo}
                  onChange={(e) => setSolicitarAccesoForm(prev => ({ ...prev, motivo: e.target.value }))}
                  style={{...styles.formInput, minHeight: '100px', resize: 'vertical'}}
                  placeholder="Motivo de la solicitud de acceso (opcional)"
                />
              </div>

              <div style={styles.formActions}>
                <button
                  type="button"
                  onClick={() => {
                    setShowSolicitarAccesoModal(false);
                    setSolicitarAccesoForm({ ciPaciente: '', motivo: '' });
                  }}
                  style={styles.cancelButton}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  style={styles.submitButton}
                >
                  {loading ? 'Enviando...' : 'Solicitar Acceso'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

const styles = {
  headerCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '24px',
    marginBottom: '24px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  headerContent: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px'
  },
  headerTitle: {
    margin: '0 0 4px 0',
    fontSize: '24px',
    fontWeight: '700',
    color: '#111827'
  },
  headerSubtitle: {
    margin: 0,
    fontSize: '14px',
    color: '#6b7280'
  },
  createButton: {
    backgroundColor: '#10b981',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 24px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '8px'
  },
  uploadButton: {
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 24px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '8px'
  },
  solicitarAccesoButton: {
    backgroundColor: '#fbbf24', // Amarillo
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 24px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '8px'
  },
  searchSection: {
    marginTop: '20px'
  },
  searchInputGroup: {
    display: 'flex',
    gap: '12px'
  },
  searchInput: {
    flex: 1,
    padding: '12px 16px',
    border: '2px solid #e5e7eb',
    borderRadius: '8px',
    fontSize: '15px',
    outline: 'none',
    transition: 'border-color 0.2s'
  },
  searchButton: {
    padding: '12px 24px',
    backgroundColor: '#10b981',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer'
  },
  errorCard: {
    backgroundColor: '#fef2f2',
    border: '1px solid #fecaca',
    borderRadius: '8px',
    padding: '16px',
    marginBottom: '24px',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    color: '#991b1b'
  },
  errorIcon: {
    fontSize: '20px'
  },
  documentosCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '24px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  documentosTitle: {
    margin: '0 0 20px 0',
    fontSize: '20px',
    fontWeight: '700',
    color: '#111827'
  },
  documentosList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px'
  },
  documentoItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
    padding: '16px',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    transition: 'all 0.2s'
  },
  documentoIcon: {
    fontSize: '32px'
  },
  documentoInfo: {
    flex: 1
  },
  documentoHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px'
  },
  documentoTipo: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#111827'
  },
  documentoFecha: {
    fontSize: '13px',
    color: '#6b7280'
  },
  documentoDescripcion: {
    fontSize: '14px',
    color: '#374151',
    marginBottom: '8px'
  },
  documentoMeta: {
    fontSize: '13px',
    color: '#9ca3af',
    display: 'flex',
    gap: '12px'
  },
  downloadButton: {
    backgroundColor: '#10b981',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: '10px 20px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '8px'
  },
  emptyCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '48px',
    textAlign: 'center',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  emptyIcon: {
    fontSize: '64px',
    marginBottom: '16px'
  },
  emptyText: {
    fontSize: '16px',
    color: '#6b7280'
  },
  modalOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000
  },
  modalContent: {
    backgroundColor: 'white',
    borderRadius: '12px',
    width: '90%',
    maxWidth: '600px',
    maxHeight: '90vh',
    overflow: 'auto'
  },
  modalHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '24px',
    borderBottom: '1px solid #e5e7eb'
  },
  modalTitle: {
    margin: 0,
    fontSize: '20px',
    fontWeight: '700',
    color: '#111827'
  },
  modalClose: {
    backgroundColor: 'transparent',
    border: 'none',
    fontSize: '24px',
    cursor: 'pointer',
    color: '#6b7280'
  },
  modalForm: {
    padding: '24px'
  },
  formGroup: {
    marginBottom: '20px'
  },
  formLabel: {
    display: 'block',
    marginBottom: '8px',
    fontSize: '14px',
    fontWeight: '600',
    color: '#374151'
  },
  formInput: {
    width: '100%',
    padding: '12px',
    border: '2px solid #e5e7eb',
    borderRadius: '8px',
    fontSize: '15px',
    outline: 'none',
    boxSizing: 'border-box'
  },
  formTextarea: {
    width: '100%',
    padding: '12px',
    border: '2px solid #e5e7eb',
    borderRadius: '8px',
    fontSize: '15px',
    outline: 'none',
    resize: 'vertical',
    fontFamily: 'inherit',
    boxSizing: 'border-box'
  },
  modalActions: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '12px',
    marginTop: '24px'
  },
  cancelButton: {
    padding: '12px 24px',
    backgroundColor: '#f3f4f6',
    color: '#374151',
    border: 'none',
    borderRadius: '8px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer'
  },
  submitButton: {
    padding: '12px 24px',
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer'
  }
};

export default DocumentosPage;

