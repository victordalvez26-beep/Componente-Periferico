import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import './DocumentosPage.css';

function DocumentosPage() {
  const { tenantId } = useParams();
  const navigate = useNavigate();
  const [ciPaciente, setCiPaciente] = useState('');
  const [documentos, setDocumentos] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Limpiar datos al montar el componente
  useEffect(() => {
    setDocumentos([]);
    setError(null);
    setCreateError(null);
    setCiPaciente('');
    setResumen(null);
    setShowResumenModal(false);
    setErrorResumen(null);
  }, [tenantId]);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [showSolicitarAccesoModal, setShowSolicitarAccesoModal] = useState(false);
  const [solicitarAccesoForm, setSolicitarAccesoForm] = useState({
    ciPaciente: '',
    motivo: ''
  });
  const [uploadForm, setUploadForm] = useState({
    archivo: null,
    ciPaciente: '',
    tipoDocumento: 'CONSULTA_MEDICA',
    descripcion: ''
  });
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createForm, setCreateForm] = useState({
    ciPaciente: '',
    contenido: '',
    tipoDocumento: 'CONSULTA_MEDICA',
    descripcion: '',
    titulo: '',
    autor: ''
  });
  const [createError, setCreateError] = useState(null);
  const [showResumenModal, setShowResumenModal] = useState(false);
  const [resumen, setResumen] = useState(null);
  const [loadingResumen, setLoadingResumen] = useState(false);
  const [errorResumen, setErrorResumen] = useState(null);

  // Función helper para detectar errores de MongoDB/base de datos
  const isDatabaseError = (errorMsg) => {
    if (!errorMsg) return false;
    const msg = errorMsg.toLowerCase();
    return msg.includes('mongo') || 
           msg.includes('database') || 
           msg.includes('connection') ||
           msg.includes('timeout') ||
           msg.includes('network') ||
           msg.includes('unable to connect') ||
           msg.includes('connection refused');
  };

  const handleDatabaseError = (errorMsg, defaultMsg = 'Error de conexión') => {
    if (isDatabaseError(errorMsg)) {
      return 'Error al conectarse con la base de datos. Contacte a su administrador.';
    }
    return errorMsg || defaultMsg;
  };

  const buscarDocumentos = async () => {
    if (!ciPaciente.trim()) {
      setError('Por favor ingrese un CI');
      return;
    }

    setLoading(true);
    setError(null);
    setCreateError(null);

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
        const errorData = await response.json().catch(() => ({}));
        const errorMsg = errorData.error || 'Error al buscar documentos';
        setError(handleDatabaseError(errorMsg, 'Error al buscar documentos'));
      }
    } catch (err) {
      const errMsg = err.message || String(err);
      setError(handleDatabaseError(errMsg, 'Error de conexión. Verifique que el servidor esté disponible.'));
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const descargarDocumento = async (documento) => {
    try {
      // El documento puede tener uriDocumento o urlAcceso que apunta directamente al componente periférico
      // o podemos usar el ID para descargar desde HCEN Central que hace proxy
      let downloadUrl;
      
      // Priorizar uriDocumento o urlAcceso (URI directa al componente periférico)
      if (documento.uriDocumento) {
        downloadUrl = documento.uriDocumento;
      } else if (documento.urlAcceso) {
        downloadUrl = documento.urlAcceso;
      } else if (documento.id) {
        // Si no tiene URI, usar el endpoint de HCEN Central que hace proxy
        // IMPORTANTE: Incluir el prefijo /hcen en la URL
        const backendUrl = process.env.REACT_APP_HCEN_BACKEND_URL || 'http://localhost:8080';
        downloadUrl = `${backendUrl}/hcen/api/metadatos-documento/${documento.id}/descargar`;
      } else {
        alert('No se puede descargar: el documento no tiene información de descarga disponible.');
        return;
      }

      const token = localStorage.getItem('token');
      const headers = {
        'Authorization': `Bearer ${token}`,
      };

      console.log('Descargando documento desde:', downloadUrl);

      const response = await fetch(downloadUrl, {
        method: 'GET',
        headers: headers,
        credentials: 'include', // Incluir cookies para autenticación
      });

      if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const fileName = documento.tipoDocumento 
          ? `documento-${documento.tipoDocumento}-${documento.id || 'descarga'}.pdf`
          : `documento-${documento.id || 'descarga'}.pdf`;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      } else if (response.status === 403) {
        const errorText = await response.text();
        alert(`No tiene permiso para acceder a este documento. Use el botón "Solicitar Acceso" para solicitar acceso.`);
      } else if (response.status === 404) {
        alert(`No se encontró el documento (404). Puede que el documento ya no esté disponible o que la URL sea incorrecta. URL intentada: ${downloadUrl}`);
      } else {
        const errorText = await response.text().catch(() => 'Error desconocido');
        const errorMsg = handleDatabaseError(errorText, 'Error al descargar el documento');
        alert(`Error al descargar el documento (${response.status}): ${errorMsg}`);
      }
    } catch (err) {
      const errMsg = err.message || String(err);
      console.error('Error al descargar:', err);
      if (errMsg.includes('Failed to fetch') || errMsg.includes('CORS')) {
        alert(`Error de conexión: No se pudo conectar al servidor para descargar el documento. Verifique su conexión y que el servidor esté disponible. Error: ${errMsg}`);
      } else {
        alert(`Error al descargar el documento: ${handleDatabaseError(errMsg)}`);
      }
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
        const errorText = await response.text().catch(() => 'Error desconocido');
        let errorMsg = 'Error desconocido';
        try {
          const errorData = JSON.parse(errorText);
          errorMsg = errorData.error || errorData.detalle || errorText;
        } catch {
          errorMsg = errorText.includes('404') || errorText.includes('Not Found') 
            ? 'El servicio de solicitud de acceso no está disponible en este momento. Contacte al administrador.'
            : errorText;
        }
        setError(`Error al enviar solicitud: ${handleDatabaseError(errorMsg)}`);
      }
    } catch (err) {
      const errMsg = err.message || String(err);
      setError(`Error al enviar solicitud de acceso: ${handleDatabaseError(errMsg)}`);
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
        const errorData = await response.json().catch(() => ({}));
        const errorMsg = errorData.error || 'Error al subir el documento';
        setError(handleDatabaseError(errorMsg, 'Error al subir el documento'));
      }
    } catch (err) {
      const errMsg = err.message || String(err);
      setError(handleDatabaseError(errMsg, 'Error de conexión al subir el documento'));
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
        setCreateError(null);
        setCreateForm({
          ciPaciente: '',
          contenido: '',
          tipoDocumento: 'CONSULTA_MEDICA',
          descripcion: '',
          titulo: '',
          autor: ''
        });
        // Si el CI coincide, actualizar la lista
        if (createForm.ciPaciente === ciPaciente) {
          buscarDocumentos();
        }
      } else {
        const errorData = await response.json().catch(() => ({}));
        const msg = errorData.error || 'Error al crear el documento';
        setCreateError(handleDatabaseError(msg, 'Error al crear el documento'));
        setError(null);
      }
    } catch (err) {
      const errMsg = err.message || String(err);
      setError(handleDatabaseError(errMsg, 'Error de conexión al crear el documento'));
      setCreateError(null);
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const generarResumen = async () => {
    if (!ciPaciente.trim()) {
      setErrorResumen('Por favor ingrese un CI del paciente');
      return;
    }

    setLoadingResumen(true);
    setErrorResumen(null);
    setResumen(null);

    try {
      const token = localStorage.getItem('token');
      const backendBase = process.env.REACT_APP_BACKEND_URL || '';
      const response = await fetch(`${backendBase}/hcen-web/api/documentos/${ciPaciente}/resumen`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        setResumen(data);
        setShowResumenModal(true);
        setErrorResumen(null);
      } else if (response.status === 404) {
        setErrorResumen('No se encontraron documentos para este paciente');
      } else if (response.status === 403) {
        const errorData = await response.json().catch(() => ({}));
        setErrorResumen(errorData.error || 'No tiene permisos para acceder a la historia clínica de este paciente');
      } else {
        const errorData = await response.json().catch(() => ({}));
        const errorMsg = errorData.error || 'Error al generar el resumen';
        setErrorResumen(handleDatabaseError(errorMsg, 'Error al generar el resumen'));
      }
    } catch (err) {
      const errMsg = err.message || String(err);
      setErrorResumen(handleDatabaseError(errMsg, 'Error de conexión al generar el resumen'));
      console.error('Error:', err);
    } finally {
      setLoadingResumen(false);
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

  const renderCreateError = () => {
    if (!createError) {
      return null;
    }

    const normalizedError = createError.toLowerCase();
    const shouldSuggestCreatePatient =
      normalizedError.includes('paciente/usuario no encontrado') ||
      normalizedError.includes('no encontrado') ||
      normalizedError.includes('debe crear') ||
      normalizedError.includes('registre al paciente') ||
      (normalizedError.includes('paciente') && normalizedError.includes('crear'));
    
    // Verificar si el error es por cédula incompleta
    const isCedulaIncompleta = normalizedError.includes('cedula') || 
                                 normalizedError.includes('cédula') ||
                                 normalizedError.includes('ci') && normalizedError.includes('incompleto');

    return (
      <div style={styles.modalErrorCard}>
        <div style={styles.modalErrorHeader}>
          <span style={styles.modalErrorIcon}>!</span>
          <div>
            <div style={styles.modalErrorTitle}>No se pudo crear el documento</div>
            <div style={styles.modalErrorText}>
              {createError.toLowerCase().includes('no encontrado') && createError.toLowerCase().includes('paciente')
                ? 'El paciente no está registrado en esta clínica. Por favor, verifique que la cédula esté completa y registre al paciente antes de crear documentos.'
                : createError}
            </div>
          </div>
        </div>

        {shouldSuggestCreatePatient && (
          <div style={styles.modalErrorActions}>
            <button
              type="button"
              style={styles.goCreatePatientButton}
              onClick={() => navigate(`/portal/clinica/${tenantId}/usuarios`)}
            >
              Crear paciente
            </button>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="documentos-page-container">
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
                setShowCreateModal(true);
                setCreateForm(prev => ({ ...prev, ciPaciente: ciPaciente }));
                setCreateError(null);
              }}
              style={styles.createButton}
            >
              Crear Documento
            </button>
            <button
              onClick={() => {
                setShowUploadModal(true);
                setUploadForm(prev => ({ ...prev, ciPaciente: ciPaciente }));
              }}
              style={styles.uploadButton}
            >
              Subir PDF
            </button>
            <button
              onClick={() => setShowSolicitarAccesoModal(true)}
              style={styles.solicitarAccesoButton}
            >
              Solicitar Acceso
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
              {loading ? 'Buscando...' : 'Buscar'}
            </button>
            <button
              onClick={generarResumen}
              disabled={loadingResumen || !ciPaciente.trim()}
              style={styles.resumenButton}
              title="Generar resumen de la historia clínica"
            >
              {loadingResumen ? 'Generando...' : 'Resumen'}
            </button>
          </div>
          {errorResumen && (
            <div style={styles.permisoErrorCard}>
              <div style={styles.permisoErrorHeader}>
                <span style={styles.permisoErrorIcon}>⚠️</span>
                <h4 style={styles.permisoErrorTitle}>Acceso Denegado</h4>
              </div>
              <p style={styles.permisoErrorText}>{errorResumen}</p>
              {errorResumen.includes('permisos') && ciPaciente && (
                <div style={styles.permisoErrorActions}>
                  <button
                    onClick={() => {
                      setShowSolicitarAccesoModal(true);
                      setSolicitarAccesoForm(prev => ({ ...prev, ciPaciente: ciPaciente }));
                      setErrorResumen(null);
                    }}
                    style={styles.permisoSolicitarButton}
                  >
                    📝 Solicitar Acceso
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Error message */}
      {error && (
        <div style={styles.errorCard}>
          <span style={styles.errorIcon}>!</span>
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
                    <span style={styles.documentoTipo}>{doc.tipoDocumento || 'CONSULTA_MEDICA'}</span>
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
                  onClick={() => descargarDocumento(doc)}
                  style={styles.downloadButton}
                  title="Descargar PDF"
                  disabled={!doc.id && !doc.uriDocumento}
                >
                  Descargar
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
        <div
          style={styles.modalOverlay}
          onClick={() => {
            setShowCreateModal(false);
            setCreateError(null);
            setCreateForm({
              ciPaciente: '',
              contenido: '',
              tipoDocumento: 'CONSULTA_MEDICA',
              descripcion: '',
              titulo: '',
              autor: ''
            });
          }}
        >
          <div style={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <div style={styles.modalHeader}>
              <h3 style={styles.modalTitle}>Crear Documento Clínico</h3>
              <button
                onClick={() => {
                  setShowCreateModal(false);
                  setCreateError(null);
                }}
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
                  <option value="CONSULTA_MEDICA">Consulta Médica</option>
                  <option value="RECETA_MEDICA">Receta Médica</option>
                  <option value="INFORME_LABORATORIO">Informe de Laboratorio</option>
                  <option value="RADIOGRAFIA">Radiografía</option>
                  <option value="RESUMEN_ALTA">Resumen de Alta</option>
                  <option value="CIRUGIA">Informe Quirúrgico</option>
                  <option value="ESTUDIO_IMAGENOLOGIA">Estudio de Imagenología</option>
                  <option value="ELECTROCARDIOGRAMA">Electrocardiograma</option>
                  <option value="INFORME_PATOLOGIA">Informe de Patología</option>
                  <option value="VACUNACION">Vacunación</option>
                  <option value="OTROS">Otros</option>
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

              {renderCreateError()}

              <div style={styles.modalActions}>
                <button
                  type="button"
                  onClick={() => {
                    setShowCreateModal(false);
                    setCreateError(null);
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
                  {loading ? 'Creando...' : 'Crear Documento'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal de subida */}
      {showUploadModal && (
        <div style={styles.modalOverlay} onClick={() => {
          setShowUploadModal(false);
          setUploadForm({
            archivo: null,
            ciPaciente: '',
            tipoDocumento: 'CONSULTA_MEDICA',
            descripcion: ''
          });
        }}>
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
                  <option value="CONSULTA_MEDICA">Consulta Médica</option>
                  <option value="RECETA_MEDICA">Receta Médica</option>
                  <option value="INFORME_LABORATORIO">Informe de Laboratorio</option>
                  <option value="RADIOGRAFIA">Radiografía</option>
                  <option value="RESUMEN_ALTA">Resumen de Alta</option>
                  <option value="CIRUGIA">Informe Quirúrgico</option>
                  <option value="ESTUDIO_IMAGENOLOGIA">Estudio de Imagenología</option>
                  <option value="ELECTROCARDIOGRAMA">Electrocardiograma</option>
                  <option value="INFORME_PATOLOGIA">Informe de Patología</option>
                  <option value="VACUNACION">Vacunación</option>
                  <option value="OTROS">Otros</option>
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

      {/* Modal para mostrar resumen */}
      {showResumenModal && resumen && (
        <div
          style={styles.modalOverlay}
          onClick={() => {
            setShowResumenModal(false);
            setErrorResumen(null);
          }}
        >
          <div style={styles.resumenModalContent} onClick={(e) => e.stopPropagation()}>
            <div style={styles.modalHeader}>
              <h3 style={styles.modalTitle}>
                Resumen de Historia Clínica
              </h3>
              <button
                onClick={() => {
                  setShowResumenModal(false);
                  setErrorResumen(null);
                }}
                style={styles.modalClose}
              >
                ✕
              </button>
            </div>
            <div style={styles.resumenModalBody}>
              <div style={styles.resumenInfo}>
                <div style={styles.resumenInfoItem}>
                  <span style={styles.resumenInfoLabel}>Paciente:</span>
                  <span style={styles.resumenInfoValue}>{resumen.paciente}</span>
                </div>
                <div style={styles.resumenInfoItem}>
                  <span style={styles.resumenInfoLabel}>Documentos procesados:</span>
                  <span style={styles.resumenInfoValue}>{resumen.documentosProcesados || 0}</span>
                </div>
              </div>
              <div style={styles.resumenContent}>
                <div style={styles.resumenHeader}>
                  <h4 style={styles.resumenTitle}>Resumen Generado</h4>
                </div>
                <div style={styles.resumenText}>
                  {resumen.resumen?.split('\n').map((line, index) => {
                    // Si la línea está vacía o solo tiene espacios, mostrar un párrafo vacío
                    if (!line.trim()) {
                      return <br key={index} />;
                    }
                    // Si la línea parece un encabezado (empieza con ===), darle estilo especial
                    if (line.trim().startsWith('===')) {
                      return (
                        <h5 key={index} style={styles.resumenHeading}>
                          {line.replace(/=/g, '').trim()}
                        </h5>
                      );
                    }
                    return (
                      <p key={index} style={styles.resumenParagraph}>
                        {line}
                      </p>
                    );
                  })}
                </div>
              </div>
            </div>
            <div style={styles.modalActions}>
              <button
                onClick={() => {
                  setShowResumenModal(false);
                  setErrorResumen(null);
                }}
                style={styles.submitButton}
              >
                Cerrar
              </button>
            </div>
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
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
    width: '100%',
    boxSizing: 'border-box'
  },
  headerContent: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '20px',
    flexWrap: 'wrap',
    gap: '16px'
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
    gap: '8px',
    transition: 'all 0.2s'
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
    backgroundColor: '#3b82f6', // Azul
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
    gap: '12px',
    flexWrap: 'wrap'
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
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'background-color 0.2s'
  },
  resumenButton: {
    padding: '12px 24px',
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    transition: 'all 0.2s',
    boxShadow: '0 2px 4px rgba(59, 130, 246, 0.3)'
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
    transition: 'all 0.2s',
    flexWrap: 'wrap'
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
    marginBottom: '8px',
    flexWrap: 'wrap',
    gap: '8px'
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
    gap: '12px',
    flexWrap: 'wrap'
  },
  downloadButton: {
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: '10px 20px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s',
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
    overflow: 'auto',
    margin: '16px',
    boxSizing: 'border-box'
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
    marginTop: '24px',
    flexWrap: 'wrap'
  },
  cancelButton: {
    padding: '12px 24px',
    backgroundColor: '#ffffff',
    color: '#3b82f6',
    border: '2px solid #3b82f6',
    borderRadius: '8px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  submitButton: {
    padding: '12px 24px',
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  modalErrorCard: {
    marginTop: '12px',
    padding: '16px',
    borderRadius: '8px',
    backgroundColor: '#fef2f2',
    border: '1px solid #fecaca',
    color: '#991b1b'
  },
  modalErrorHeader: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '12px'
  },
  modalErrorIcon: {
    fontSize: '20px',
    marginTop: '2px'
  },
  modalErrorTitle: {
    fontWeight: '600',
    marginBottom: '4px'
  },
  modalErrorText: {
    fontSize: '14px'
  },
  modalErrorActions: {
    marginTop: '12px',
    display: 'flex',
    justifyContent: 'flex-end'
  },
  goCreatePatientButton: {
    padding: '10px 18px',
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '999px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  resumenModalContent: {
    backgroundColor: 'white',
    borderRadius: '12px',
    width: '90%',
    maxWidth: '800px',
    maxHeight: '90vh',
    overflow: 'auto',
    boxShadow: '0 10px 40px rgba(0,0,0,0.2)',
    margin: '16px',
    boxSizing: 'border-box'
  },
  resumenModalBody: {
    padding: '24px'
  },
  resumenInfo: {
    display: 'flex',
    gap: '24px',
    marginBottom: '24px',
    padding: '16px',
    backgroundColor: '#f9fafb',
    borderRadius: '8px',
    border: '1px solid #e5e7eb',
    flexWrap: 'wrap'
  },
  resumenInfoItem: {
    display: 'flex',
    flexDirection: 'column',
    gap: '4px'
  },
  resumenInfoLabel: {
    fontSize: '13px',
    color: '#6b7280',
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  resumenInfoValue: {
    fontSize: '16px',
    color: '#111827',
    fontWeight: '600'
  },
  resumenContent: {
    backgroundColor: '#ffffff',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    padding: '24px'
  },
  resumenHeader: {
    marginBottom: '16px',
    paddingBottom: '12px',
    borderBottom: '2px solid #e5e7eb'
  },
  resumenTitle: {
    margin: 0,
    fontSize: '18px',
    fontWeight: '700',
    color: '#111827'
  },
  resumenText: {
    fontSize: '15px',
    lineHeight: '1.8',
    color: '#374151'
  },
  resumenParagraph: {
    margin: '0 0 12px 0',
    whiteSpace: 'pre-wrap',
    wordWrap: 'break-word',
    textAlign: 'justify'
  },
  resumenHeading: {
    margin: '16px 0 8px 0',
    fontSize: '16px',
    fontWeight: '700',
    color: '#8b5cf6',
    borderBottom: '2px solid #e5e7eb',
    paddingBottom: '8px'
  },
  permisoErrorCard: {
    backgroundColor: '#fef3c7',
    border: '2px solid #f59e0b',
    borderRadius: '12px',
    padding: '20px',
    marginTop: '16px',
    boxShadow: '0 2px 8px rgba(245, 158, 11, 0.2)'
  },
  permisoErrorHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    marginBottom: '12px'
  },
  permisoErrorIcon: {
    fontSize: '24px'
  },
  permisoErrorTitle: {
    margin: 0,
    fontSize: '18px',
    fontWeight: '700',
    color: '#92400e'
  },
  permisoErrorText: {
    margin: '0 0 16px 0',
    fontSize: '15px',
    color: '#78350f',
    lineHeight: '1.6'
  },
  permisoErrorActions: {
    display: 'flex',
    gap: '12px'
  },
  permisoSolicitarButton: {
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: '10px 20px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'background-color 0.2s',
    display: 'flex',
    alignItems: 'center',
    gap: '8px'
  }
};

export default DocumentosPage;

