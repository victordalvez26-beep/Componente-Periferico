import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import ClinicAdmin from './components/ClinicAdmin';
import ActivatePage from './components/ActivatePage';
import './index.css';

/**
 * Path-based routing para multi-tenancy:
 * - /portal/clinica-{id}/activate - Activación de cuenta
 * - /portal/clinica-{id}/login - Login de la clínica
 * - /portal/clinica-{id}/* - Portal de la clínica
 */
export default function App(){
  return (
    <Router>
      <Routes>
        {/* Ruta de activación: /portal/clinica-123/activate?token=xyz */}
        <Route path="/portal/clinica-:tenantId/activate" element={<ActivatePage />} />
        
        {/* Ruta principal del portal de cada clínica */}
        <Route path="/portal/clinica-:tenantId/*" element={<ClinicPortal />} />
        
        {/* Ruta legacy para testing (sin tenant específico) */}
        <Route path="/" element={<LegacyPortal />} />
        
        {/* Redirect cualquier otra ruta al home */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}

/**
 * Portal de clínica específica (path-based).
 * El tenantId se extrae de la URL: /portal/clinica-123
 */
function ClinicPortal() {
  const { tenantId } = useParams();
  
  return (
    <div className="app">
      <div className="header">
        <h1 className="title">Nodo Periférico - Clínica {tenantId}</h1>
        <p style={{color: '#6b7280', fontSize: '14px', marginTop: '5px'}}>
          Portal de Administración y Profesionales
        </p>
      </div>
      <main>
        <ClinicAdmin tenantId={tenantId} />
      </main>
      <div className="footer">
        Clínica {tenantId} — Endpoints: <code>/config</code>, <code>/api</code>
      </div>
    </div>
  );
}

/**
 * Portal legacy para testing sin tenant específico.
 */
function LegacyPortal() {
  return (
    <div className="app">
      <div className="header">
        <h1 className="title">Nodo Periférico - Admin Clínica / Profesionales</h1>
      </div>
      <main>
        <ClinicAdmin />
      </main>
      <div className="footer">Built for local testing — endpoints: <code>/nodo-periferico</code></div>
    </div>
  );
}
