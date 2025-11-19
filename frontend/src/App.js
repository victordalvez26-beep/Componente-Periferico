import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './components/LoginPage';
import ActivatePage from './components/ActivatePage';
import TenantProtectedRoute from './components/TenantProtectedRoute';
import Layout from './components/Layout';
import HomePage from './pages/HomePage';
import ProfesionalesPage from './pages/ProfesionalesPage';
import UsuariosSaludPage from './pages/UsuariosSaludPage';
import ConfiguracionPage from './pages/ConfiguracionPage';
import DocumentosPage from './pages/DocumentosPage';
import './index.css';

/**
 * Aplicación Multi-Tenant para Componente Periférico HCEN
 * 
 * Arquitectura de Rutas:
 * - /portal/clinica/:tenantId/login - Login específico por clínica
 * - /portal/clinica/:tenantId/activate - Activación de cuenta admin
 * - /portal/clinica/:tenantId/home - Dashboard (protegido)
 * - /portal/clinica/:tenantId/profesionales - Gestión de profesionales (protegido)
 * - /portal/clinica/:tenantId/usuarios - Gestión de usuarios de salud (protegido)
 * - /portal/clinica/:tenantId/configuracion - Configuración del portal (protegido)
 * 
 * Seguridad Multi-Tenant:
 * - TenantProtectedRoute valida que el tenant_id del JWT coincida con la URL
 * - Previene que un usuario de la clínica A acceda a datos de la clínica B
 * - Layout maneja autenticación y roles (ADMINISTRADOR vs PROFESIONAL)
 */
export default function App() {
  return (
    <Router>
      <Routes>
        {/* ========== RUTAS PÚBLICAS ========== */}
        
        {/* Login por tenant */}
        <Route 
          path="/portal/clinica/:tenantId/login" 
          element={<LoginPage />} 
        />
        
        {/* Activación de cuenta admin */}
        <Route 
          path="/portal/clinica/:tenantId/activate" 
          element={<ActivatePage />} 
        />

        {/* ========== RUTAS PROTEGIDAS ========== */}
        
        {/* Dashboard / Home */}
        <Route 
          path="/portal/clinica/:tenantId/home" 
          element={
            <TenantProtectedRoute>
              <Layout>
                <HomePage />
              </Layout>
            </TenantProtectedRoute>
          } 
        />

        {/* Gestión de Profesionales de Salud (Admin) */}
        <Route 
          path="/portal/clinica/:tenantId/profesionales" 
          element={
            <TenantProtectedRoute>
              <Layout>
                <ProfesionalesPage />
              </Layout>
            </TenantProtectedRoute>
          } 
        />

        {/* Gestión de Usuarios de Salud / INUS (Admin) */}
        <Route 
          path="/portal/clinica/:tenantId/usuarios" 
          element={
            <TenantProtectedRoute>
              <Layout>
                <UsuariosSaludPage />
              </Layout>
            </TenantProtectedRoute>
          } 
        />

        {/* Gestión de Pacientes (Profesional) - Misma funcionalidad que usuarios */}
        <Route 
          path="/portal/clinica/:tenantId/pacientes" 
          element={
            <TenantProtectedRoute>
              <Layout>
                <UsuariosSaludPage />
              </Layout>
            </TenantProtectedRoute>
          } 
        />

        {/* Gestión de Documentos Clínicos (Profesional) */}
        <Route 
          path="/portal/clinica/:tenantId/documentos" 
          element={
            <TenantProtectedRoute>
              <Layout>
                <DocumentosPage />
              </Layout>
            </TenantProtectedRoute>
          } 
        />

        {/* Configuración del Portal (Admin) */}
        <Route 
          path="/portal/clinica/:tenantId/configuracion" 
          element={
            <TenantProtectedRoute>
              <Layout>
                <ConfiguracionPage />
              </Layout>
            </TenantProtectedRoute>
          } 
        />

        {/* ========== REDIRECTS ========== */}
        
        {/* Root redirect a instrucciones */}
        <Route 
          path="/" 
          element={
            <div style={styles.landingPage}>
              <div style={styles.landingCard}>
                <div style={styles.landingIcon}>🏥</div>
                <h1 style={styles.landingTitle}>
                  Componente Periférico Multi-Tenant
                </h1>
                <p style={styles.landingSubtitle}>
                  Sistema de Historias Clínicas Electrónicas
                </p>
                <div style={styles.landingInfo}>
                  <h3 style={styles.landingInfoTitle}>¿Cómo acceder?</h3>
                  <p style={styles.landingInfoText}>
                    Use la URL específica de su clínica:
                  </p>
                  <div style={styles.landingExample}>
                    <code>http://localhost:3001/portal/clinica/[ID]/login</code>
                  </div>
                  <p style={styles.landingFooter}>
                    Si no tiene credenciales, contacte al administrador de HCEN.
                  </p>
                </div>
              </div>
            </div>
          } 
        />

        {/* Catch-all: redirect a home si no existe la ruta */}
        <Route 
          path="*" 
          element={<Navigate to="/" replace />} 
        />
      </Routes>
    </Router>
  );
}

const styles = {
  landingPage: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    padding: '20px',
    fontFamily: 'system-ui, -apple-system, sans-serif'
  },
  landingCard: {
    backgroundColor: 'white',
    borderRadius: '16px',
    boxShadow: '0 20px 60px rgba(0,0,0,0.3)',
    padding: '60px 40px',
    maxWidth: '600px',
    textAlign: 'center'
  },
  landingIcon: {
    fontSize: '80px',
    marginBottom: '24px'
  },
  landingTitle: {
    margin: '0 0 12px 0',
    fontSize: '32px',
    fontWeight: '700',
    color: '#111827'
  },
  landingSubtitle: {
    margin: '0 0 40px 0',
    fontSize: '18px',
    color: '#6b7280'
  },
  landingInfo: {
    backgroundColor: '#f9fafb',
    borderRadius: '12px',
    padding: '32px',
    textAlign: 'left'
  },
  landingInfoTitle: {
    margin: '0 0 16px 0',
    fontSize: '20px',
    fontWeight: '700',
    color: '#111827'
  },
  landingInfoText: {
    margin: '0 0 12px 0',
    fontSize: '15px',
    color: '#374151'
  },
  landingExample: {
    backgroundColor: '#1f2937',
    color: '#10b981',
    padding: '16px',
    borderRadius: '8px',
    fontFamily: 'monospace',
    fontSize: '14px',
    marginBottom: '20px',
    wordBreak: 'break-all'
  },
  landingFooter: {
    margin: '0',
    fontSize: '14px',
    color: '#9ca3af',
    fontStyle: 'italic'
  }
};
