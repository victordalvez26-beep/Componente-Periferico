import React, { useState, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { getApiUrl } from '../utils/api';

// Estilos CSS inline para forzar layout en filas
const inlineStyles = `
  .documentos-list {
    display: flex !important;
    flex-direction: column !important;
    width: 100% !important;
  }
  .documento-item {
    display: flex !important;
    flex-direction: row !important;
    width: 100% !important;
    max-width: 100% !important;
  }
`;

function DocumentosPage() {
  const { tenantId } = useParams();
  const [ciPaciente, setCiPaciente] = useState('');
  const [documentos, setDocumentos] = useState([]);
  const [documentoSeleccionado, setDocumentoSeleccionado] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  
  // Estados para crear documento completo
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [modoCreacion, setModoCreacion] = useState('sin-archivo');
  const [createForm, setCreateForm] = useState({
    documentoIdPaciente: '',
    contenido: 'Paciente presenta sintomas de gripe. Diagnostico: Gripe estacional. Tratamiento: reposo, hidratacion y medicamentos sintomaticos.',
    titulo: 'Consulta Medica - Gripe',
    especialidad: 'MEDICINA_GENERAL',
    descripcion: '',
    archivo: null
  });
  const fileInputRef = useRef(null);
  const [archivoInputKey, setArchivoInputKey] = useState(0);

  // Estados para resumen
  const [resumen, setResumen] = useState(null);
  const [showResumenModal, setShowResumenModal] = useState(false);

  // Estados para solicitar acceso
  const [showSolicitarAccesoModal, setShowSolicitarAccesoModal] = useState(false);
  const [solicitudForm, setSolicitudForm] = useState({
    codDocumPaciente: '',
    tipoDocumento: '',
    documentoId: '',
    razonSolicitud: 'Solicitud de acceso desde portal',
    historiaCompleta: false
  });

  // Estados para visor de contenido
  const [contenidoVista, setContenidoVista] = useState(null);
  const [mostrarVisor, setMostrarVisor] = useState(false);

  // Estados para permisos de documentos
  const [permisosDocumentos, setPermisosDocumentos] = useState({});

  const getToken = () => localStorage.getItem('token');
  const getApiBase = () => getApiUrl('/hcen-web/api');

  // Buscar documentos PDF del periférico
  const buscarDocumentos = async () => {
    if (!ciPaciente.trim()) {
      setError('Por favor ingrese un CI');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const token = getToken();
      const response = await fetch(`${getApiBase()}/documentos-pdf/paciente/${ciPaciente}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        const docsArray = Array.isArray(data) ? data : [];
        setDocumentos(docsArray);
        setSuccess(
          docsArray.length === 0
            ? 'No se encontraron documentos PDF para este paciente'
            : `Se encontraron ${docsArray.length} documentos PDF`
        );
        
        // Verificar permisos para cada documento
        if (docsArray.length > 0) {
          verificarPermisosDocumentos(docsArray);
        }
      } else if (response.status === 404) {
        setDocumentos([]);
        setSuccess('No se encontraron documentos PDF para este paciente');
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

  // Verificar permisos de acceso a documentos
  const verificarPermisosDocumentos = async (docs) => {
    const permisos = {};
    const token = getToken();
    
    for (const doc of docs) {
      try {
        // Intentar acceder al documento para verificar permisos
        const response = await fetch(`${getApiBase()}/documentos-pdf/${doc.id}`, {
          method: 'HEAD',
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        permisos[doc.id] = response.ok;
      } catch (e) {
        permisos[doc.id] = false;
      }
    }
    
    setPermisosDocumentos(permisos);
  };

  // Crear documento completo sin archivo
  const handleCrearDocumentoSinArchivo = async () => {
    const token = getToken();
    if (!token) {
      setError('Debes iniciar sesión primero. Por favor, inicia sesión nuevamente.');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const username = localStorage.getItem('username') || 'admin_c1';

      const body = {
        documentoIdPaciente: createForm.documentoIdPaciente,
        contenido: createForm.contenido,
        titulo: createForm.titulo,
        especialidad: createForm.especialidad,
        formato: 'text/plain',
        autor: username,
        fechaCreacion: new Date().toISOString(),
        breakingTheGlass: false
      };

      if (createForm.descripcion) {
        body.descripcion = createForm.descripcion;
      }

      const response = await fetch(`${getApiBase()}/documentos/completo`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          'Accept': 'application/json'
        },
        body: JSON.stringify(body)
      });

      const text = await response.text();
      let data;
      try {
        data = JSON.parse(text);
      } catch (e) {
        setError(`Error ${response.status}: ${text || response.statusText}`);
        setLoading(false);
        return;
      }

      if (response.ok) {
        setSuccess('Documento creado exitosamente' + (data.tienePdf ? ' (PDF generado)' : ''));
        setShowCreateModal(false);
        // Recargar documentos si el CI coincide
        if (createForm.documentoIdPaciente === ciPaciente && ciPaciente) {
          setTimeout(() => buscarDocumentos(), 500);
        }
      } else if (response.status === 401) {
        setError('Sesión expirada. Por favor, inicia sesión nuevamente.');
        localStorage.clear();
        window.location.href = `/portal/clinica/${tenantId}/login`;
      } else {
        setError(data.error || `Error al crear documento (${response.status})`);
      }
    } catch (error) {
      setError(error.message || 'Error al crear documento');
    } finally {
      setLoading(false);
    }
  };

  // Crear documento completo con archivo adjunto
  const handleCrearDocumentoConArchivo = async () => {
    if (!getToken()) {
      setError('Debes iniciar sesión primero');
      return;
    }

    if (!createForm.archivo) {
      setError('Debes seleccionar un archivo adjunto');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const token = getToken();
      const userPrincipal = JSON.parse(localStorage.getItem('user') || '{}');
      const nickname = userPrincipal.nickname || 'admin_c1';

      const formData = new FormData();
      formData.append('contenido', createForm.contenido);
      formData.append('documentoIdPaciente', createForm.documentoIdPaciente);
      formData.append('titulo', createForm.titulo);
      formData.append('autor', nickname);
      formData.append('especialidad', createForm.especialidad);
      formData.append('archivo', createForm.archivo);

      if (createForm.descripcion) {
        formData.append('descripcion', createForm.descripcion);
      }

      const response = await fetch(`${getApiBase()}/documentos/completo-con-archivo`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      });

      const text = await response.text();
      let data;
      try {
        data = JSON.parse(text);
      } catch (e) {
        setError(`Error ${response.status}: ${text || response.statusText}`);
        setLoading(false);
        return;
      }

      if (response.ok) {
        setSuccess('Documento creado exitosamente (con archivo adjunto)' + 
          (data.tienePdf ? ' (PDF generado)' : '') +
          (data.tieneArchivoAdjunto ? ' (Archivo adjunto guardado)' : ''));
        setShowCreateModal(false);
        setCreateForm(prev => ({ ...prev, archivo: null }));
        setArchivoInputKey(prev => prev + 1);
        // Recargar documentos si el CI coincide
        if (createForm.documentoIdPaciente === ciPaciente && ciPaciente) {
          setTimeout(() => buscarDocumentos(), 500);
        }
      } else {
        setError(data.error || 'Error al crear documento');
      }
    } catch (error) {
      setError(error.message || 'Error al crear documento');
    } finally {
      setLoading(false);
    }
  };

  // Generar resumen de historia clínica
  const generarResumen = async () => {
    if (!ciPaciente.trim()) {
      setError('Por favor ingrese un CI');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const token = getToken();
      const response = await fetch(`${getApiBase()}/documentos/paciente/${ciPaciente}/resumen`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      const text = await response.text();
      let data;
      try {
        data = JSON.parse(text);
      } catch (e) {
        setError(`Error ${response.status}: ${text || response.statusText}`);
        setLoading(false);
        return;
      }

      if (response.ok) {
        setResumen(data.resumen || data);
        setShowResumenModal(true);
        setSuccess('Resumen generado exitosamente');
      } else {
        setError(data.error || 'Error al generar resumen');
      }
    } catch (err) {
      setError('Error de conexión al generar resumen');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  // Solicitar acceso a documento o historia clínica
  const handleSolicitarAcceso = async () => {
    if (!getToken()) {
      setError('Debes iniciar sesión primero');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const token = getToken();
      let endpoint;
      let body;

      if (solicitudForm.historiaCompleta) {
        endpoint = `${getApiBase()}/documentos/solicitar-acceso-historia-clinica`;
        body = {
          codDocumPaciente: solicitudForm.codDocumPaciente || ciPaciente,
          razonSolicitud: solicitudForm.razonSolicitud || 'Solicitud de acceso a toda la historia clínica',
          especialidad: createForm.especialidad || 'MEDICINA_GENERAL'
        };
      } else {
        endpoint = `${getApiBase()}/documentos/solicitar-acceso`;
        body = {
          codDocumPaciente: solicitudForm.codDocumPaciente || ciPaciente,
          tipoDocumento: solicitudForm.tipoDocumento || null,
          documentoId: solicitudForm.documentoId || null,
          razonSolicitud: solicitudForm.razonSolicitud || 'Solicitud de acceso desde portal'
        };
      }

      const response = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(body)
      });

      const text = await response.text();
      let data;
      try {
        data = JSON.parse(text);
      } catch (e) {
        setError(`Error ${response.status}: ${text || response.statusText}`);
        setLoading(false);
        return;
      }

      if (response.ok) {
        setSuccess('Solicitud creada exitosamente. ID: ' + (data.id || data.solicitudId || 'N/A'));
        setShowSolicitarAccesoModal(false);
        // Recargar documentos para actualizar permisos
        if (ciPaciente) {
          setTimeout(() => buscarDocumentos(), 1000);
        }
      } else {
        // Mostrar mensaje de error del backend, o un mensaje por defecto según el código de estado
        let errorMessage = data.error || 'Error al crear solicitud';
        if (response.status === 503) {
          errorMessage = data.error || 'El servicio de políticas no está disponible. La solicitud no se pudo procesar en este momento.';
        } else if (response.status === 401) {
          errorMessage = 'No estás autenticado. Por favor, inicia sesión nuevamente.';
        } else if (response.status === 403) {
          errorMessage = 'No tienes permisos para realizar esta acción.';
        }
        setError(errorMessage);
      }
    } catch (error) {
      setError(error.message || 'Error al crear solicitud');
    } finally {
      setLoading(false);
    }
  };

  // Solicitar acceso a un documento específico
  const solicitarAccesoDocumento = (doc) => {
    setSolicitudForm({
      codDocumPaciente: doc.ciPaciente || ciPaciente,
      tipoDocumento: doc.tipoDocumento || 'EVALUACION',
      documentoId: doc.id,
      razonSolicitud: 'Solicitud de acceso al documento',
      historiaCompleta: false
    });
    setShowSolicitarAccesoModal(true);
  };

  // Descargar documento
  const descargarDocumento = async (documentoId) => {
    try {
      const token = getToken();
      const response = await fetch(`${getApiBase()}/documentos-pdf/${documentoId}`, {
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
        setSuccess('Documento descargado exitosamente');
      } else {
        const errorData = await response.json().catch(() => ({ error: 'Error al descargar documento' }));
        setError(errorData.error || 'Error al descargar el documento');
      }
    } catch (err) {
      setError('Error al descargar el documento');
      console.error('Error:', err);
    }
  };

  // Ver contenido de documento
  const verContenido = async (documentoId) => {
    if (!getToken()) {
      setError('Debes iniciar sesión primero');
      return;
    }

    setLoading(true);
    setError(null);
    setContenidoVista(null);
    setMostrarVisor(false);

    try {
      const token = getToken();
      const response = await fetch(`${getApiBase()}/documentos-pdf/${documentoId}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      const contentType = response.headers.get('Content-Type') || '';

      if (!response.ok) {
        const text = await response.text();
        let errorData;
        try {
          errorData = JSON.parse(text);
        } catch (e) {
          errorData = { error: text || response.statusText };
        }
        setError(errorData.error || `Error ${response.status}`);
        setLoading(false);
        return;
      }

      if (contentType.includes('application/pdf') || contentType.includes('application/octet-stream')) {
        const blob = await response.blob();
        const arrayBuffer = await blob.arrayBuffer();
        const uint8Array = new Uint8Array(arrayBuffer);
        const isPdf = uint8Array.length >= 4 && 
                      uint8Array[0] === 0x25 && uint8Array[1] === 0x50 && 
                      uint8Array[2] === 0x44 && uint8Array[3] === 0x46;

        if (isPdf) {
          const url = window.URL.createObjectURL(blob);
          setContenidoVista({ tipo: 'pdf', url: url, nombre: `documento-${documentoId}.pdf` });
          setMostrarVisor(true);
          setSuccess('PDF cargado exitosamente');
        } else {
          const text = await new Response(blob).text();
          try {
            const errorData = JSON.parse(text);
            setError(errorData.error || 'Error al cargar PDF');
          } catch (e) {
            setContenidoVista({ tipo: 'texto', data: text, nombre: `documento-${documentoId}.txt` });
            setMostrarVisor(true);
            setSuccess('Contenido cargado');
          }
        }
      } else if (contentType.includes('image/')) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        setContenidoVista({ tipo: 'imagen', url: url, nombre: `imagen-${documentoId}` });
        setMostrarVisor(true);
        setSuccess('Imagen cargada exitosamente');
      } else {
        const blob = await response.blob();
        const text = await blob.text();
        setContenidoVista({ tipo: 'texto', data: text, nombre: `documento-${documentoId}.txt` });
        setMostrarVisor(true);
        setSuccess('Contenido cargado exitosamente');
      }
    } catch (error) {
      setError(error.message || 'Error al cargar contenido');
    } finally {
      setLoading(false);
    }
  };

  const cerrarVisor = () => {
    if (contenidoVista && contenidoVista.url) {
      window.URL.revokeObjectURL(contenidoVista.url);
    }
    setContenidoVista(null);
    setMostrarVisor(false);
  };

  const formatDate = (dateInput) => {
    // Si es null, undefined, o string vacío
    if (dateInput === null || dateInput === undefined || dateInput === '') {
      return 'N/A';
    }
    
    try {
      let date;
      
      // Si es un número (timestamp en milisegundos desde epoch)
      if (typeof dateInput === 'number') {
        // Java Date serializado viene como timestamp en milisegundos
        // Si es un número muy pequeño (menos de 10 dígitos), podría estar en segundos
        if (dateInput > 0 && dateInput < 10000000000) {
          date = new Date(dateInput * 1000);
        } else {
          date = new Date(dateInput);
        }
      }
      // Si es un string
      else if (typeof dateInput === 'string') {
        // Si está vacío
        if (dateInput.trim() === '' || dateInput === 'null' || dateInput === 'undefined') {
          return 'N/A';
        }
        // Intentar parsear como ISO string primero
        date = new Date(dateInput);
        
        // Si falla, intentar otros formatos
        if (isNaN(date.getTime())) {
          // Formato: "2024-01-01" o "2024/01/01"
          const dateMatch = dateInput.match(/(\d{4})[-\/](\d{1,2})[-\/](\d{1,2})/);
          if (dateMatch) {
            date = new Date(parseInt(dateMatch[1]), parseInt(dateMatch[2]) - 1, parseInt(dateMatch[3]));
          } else {
            // Intentar como timestamp string
            const timestamp = parseInt(dateInput);
            if (!isNaN(timestamp)) {
              date = new Date(timestamp);
            }
          }
        }
      }
      // Si es un objeto
      else if (typeof dateInput === 'object') {
        // Formato MongoDB: { $date: "2024-01-01T00:00:00.000Z" } o { $date: 1234567890 }
        if (dateInput.$date !== undefined) {
          const dateValue = dateInput.$date;
          if (typeof dateValue === 'number') {
            date = new Date(dateValue);
          } else if (typeof dateValue === 'string') {
            date = new Date(dateValue);
          } else {
            return 'N/A';
          }
        } 
        // Si tiene método getTime (objeto Date nativo del navegador)
        else if (typeof dateInput.getTime === 'function') {
          date = dateInput;
        }
        // Si tiene propiedad time (Java Date serializado como objeto)
        else if (dateInput.time !== undefined && typeof dateInput.time === 'number') {
          date = new Date(dateInput.time);
        }
        // Si tiene propiedades year/month/day
        else if (dateInput.year !== undefined || dateInput.month !== undefined || dateInput.day !== undefined) {
          const year = dateInput.year || new Date().getFullYear();
          const month = (dateInput.month || 1) - 1; // Los meses en JS son 0-indexed
          const day = dateInput.day || 1;
          date = new Date(year, month, day);
        }
        // Si es un array [año, mes, día, ...]
        else if (Array.isArray(dateInput)) {
          date = new Date(...dateInput);
        }
        else {
          // Último recurso: convertir a string y parsear
          const dateStr = String(dateInput);
          if (dateStr === '[object Object]') {
            return 'N/A';
          }
          date = new Date(dateStr);
        }
      }
      else {
        return 'N/A';
      }
      
      // Verificar que la fecha es válida
      if (!date || isNaN(date.getTime()) || !isFinite(date.getTime())) {
        console.warn('Fecha inválida recibida:', dateInput, 'Tipo:', typeof dateInput);
        return 'N/A';
      }
      
      // Formatear la fecha
      try {
        const formatted = date.toLocaleDateString('es-UY', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        });
        // Verificar que no sea "Invalid Date"
        if (formatted.includes('Invalid') || formatted.includes('NaN')) {
          return 'N/A';
        }
        return formatted;
      } catch (formatError) {
        // Si falla el formato, intentar formato básico
        try {
          return date.toLocaleDateString('es-UY');
        } catch (e) {
          return 'N/A';
        }
      }
    } catch (e) {
      console.warn('Error formateando fecha:', dateInput, 'Tipo:', typeof dateInput, 'Error:', e);
      return 'N/A';
    }
  };

  return (
    <div style={styles.container}>
      <style>{inlineStyles}</style>
      {/* Header */}
      <div style={styles.headerCard}>
        <div style={styles.headerContent}>
          <div>
            <h2 style={styles.headerTitle}>Documentos Clínicos</h2>
            <p style={styles.headerSubtitle}>
              Gestione documentos, solicitudes de acceso y resúmenes de historia clínica
            </p>
          </div>
          <div style={styles.headerButtons}>
            <button
              onClick={() => {
                setShowCreateModal(true);
                setCreateForm(prev => ({ ...prev, documentoIdPaciente: ciPaciente }));
              }}
              style={styles.createButton}
            >
              ➕ Crear Documento
            </button>
          </div>
        </div>

        {/* Búsqueda */}
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
              🔍 Buscar Documentos
            </button>
          </div>
          
          {ciPaciente && (
            <div style={styles.actionButtonsRow}>
              <button
                onClick={generarResumen}
                disabled={loading}
                style={styles.actionButton}
              >
                📋 Generar Resumen del Paciente
              </button>
              <button
                onClick={() => {
                  setSolicitudForm({
                    codDocumPaciente: ciPaciente,
                    tipoDocumento: '',
                    documentoId: '',
                    razonSolicitud: 'Solicitud de acceso a la historia clínica',
                    historiaCompleta: true
                  });
                  setShowSolicitarAccesoModal(true);
                }}
                style={styles.actionButton}
              >
                🔐 Solicitar Acceso
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Mensajes */}
      {error && (
        <div style={styles.errorCard}>
          <span style={styles.errorIcon}>⚠️</span>
          <span>{error}</span>
          <button onClick={() => setError(null)} style={styles.closeButton}>✕</button>
        </div>
      )}
      {success && (
        <div style={styles.successCard}>
          <span style={styles.successIcon}>✅</span>
          <span>{success}</span>
          <button onClick={() => setSuccess(null)} style={styles.closeButton}>✕</button>
        </div>
      )}

      {/* Documentos PDF */}
      {documentos.length > 0 && (
        <div style={styles.documentosCard}>
          <h3 style={styles.documentosTitle}>
            Documentos PDF ({documentos.length})
          </h3>
          <div style={styles.documentosList} className="documentos-list">
            {documentos.map((doc, index) => {
              const tieneAcceso = permisosDocumentos[doc.id] !== false;
              const fechaFormateada = formatDate(doc.fechaCreacion);
              // Debug: log para ver qué está llegando
              if (fechaFormateada === 'N/A' || fechaFormateada.includes('Invalid')) {
                console.log('Fecha problema:', doc.fechaCreacion, 'Tipo:', typeof doc.fechaCreacion);
              }
              return (
                <div key={doc.id || index} style={styles.documentoItem} className="documento-item">
                  <div style={styles.documentoIcon}>📄</div>
                  <div style={styles.documentoInfo}>
                    <div style={styles.documentoHeader}>
                      <span style={styles.documentoTipo}>{doc.titulo || doc.tipoDocumento || 'Documento Clínico'}</span>
                      <span style={styles.documentoFecha}>{fechaFormateada}</span>
                    </div>
                    {doc.descripcion && (
                      <div style={styles.documentoDescripcion}>
                        {doc.descripcion.length > 200 
                          ? doc.descripcion.substring(0, 200) + '...' 
                          : doc.descripcion}
                      </div>
                    )}
                    <div style={styles.documentoMeta}>
                      <span>CI: {doc.ciPaciente || ciPaciente}</span>
                      {doc.profesionalId && <span>• Profesional: {doc.profesionalId}</span>}
                      {doc.tipoDocumento && doc.tipoDocumento !== (doc.titulo || 'EVALUACION') && (
                        <span>• Tipo: {doc.tipoDocumento}</span>
                      )}
                    </div>
                  </div>
                  <div style={styles.documentoActions}>
                    {!tieneAcceso ? (
                      <button
                        onClick={() => solicitarAccesoDocumento(doc)}
                        style={styles.solicitarButton}
                        title="Solicitar acceso"
                      >
                        🔐 Solicitar Acceso
                      </button>
                    ) : (
                      <>
                        <button
                          onClick={() => descargarDocumento(doc.id)}
                          style={styles.descargarButton}
                          title="Descargar PDF"
                        >
                          ⬇️ Descargar
                        </button>
                        <button
                          onClick={() => verContenido(doc.id)}
                          style={styles.verButton}
                          title="Ver contenido"
                        >
                          👁️ Ver Contenido
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

      {documentos.length === 0 && !loading && ciPaciente && !error && (
        <div style={styles.emptyCard}>
          <div style={styles.emptyIcon}>📭</div>
          <div style={styles.emptyText}>
            No se encontraron documentos para el CI: {ciPaciente}
          </div>
        </div>
      )}

      {/* Modal: Crear Documento */}
      {showCreateModal && (
        <div style={styles.modalOverlay} onClick={() => setShowCreateModal(false)}>
          <div style={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <div style={styles.modalHeader}>
              <h3 style={styles.modalTitle}>Crear Documento Clínico</h3>
              <button onClick={() => setShowCreateModal(false)} style={styles.modalClose}>✕</button>
            </div>
            <div style={styles.modalBody}>
              <div style={styles.formGroup}>
                <label style={styles.formLabel}>CI del Paciente *</label>
                <input
                  type="text"
                  value={createForm.documentoIdPaciente}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, documentoIdPaciente: e.target.value }))}
                  style={styles.formInput}
                  required
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Título *</label>
                <input
                  type="text"
                  value={createForm.titulo}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, titulo: e.target.value }))}
                  style={styles.formInput}
                  required
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Especialidad *</label>
                <select
                  value={createForm.especialidad}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, especialidad: e.target.value }))}
                  style={styles.formInput}
                >
                  <option value="MEDICINA_GENERAL">Medicina General</option>
                  <option value="PEDIATRIA">Pediatría</option>
                  <option value="CARDIOLOGIA">Cardiología</option>
                  <option value="DERMATOLOGIA">Dermatología</option>
                </select>
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Contenido *</label>
                <textarea
                  value={createForm.contenido}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, contenido: e.target.value }))}
                  style={styles.formTextarea}
                  rows="5"
                  required
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Descripción (opcional)</label>
                <textarea
                  value={createForm.descripcion}
                  onChange={(e) => setCreateForm(prev => ({ ...prev, descripcion: e.target.value }))}
                  style={styles.formTextarea}
                  rows="3"
                />
              </div>

              <div style={styles.formGroup}>
                <label style={{ marginRight: '15px' }}>
                  <input
                    type="radio"
                    value="sin-archivo"
                    checked={modoCreacion === 'sin-archivo'}
                    onChange={(e) => setModoCreacion(e.target.value)}
                    style={{ marginRight: '5px' }}
                  />
                  Sin Archivo Adjunto
                </label>
                <label>
                  <input
                    type="radio"
                    value="con-archivo"
                    checked={modoCreacion === 'con-archivo'}
                    onChange={(e) => setModoCreacion(e.target.value)}
                    style={{ marginRight: '5px' }}
                  />
                  Con Archivo Adjunto
                </label>
              </div>

              {modoCreacion === 'con-archivo' && (
                <div style={styles.formGroup}>
                  <label style={styles.formLabel}>Archivo Adjunto</label>
                  <input
                    key={archivoInputKey}
                    ref={fileInputRef}
                    type="file"
                    onChange={(e) => setCreateForm(prev => ({ ...prev, archivo: e.target.files[0] }))}
                    style={styles.formInput}
                  />
                </div>
              )}

              <div style={styles.modalActions}>
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  style={styles.cancelButton}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={modoCreacion === 'sin-archivo' ? handleCrearDocumentoSinArchivo : handleCrearDocumentoConArchivo}
                  disabled={loading}
                  style={styles.submitButton}
                >
                  {loading ? 'Creando...' : 'Crear Documento'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Solicitar Acceso */}
      {showSolicitarAccesoModal && (
        <div style={styles.modalOverlay} onClick={() => setShowSolicitarAccesoModal(false)}>
          <div style={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <div style={styles.modalHeader}>
              <h3 style={styles.modalTitle}>Solicitar Acceso</h3>
              <button onClick={() => setShowSolicitarAccesoModal(false)} style={styles.modalClose}>✕</button>
            </div>
            <div style={styles.modalBody}>
              <div style={styles.formGroup}>
                <label>
                  <input
                    type="checkbox"
                    checked={solicitudForm.historiaCompleta}
                    onChange={(e) => setSolicitudForm(prev => ({ ...prev, historiaCompleta: e.target.checked }))}
                    style={{ marginRight: '5px' }}
                  />
                  Solicitar acceso a historia clínica completa
                </label>
              </div>

              {!solicitudForm.historiaCompleta && (
                <>
                  <div style={styles.formGroup}>
                    <label style={styles.formLabel}>Tipo de Documento (opcional)</label>
                    <input
                      type="text"
                      value={solicitudForm.tipoDocumento}
                      onChange={(e) => setSolicitudForm(prev => ({ ...prev, tipoDocumento: e.target.value }))}
                      style={styles.formInput}
                      placeholder="EVALUACION"
                    />
                  </div>

                  <div style={styles.formGroup}>
                    <label style={styles.formLabel}>Documento ID (opcional)</label>
                    <input
                      type="text"
                      value={solicitudForm.documentoId}
                      onChange={(e) => setSolicitudForm(prev => ({ ...prev, documentoId: e.target.value }))}
                      style={styles.formInput}
                    />
                  </div>
                </>
              )}

              <div style={styles.formGroup}>
                <label style={styles.formLabel}>Razón de Solicitud</label>
                <textarea
                  value={solicitudForm.razonSolicitud}
                  onChange={(e) => setSolicitudForm(prev => ({ ...prev, razonSolicitud: e.target.value }))}
                  style={styles.formTextarea}
                  rows="3"
                />
              </div>

              <div style={styles.modalActions}>
                <button
                  type="button"
                  onClick={() => setShowSolicitarAccesoModal(false)}
                  style={styles.cancelButton}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={handleSolicitarAcceso}
                  disabled={loading}
                  style={styles.submitButton}
                >
                  {loading ? 'Enviando...' : 'Enviar Solicitud'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Resumen */}
      {showResumenModal && resumen && (
        <div style={styles.modalOverlay} onClick={() => setShowResumenModal(false)}>
          <div style={styles.modalContentLarge} onClick={(e) => e.stopPropagation()}>
            <div style={styles.modalHeader}>
              <h3 style={styles.modalTitle}>Resumen de Historia Clínica - CI: {ciPaciente}</h3>
              <button onClick={() => setShowResumenModal(false)} style={styles.modalClose}>✕</button>
            </div>
            <div style={styles.modalBody}>
              <div style={styles.resumenContent}>
                {typeof resumen === 'string' ? resumen : JSON.stringify(resumen, null, 2)}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Visor de Contenido */}
      {mostrarVisor && contenidoVista && (
        <div style={styles.visorOverlay} onClick={cerrarVisor}>
          <div style={styles.visorContent} onClick={(e) => e.stopPropagation()}>
            <div style={styles.visorHeader}>
              <h3>
                {contenidoVista.tipo === 'pdf' && '📄 Visor de PDF'}
                {contenidoVista.tipo === 'imagen' && '🖼️ Visor de Imagen'}
                {contenidoVista.tipo === 'texto' && '📝 Contenido'}
              </h3>
              <button onClick={cerrarVisor} style={styles.visorClose}>✕ Cerrar</button>
            </div>
            <div style={styles.visorBody}>
              {contenidoVista.tipo === 'pdf' && (
                <iframe
                  src={contenidoVista.url}
                  style={styles.visorIframe}
                  title={contenidoVista.nombre}
                />
              )}
              {contenidoVista.tipo === 'imagen' && (
                <img
                  src={contenidoVista.url}
                  alt={contenidoVista.nombre}
                  style={styles.visorImage}
                />
              )}
              {contenidoVista.tipo === 'texto' && (
                <pre style={styles.visorText}>{contenidoVista.data}</pre>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const styles = {
  container: {
    padding: 'clamp(12px, 2vw, 16px)',
    maxWidth: '1400px',
    margin: '0 auto',
    width: '100%',
    boxSizing: 'border-box'
  },
  headerCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: 'clamp(16px, 3vw, 20px)',
    marginBottom: '24px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  headerContent: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px',
    flexWrap: 'wrap',
    gap: '12px'
  },
  headerTitle: {
    margin: '0 0 4px 0',
    fontSize: 'clamp(20px, 4vw, 24px)',
    fontWeight: '700',
    color: '#111827'
  },
  headerSubtitle: {
    margin: 0,
    fontSize: '14px',
    color: '#6b7280'
  },
  headerButtons: {
    display: 'flex',
    gap: '8px',
    flexWrap: 'wrap'
  },
  createButton: {
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 24px',
    fontSize: 'clamp(13px, 2.5vw, 15px)',
    fontWeight: '600',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    minWidth: '180px',
    textAlign: 'center',
    transition: 'all 0.2s'
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
    flex: '1 1 200px',
    minWidth: '150px',
    padding: '12px 16px',
    border: '2px solid #e5e7eb',
    borderRadius: '8px',
    fontSize: '15px',
    outline: 'none',
    boxSizing: 'border-box',
    width: '100%'
  },
  searchButton: {
    padding: '12px 24px',
    backgroundColor: '#10b981',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: 'clamp(13px, 2.5vw, 15px)',
    fontWeight: '600',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    minWidth: '180px',
    textAlign: 'center',
    transition: 'all 0.2s'
  },
  actionButtonsRow: {
    display: 'flex',
    gap: '12px',
    marginTop: '12px',
    flexWrap: 'wrap',
    justifyContent: 'flex-start',
    alignItems: 'center'
  },
  actionButton: {
    padding: '12px 24px',
    backgroundColor: '#8b5cf6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: 'clamp(13px, 2.5vw, 15px)',
    fontWeight: '600',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    minWidth: '180px',
    textAlign: 'center',
    transition: 'all 0.2s'
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
  successCard: {
    backgroundColor: '#f0fdf4',
    border: '1px solid #bbf7d0',
    borderRadius: '8px',
    padding: '16px',
    marginBottom: '24px',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    color: '#166534'
  },
  errorIcon: { fontSize: '20px' },
  successIcon: { fontSize: '20px' },
  closeButton: {
    marginLeft: 'auto',
    background: 'none',
    border: 'none',
    fontSize: '18px',
    cursor: 'pointer',
    color: 'inherit'
  },
  documentosCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: 'clamp(16px, 3vw, 24px)',
    marginBottom: '24px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  documentosTitle: {
    margin: '0 0 20px 0',
    fontSize: '20px',
    fontWeight: '700',
    color: '#111827'
  },
  documentosList: {
    display: 'flex !important',
    flexDirection: 'column !important',
    gap: '12px',
    width: '100%',
    maxWidth: '100%',
    boxSizing: 'border-box'
  },
  documentoItem: {
    display: 'flex !important',
    flexDirection: 'row !important',
    alignItems: 'flex-start',
    gap: 'clamp(12px, 2vw, 16px)',
    padding: 'clamp(16px, 3vw, 20px)',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    flexWrap: 'wrap',
    width: '100% !important',
    maxWidth: '100% !important',
    minWidth: '0',
    boxSizing: 'border-box',
    backgroundColor: 'white',
    marginBottom: '0'
  },
  documentoIcon: { fontSize: '32px', flexShrink: 0, marginTop: '2px' },
  documentoInfo: { flex: '1 1 300px', minWidth: '0', width: '100%' },
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
    marginBottom: '8px',
    lineHeight: '1.5',
    maxHeight: '60px',
    overflow: 'hidden',
    textOverflow: 'ellipsis'
  },
  documentoMeta: {
    fontSize: '13px',
    color: '#9ca3af',
    display: 'flex',
    gap: '12px',
    flexWrap: 'wrap',
    marginTop: '8px'
  },
  documentoActions: {
    display: 'flex',
    gap: '10px',
    flexWrap: 'wrap',
    flexShrink: 0,
    width: '100%',
    justifyContent: 'flex-end',
    marginTop: '12px',
    alignItems: 'center'
  },
  solicitarButton: {
    backgroundColor: '#f59e0b',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: '10px 20px',
    fontSize: 'clamp(13px, 2.5vw, 14px)',
    fontWeight: '600',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    minWidth: '150px',
    textAlign: 'center',
    transition: 'all 0.2s'
  },
  descargarButton: {
    backgroundColor: '#10b981',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: '10px 20px',
    fontSize: 'clamp(13px, 2.5vw, 14px)',
    fontWeight: '600',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    minWidth: '150px',
    textAlign: 'center',
    transition: 'all 0.2s'
  },
  verButton: {
    backgroundColor: '#3b82f6',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    padding: '10px 20px',
    fontSize: 'clamp(13px, 2.5vw, 14px)',
    fontWeight: '600',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    minWidth: '150px',
    textAlign: 'center',
    transition: 'all 0.2s'
  },
  emptyCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '32px 20px',
    textAlign: 'center',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  emptyIcon: { fontSize: '64px', marginBottom: '16px' },
  emptyText: { fontSize: '16px', color: '#6b7280' },
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
    padding: '20px',
    boxSizing: 'border-box'
  },
  modalContent: {
    backgroundColor: 'white',
    borderRadius: '12px',
    width: '100%',
    maxWidth: '600px',
    maxHeight: '90vh',
    overflow: 'auto',
    margin: '10px'
  },
  modalContentLarge: {
    backgroundColor: 'white',
    borderRadius: '12px',
    width: '100%',
    maxWidth: '800px',
    maxHeight: '90vh',
    overflow: 'auto',
    margin: '10px'
  },
  modalHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '20px',
    borderBottom: '1px solid #e5e7eb',
    flexWrap: 'wrap',
    gap: '12px'
  },
  modalTitle: {
    margin: 0,
    fontSize: '20px',
    fontWeight: '700',
    color: '#111827'
  },
  modalClose: {
    background: 'none',
    border: 'none',
    fontSize: '24px',
    cursor: 'pointer',
    color: '#6b7280'
  },
  modalBody: {
    padding: '20px'
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
  },
  resumenContent: {
    padding: '20px',
    backgroundColor: '#f9fafb',
    borderRadius: '8px',
    whiteSpace: 'pre-wrap',
    lineHeight: '1.6',
    fontSize: '15px',
    overflow: 'auto',
    maxHeight: '60vh'
  },
  visorOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    background: 'rgba(0,0,0,0.8)',
    zIndex: 2000,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '20px',
    boxSizing: 'border-box'
  },
  visorContent: {
    background: 'white',
    borderRadius: '8px',
    width: '100%',
    maxWidth: '1200px',
    height: '90%',
    maxHeight: '90vh',
    display: 'flex',
    flexDirection: 'column',
    boxShadow: '0 4px 20px rgba(0,0,0,0.3)'
  },
  visorHeader: {
    padding: '15px 20px',
    borderBottom: '1px solid #ddd',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    background: '#f5f5f5',
    flexShrink: 0
  },
  visorClose: {
    background: '#dc3545',
    color: 'white',
    border: 'none',
    padding: '8px 16px',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: 'bold'
  },
  visorBody: {
    flex: 1,
    overflow: 'auto',
    padding: '20px',
    background: '#fff',
    minHeight: 0
  },
  visorIframe: {
    width: '100%',
    height: '100%',
    border: 'none',
    minHeight: '400px'
  },
  visorImage: {
    maxWidth: '100%',
    maxHeight: '100%',
    objectFit: 'contain'
  },
  visorText: {
    background: '#f8f9fa',
    padding: '15px',
    borderRadius: '4px',
    overflow: 'auto',
    fontSize: '14px',
    lineHeight: '1.5',
    whiteSpace: 'pre-wrap',
    wordWrap: 'break-word',
    fontFamily: 'monospace',
    maxHeight: '70vh'
  }
};

export default DocumentosPage;
