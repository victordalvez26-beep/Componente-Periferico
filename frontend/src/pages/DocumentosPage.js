import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { getApiUrl, getToken } from '../utils/api';

function DocumentosPage() {
  const { tenantId } = useParams();
  const [ciPaciente, setCiPaciente] = useState('');
  const [documentos, setDocumentos] = useState([]);
  const [loading, setLoading] = useState(false);
  const [generandoResumen, setGenerandoResumen] = useState(false);
  const [resumen, setResumen] = useState(null);
  const [solicitandoAcceso, setSolicitandoAcceso] = useState(false);
  const [error, setError] = useState(null);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [uploadForm, setUploadForm] = useState({
    archivo: null,
    ciPaciente: '',
    tipoDocumento: 'EVALUACION',
    descripcion: '',
    titulo: ''
  });
  const [createForm, setCreateForm] = useState({
    ciPaciente: '',
    titulo: '',
    contenido: '',
    tipoDocumento: 'EVALUACION',
    descripcion: '',
    especialidad: ''
  });

  const buscarDocumentos = async () => {
    if (!ciPaciente.trim()) {
      setError('Por favor ingrese un CI');
      return;
    }

    setLoading(true);
    setError(null);
    setResumen(null);

    try {
      const token = getToken();
      if (!token) {
        setError('Debes iniciar sesión primero');
        setLoading(false);
        return;
      }

      const response = await fetch(getApiUrl(`/hcen-web/api/documentos-pdf/paciente/${ciPaciente}`), {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        setDocumentos(data || []);
        if (!data || data.length === 0) {
          setError(`No se encontraron ${data?.length || 0} documentos PDF`);
        }
      } else if (response.status === 404) {
        setDocumentos([]);
        setError('No se encontraron documentos para este paciente');
      } else {
        const errorData = await response.json().catch(() => ({ error: 'Error al buscar documentos' }));
        setError(errorData.error || 'Error al buscar documentos');
      }
    } catch (err) {
      setError('Error de conexión. Verifique que el servidor esté disponible.');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const generarResumen = async () => {
    if (!ciPaciente.trim()) {
      setError('Por favor ingrese un CI para generar el resumen');
      return;
    }

    setGenerandoResumen(true);
    setError(null);
    setResumen(null);

    try {
      const token = getToken();
      if (!token) {
        setError('Debes iniciar sesión primero');
        setGenerandoResumen(false);
        return;
      }

      const response = await fetch(getApiUrl(`/hcen-web/api/documentos/paciente/${ciPaciente}/resumen`), {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        setResumen(data.resumen || data);
      } else {
        const errorData = await response.json().catch(() => ({ error: 'Error al generar el resumen' }));
        setError(errorData.error || 'Error al generar el resumen');
      }
    } catch (err) {
      setError('Error de conexión al generar el resumen');
      console.error('Error:', err);
    } finally {
      setGenerandoResumen(false);
    }
  };

  const solicitarAccesoHistoriaClinica = async () => {
    if (!ciPaciente.trim()) {
      setError('Por favor ingrese un CI para solicitar acceso');
      return;
    }

    setSolicitandoAcceso(true);
    setError(null);

    try {
      const token = getToken();
      if (!token) {
        setError('Debes iniciar sesión primero');
        setSolicitandoAcceso(false);
        return;
      }

      const response = await fetch(getApiUrl(`/hcen-web/api/documentos/solicitar-acceso-historia-clinica`), {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          codDocumPaciente: ciPaciente,
          razonSolicitud: 'Solicitud de acceso a historia clínica completa'
        })
      });

      if (response.ok) {
        alert('Solicitud de acceso enviada correctamente');
      } else {
        const errorData = await response.json().catch(() => ({ error: 'Error al solicitar acceso' }));
        setError(errorData.error || 'Error al solicitar acceso');
      }
    } catch (err) {
      setError('Error de conexión al solicitar acceso');
      console.error('Error:', err);
    } finally {
      setSolicitandoAcceso(false);
    }
  };

  const descargarDocumento = async (documentoId) => {
    try {
      const token = getToken();
      if (!token) {
        setError('Debes iniciar sesión primero');
        return;
      }

      const response = await fetch(getApiUrl(`/hcen-web/api/documentos/${documentoId}/contenido`), {
        headers: {
          'Authorization': `Bearer ${token}`
        }
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
      } else {
        const errorData = await response.json().catch(() => ({ error: 'Error al descargar el documento' }));
        alert(errorData.error || 'Error al descargar el documento');
      }
    } catch (err) {
      alert('Error al descargar el documento');
      console.error('Error:', err);
    }
  };

  const verContenido = async (documentoId) => {
    try {
      const token = getToken();
      if (!token) {
        setError('Debes iniciar sesión primero');
        return;
      }

      const response = await fetch(getApiUrl(`/hcen-web/api/documentos/${documentoId}/contenido`), {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      } else {
        const errorData = await response.json().catch(() => ({ error: 'Error al ver el documento' }));
        alert(errorData.error || 'Error al ver el documento');
      }
    } catch (err) {
      alert('Error al ver el documento');
      console.error('Error:', err);
    }
  };

  const solicitarAccesoDocumento = async (documentoId, ciPaciente) => {
    try {
      const token = getToken();
      if (!token) {
        setError('Debes iniciar sesión primero');
        return;
      }

      const response = await fetch(getApiUrl(`/hcen-web/api/documentos/solicitar-acceso`), {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          codDocumPaciente: ciPaciente,
          documentoId: documentoId,
          razonSolicitud: 'Solicitud de acceso a documento específico'
        })
      });

      if (response.ok) {
        alert('Solicitud de acceso enviada correctamente');
      } else {
        const errorData = await response.json().catch(() => ({ error: 'Error al solicitar acceso' }));
        alert(errorData.error || 'Error al solicitar acceso');
      }
    } catch (err) {
      alert('Error al solicitar acceso');
      console.error('Error:', err);
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
      const token = getToken();
      if (!token) {
        setError('Debes iniciar sesión primero');
        setLoading(false);
        return;
      }

      const formData = new FormData();
      formData.append('archivo', uploadForm.archivo);
      formData.append('ciPaciente', uploadForm.ciPaciente);
      formData.append('tipoDocumento', uploadForm.tipoDocumento);
      if (uploadForm.descripcion) {
        formData.append('descripcion', uploadForm.descripcion);
      }
      if (uploadForm.titulo) {
        formData.append('titulo', uploadForm.titulo);
      }

      const response = await fetch(getApiUrl(`/hcen-web/api/documentos-pdf/upload`), {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
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
          descripcion: '',
          titulo: ''
        });
        // Si el CI coincide, actualizar la lista
        if (uploadForm.ciPaciente === ciPaciente) {
          buscarDocumentos();
        }
      } else {
        const errorData = await response.json().catch(() => ({ error: 'Error al subir el documento' }));
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
      setError('Por favor complete CI del paciente y contenido del documento');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const token = getToken();
      if (!token) {
        setError('Debes iniciar sesión primero');
        setLoading(false);
        return;
      }

      const body = {
        documentoIdPaciente: createForm.ciPaciente,
        contenido: createForm.contenido,
        tipoDocumento: createForm.tipoDocumento,
        titulo: createForm.titulo || createForm.tipoDocumento,
        descripcion: createForm.descripcion || '',
        especialidad: createForm.especialidad || '',
        formato: 'text/plain',
        languageCode: 'es'
      };

      const response = await fetch(getApiUrl(`/hcen-web/api/documentos/completo`), {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      });

      if (response.ok) {
        const data = await response.json();
        alert('Documento creado exitosamente');
        setShowCreateModal(false);
        setCreateForm({
          ciPaciente: '',
          titulo: '',
          contenido: '',
          tipoDocumento: 'EVALUACION',
          descripcion: '',
          especialidad: ''
        });
        // Si el CI coincide, actualizar la lista
        if (createForm.ciPaciente === ciPaciente) {
          buscarDocumentos();
        }
      } else {
        const errorData = await response.json().catch(() => ({ error: 'Error al crear el documento' }));
        setError(errorData.error || 'Error al crear el documento');
      }
    } catch (err) {
      setError('Error de conexión al crear el documento');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    try {
      // Manejar diferentes formatos de fecha
      let date;
      if (typeof dateString === 'string') {
        // Si es un objeto serializado como "{ time: timestamp }"
        if (dateString.includes('time')) {
          try {
            const parsed = JSON.parse(dateString);
            date = new Date(parsed.time || parsed);
          } catch {
            date = new Date(dateString);
          }
        } else {
          date = new Date(dateString);
        }
      } else if (typeof dateString === 'object' && dateString.time) {
        date = new Date(dateString.time);
      } else if (typeof dateString === 'object' && dateString instanceof Date) {
        date = dateString;
      } else {
        date = new Date(dateString);
      }

      if (isNaN(date.getTime())) {
        console.warn('Fecha inválida:', dateString);
        return 'Fecha inválida';
      }

      return date.toLocaleDateString('es-UY', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (err) {
      console.error('Error formateando fecha:', dateString, err);
      return 'Fecha inválida';
    }
  };

  return (
    <div style={styles.container}>
      {/* Header con búsqueda */}
      <div style={styles.headerCard}>
        <div style={styles.headerContent}>
          <div>
            <h2 style={styles.headerTitle}>Documentos Clínicos</h2>
            <p style={styles.headerSubtitle}>
              Busque y gestione documentos PDF de pacientes
            </p>
          </div>
          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
            <button
              onClick={() => {
                setShowUploadModal(true);
                setUploadForm(prev => ({ ...prev, ciPaciente: ciPaciente }));
              }}
              style={styles.uploadButton}
            >
              📤 Subir Documento PDF
            </button>
            <button
              onClick={() => {
                setShowCreateModal(true);
                setCreateForm(prev => ({ ...prev, ciPaciente: ciPaciente }));
              }}
              style={{...styles.uploadButton, backgroundColor: '#10b981'}}
            >
              ✏️ Crear Documento Manual
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
              style={{...styles.searchButton, opacity: loading ? 0.6 : 1, cursor: loading ? 'not-allowed' : 'pointer'}}
            >
              {loading ? '🔍 Buscando...' : '🔍 Buscar Documentos'}
            </button>
          </div>
        </div>

        {/* Botones principales */}
        <div style={styles.actionsSection}>
          <button
            onClick={generarResumen}
            disabled={generandoResumen || !ciPaciente.trim()}
            style={{
              ...styles.actionButton, 
              ...styles.primaryButton, 
              opacity: (generandoResumen || !ciPaciente.trim()) ? 0.6 : 1,
              cursor: (generandoResumen || !ciPaciente.trim()) ? 'not-allowed' : 'pointer'
            }}
          >
            {generandoResumen ? '⏳ Generando...' : '📋 Generar Resumen del Paciente'}
          </button>
          <button
            onClick={solicitarAccesoHistoriaClinica}
            disabled={solicitandoAcceso || !ciPaciente.trim()}
            style={{
              ...styles.actionButton, 
              ...styles.secondaryButton, 
              opacity: (solicitandoAcceso || !ciPaciente.trim()) ? 0.6 : 1,
              cursor: (solicitandoAcceso || !ciPaciente.trim()) ? 'not-allowed' : 'pointer'
            }}
          >
            {solicitandoAcceso ? '⏳ Enviando...' : '🔐 Solicitar acceso a historia clínica'}
          </button>
        </div>
      </div>

      {/* Error message */}
      {error && (
        <div style={styles.errorCard}>
          <span style={styles.errorIcon}>⚠️</span>
          <span>{error}</span>
        </div>
      )}

      {/* Resumen generado */}
      {resumen && (
        <div style={styles.resumenCard}>
          <div style={styles.resumenHeader}>
            <h3 style={styles.resumenTitle}>📋 Resumen del Paciente</h3>
            <button
              onClick={() => setResumen(null)}
              style={styles.closeButton}
            >
              ✕
            </button>
          </div>
          <div style={styles.resumenContent}>
            {typeof resumen === 'string' ? (
              <pre style={styles.resumenText}>{resumen}</pre>
            ) : (
              <pre style={styles.resumenText}>{JSON.stringify(resumen, null, 2)}</pre>
            )}
          </div>
        </div>
      )}

      {/* Lista de documentos */}
      {documentos.length > 0 && (
        <div style={styles.documentosCard}>
          <h3 style={styles.documentosTitle}>
            Documentos encontrados ({documentos.length})
          </h3>
          <div style={styles.documentosList}>
            {documentos.map((doc, index) => {
              const tieneAcceso = doc.tieneAcceso !== false; // Asumir acceso si no se especifica
              const documentoId = doc.id || doc.documentoId || doc._id;

              return (
                <div key={documentoId || index} style={styles.documentoItem}>
                  <div style={styles.documentoIcon}>📄</div>
                  <div style={styles.documentoInfo}>
                    <div style={styles.documentoHeader}>
                      <span style={styles.documentoTitulo}>{doc.titulo || doc.tipoDocumento || 'EVALUACION'}</span>
                      <span style={styles.documentoFecha}>
                        {formatDate(doc.fechaCreacion || doc.createdAt || doc.fecha)}
                      </span>
                    </div>
                    {doc.descripcion && (
                      <div style={styles.documentoDescripcion}>
                        {doc.descripcion.length > 200 ? `${doc.descripcion.substring(0, 200)}...` : doc.descripcion}
                      </div>
                    )}
                    <div style={styles.documentoMeta}>
                      <span>CI: {doc.ciPaciente || ciPaciente}</span>
                      {doc.tipoDocumento && doc.tipoDocumento !== doc.titulo && (
                        <span>• Tipo: {doc.tipoDocumento}</span>
                      )}
                    </div>
                  </div>
                  <div style={styles.documentoActions}>
                    {!tieneAcceso && (
                      <button
                        onClick={() => solicitarAccesoDocumento(documentoId, doc.ciPaciente || ciPaciente)}
                        style={styles.requestButton}
                        title="Solicitar acceso"
                      >
                        🔐 Solicitar acceso
                      </button>
                    )}
                    {tieneAcceso && (
                      <>
                        <button
                          onClick={() => descargarDocumento(documentoId)}
                          style={styles.downloadButton}
                          title="Descargar PDF"
                        >
                          ⬇️ Descargar
                        </button>
                        <button
                          onClick={() => verContenido(documentoId)}
                          style={styles.viewButton}
                          title="Ver contenido"
                        >
                          👁️ Ver contenido
                        </button>
                      </>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {documentos.length === 0 && !loading && ciPaciente && !error && !resumen && (
        <div style={styles.emptyCard}>
          <div style={styles.emptyIcon}>📭</div>
          <div style={styles.emptyText}>
            No se encontraron documentos para el CI: {ciPaciente}
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
                <label style={styles.formLabel}>Título del Documento</label>
                <input
                  type="text"
                  value={uploadForm.titulo}
                  onChange={(e) => setUploadForm(prev => ({ ...prev, titulo: e.target.value }))}
                  style={styles.formInput}
                  placeholder="Ej: Consulta cardiológica"
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
                  placeholder="Descripción del documento..."
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
                  style={{...styles.submitButton, opacity: loading ? 0.6 : 1, cursor: loading ? 'not-allowed' : 'pointer'}}
                >
                  {loading ? 'Subiendo...' : 'Subir Documento'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal de creación manual */}
      {showCreateModal && (
        <div style={styles.modalOverlay} onClick={() => setShowCreateModal(false)}>
          <div style={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <div style={styles.modalHeader}>
              <h3 style={styles.modalTitle}>Crear Documento Manual</h3>
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
                <label style={styles.formLabel}>Título del Documento</label>
                <input
                  type="text"
                  value={createForm.titulo}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, titulo: e.target.value }))}
                  style={styles.formInput}
                  placeholder="Ej: Consulta cardiológica"
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
                <label style={styles.formLabel}>Especialidad (opcional)</label>
                <input
                  type="text"
                  value={createForm.especialidad}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, especialidad: e.target.value }))}
                  style={styles.formInput}
                  placeholder="Ej: Cardiología"
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Descripción (opcional)</label>
                <textarea
                  value={createForm.descripcion}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, descripcion: e.target.value }))}
                  style={styles.formTextarea}
                  rows="3"
                  placeholder="Descripción del documento..."
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Contenido del Documento *</label>
                <textarea
                  value={createForm.contenido}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, contenido: e.target.value }))}
                  style={styles.formTextarea}
                  rows="10"
                  placeholder="Ingrese el contenido del documento clínico..."
                  required
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
                  style={{...styles.submitButton, opacity: loading ? 0.6 : 1, cursor: loading ? 'not-allowed' : 'pointer'}}
                >
                  {loading ? 'Creando...' : 'Crear Documento'}
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
  container: {
    width: '100%',
    maxWidth: '1400px',
    margin: '0 auto',
    padding: 'clamp(16px, 2vw, 32px)',
  },
  headerCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: 'clamp(20px, 3vw, 24px)',
    marginBottom: 'clamp(16px, 2vw, 24px)',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  headerContent: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 'clamp(16px, 2vw, 20px)',
    flexWrap: 'wrap',
    gap: 'clamp(12px, 2vw, 16px)'
  },
  headerTitle: {
    margin: '0 0 4px 0',
    fontSize: 'clamp(20px, 3vw, 24px)',
    fontWeight: '700',
    color: '#111827'
  },
  headerSubtitle: {
    margin: 0,
    fontSize: 'clamp(12px, 2vw, 14px)',
    color: '#6b7280'
  },
  uploadButton: {
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: 'clamp(10px, 2vw, 12px) clamp(20px, 3vw, 24px)',
    fontSize: 'clamp(13px, 2vw, 15px)',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    minWidth: 'fit-content'
  },
  searchSection: {
    marginTop: 'clamp(16px, 2vw, 20px)'
  },
  searchInputGroup: {
    display: 'flex',
    gap: 'clamp(8px, 1.5vw, 12px)',
    flexWrap: 'wrap'
  },
  searchInput: {
    flex: '1 1 300px',
    minWidth: '200px',
    padding: 'clamp(10px, 2vw, 12px) clamp(14px, 2vw, 16px)',
    border: '2px solid #e5e7eb',
    borderRadius: '8px',
    fontSize: 'clamp(14px, 2vw, 15px)',
    outline: 'none',
    transition: 'border-color 0.2s'
  },
  searchButton: {
    padding: 'clamp(10px, 2vw, 12px) clamp(20px, 3vw, 24px)',
    backgroundColor: '#10b981',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: 'clamp(13px, 2vw, 15px)',
    fontWeight: '600',
    minWidth: 'fit-content',
    whiteSpace: 'nowrap'
  },
  actionsSection: {
    display: 'flex',
    gap: 'clamp(8px, 1.5vw, 12px)',
    marginTop: 'clamp(16px, 2vw, 20px)',
    flexWrap: 'wrap'
  },
  actionButton: {
    padding: 'clamp(10px, 2vw, 12px) clamp(16px, 3vw, 24px)',
    border: 'none',
    borderRadius: '8px',
    fontSize: 'clamp(13px, 2vw, 15px)',
    fontWeight: '600',
    minWidth: 'fit-content',
    whiteSpace: 'nowrap',
    transition: 'all 0.2s'
  },
  primaryButton: {
    backgroundColor: '#8b5cf6',
    color: 'white'
  },
  secondaryButton: {
    backgroundColor: '#f59e0b',
    color: 'white'
  },
  errorCard: {
    backgroundColor: '#fef2f2',
    border: '1px solid #fecaca',
    borderRadius: '8px',
    padding: 'clamp(12px, 2vw, 16px)',
    marginBottom: 'clamp(16px, 2vw, 24px)',
    display: 'flex',
    alignItems: 'center',
    gap: 'clamp(8px, 1.5vw, 12px)',
    color: '#991b1b'
  },
  errorIcon: {
    fontSize: 'clamp(18px, 2.5vw, 20px)'
  },
  resumenCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: 'clamp(20px, 3vw, 24px)',
    marginBottom: 'clamp(16px, 2vw, 24px)',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  resumenHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 'clamp(12px, 2vw, 16px)'
  },
  resumenTitle: {
    margin: 0,
    fontSize: 'clamp(18px, 3vw, 20px)',
    fontWeight: '700',
    color: '#111827'
  },
  closeButton: {
    backgroundColor: 'transparent',
    border: 'none',
    fontSize: 'clamp(20px, 3vw, 24px)',
    cursor: 'pointer',
    color: '#6b7280'
  },
  resumenContent: {
    backgroundColor: '#f9fafb',
    borderRadius: '8px',
    padding: 'clamp(12px, 2vw, 16px)',
    maxHeight: '400px',
    overflow: 'auto'
  },
  resumenText: {
    margin: 0,
    fontSize: 'clamp(13px, 2vw, 14px)',
    lineHeight: '1.6',
    color: '#374151',
    whiteSpace: 'pre-wrap',
    wordWrap: 'break-word',
    fontFamily: 'inherit'
  },
  documentosCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: 'clamp(20px, 3vw, 24px)',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  documentosTitle: {
    margin: '0 0 clamp(16px, 2vw, 20px) 0',
    fontSize: 'clamp(18px, 3vw, 20px)',
    fontWeight: '700',
    color: '#111827'
  },
  documentosList: {
    display: 'flex',
    flexDirection: 'column !important',
    gap: 'clamp(10px, 1.5vw, 12px)'
  },
  documentoItem: {
    display: 'flex',
    alignItems: 'center',
    gap: 'clamp(12px, 2vw, 16px)',
    padding: 'clamp(12px, 2vw, 16px)',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    transition: 'all 0.2s',
    width: '100% !important',
    flexWrap: 'wrap'
  },
  documentoIcon: {
    fontSize: 'clamp(28px, 4vw, 32px)',
    flexShrink: 0
  },
  documentoInfo: {
    flex: '1 1 300px',
    minWidth: '200px'
  },
  documentoHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 'clamp(6px, 1vw, 8px)',
    flexWrap: 'wrap',
    gap: 'clamp(4px, 1vw, 8px)'
  },
  documentoTitulo: {
    fontSize: 'clamp(14px, 2vw, 16px)',
    fontWeight: '600',
    color: '#111827'
  },
  documentoFecha: {
    fontSize: 'clamp(11px, 1.5vw, 13px)',
    color: '#6b7280'
  },
  documentoDescripcion: {
    fontSize: 'clamp(12px, 2vw, 14px)',
    color: '#374151',
    marginBottom: 'clamp(6px, 1vw, 8px)'
  },
  documentoMeta: {
    fontSize: 'clamp(11px, 1.5vw, 13px)',
    color: '#9ca3af',
    display: 'flex',
    gap: 'clamp(8px, 1.5vw, 12px)',
    flexWrap: 'wrap'
  },
  documentoActions: {
    display: 'flex',
    gap: 'clamp(6px, 1vw, 8px)',
    flexWrap: 'wrap',
    flexShrink: 0
  },
  downloadButton: {
    backgroundColor: '#10b981',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: 'clamp(8px, 1.5vw, 10px) clamp(16px, 2.5vw, 20px)',
    fontSize: 'clamp(12px, 1.8vw, 14px)',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: 'clamp(4px, 1vw, 8px)',
    minWidth: 'fit-content'
  },
  viewButton: {
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: 'clamp(8px, 1.5vw, 10px) clamp(16px, 2.5vw, 20px)',
    fontSize: 'clamp(12px, 1.8vw, 14px)',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: 'clamp(4px, 1vw, 8px)',
    minWidth: 'fit-content'
  },
  requestButton: {
    backgroundColor: '#f59e0b',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: 'clamp(8px, 1.5vw, 10px) clamp(16px, 2.5vw, 20px)',
    fontSize: 'clamp(12px, 1.8vw, 14px)',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: 'clamp(4px, 1vw, 8px)',
    minWidth: 'fit-content'
  },
  emptyCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: 'clamp(40px, 6vw, 48px)',
    textAlign: 'center',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  emptyIcon: {
    fontSize: 'clamp(48px, 8vw, 64px)',
    marginBottom: 'clamp(12px, 2vw, 16px)'
  },
  emptyText: {
    fontSize: 'clamp(14px, 2vw, 16px)',
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
    zIndex: 1000,
    padding: 'clamp(16px, 3vw, 20px)'
  },
  modalContent: {
    backgroundColor: 'white',
    borderRadius: '12px',
    width: '100%',
    maxWidth: '500px',
    maxHeight: '90vh',
    overflow: 'auto'
  },
  modalHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 'clamp(20px, 3vw, 24px)',
    borderBottom: '1px solid #e5e7eb'
  },
  modalTitle: {
    margin: 0,
    fontSize: 'clamp(18px, 3vw, 20px)',
    fontWeight: '700',
    color: '#111827'
  },
  modalClose: {
    backgroundColor: 'transparent',
    border: 'none',
    fontSize: 'clamp(20px, 3vw, 24px)',
    cursor: 'pointer',
    color: '#6b7280'
  },
  modalForm: {
    padding: 'clamp(20px, 3vw, 24px)'
  },
  formGroup: {
    marginBottom: 'clamp(16px, 2vw, 20px)'
  },
  formLabel: {
    display: 'block',
    marginBottom: 'clamp(6px, 1vw, 8px)',
    fontSize: 'clamp(12px, 2vw, 14px)',
    fontWeight: '600',
    color: '#374151'
  },
  formInput: {
    width: '100%',
    padding: 'clamp(10px, 2vw, 12px)',
    border: '2px solid #e5e7eb',
    borderRadius: '8px',
    fontSize: 'clamp(13px, 2vw, 15px)',
    outline: 'none',
    boxSizing: 'border-box'
  },
  formTextarea: {
    width: '100%',
    padding: 'clamp(10px, 2vw, 12px)',
    border: '2px solid #e5e7eb',
    borderRadius: '8px',
    fontSize: 'clamp(13px, 2vw, 15px)',
    outline: 'none',
    resize: 'vertical',
    fontFamily: 'inherit',
    boxSizing: 'border-box'
  },
  modalActions: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: 'clamp(8px, 1.5vw, 12px)',
    marginTop: 'clamp(20px, 3vw, 24px)'
  },
  cancelButton: {
    padding: 'clamp(10px, 2vw, 12px) clamp(20px, 3vw, 24px)',
    backgroundColor: '#f3f4f6',
    color: '#374151',
    border: 'none',
    borderRadius: '8px',
    fontSize: 'clamp(13px, 2vw, 15px)',
    fontWeight: '600',
    cursor: 'pointer'
  },
  submitButton: {
    padding: 'clamp(10px, 2vw, 12px) clamp(20px, 3vw, 24px)',
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: 'clamp(13px, 2vw, 15px)',
    fontWeight: '600'
  }
};

export default DocumentosPage;
