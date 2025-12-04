import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, useParams } from 'react-router-dom';
import { useClinicConfig } from '../hooks/useClinicConfig';
import './Layout.css';

function Layout({ children }) {
  const { tenantId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [userRole, setUserRole] = useState(null);
  const [username, setUsername] = useState('');
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { config, loading: configLoading } = useClinicConfig(tenantId);

  useEffect(() => {
    const role = localStorage.getItem('role');
    const user = localStorage.getItem('username');
    setUserRole(role);
    setUsername(user);
  }, []);

  const handleLogout = () => {
    localStorage.clear();
    navigate(`/portal/clinica/${tenantId}/login`, { replace: true });
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
        color: '#6b7280',
        backgroundColor: '#f9fafb'
      }}>
        Cargando configuración...
      </div>
    );
  }

  const menuItems = {
    ADMINISTRADOR: [
      { icon: '', label: 'Inicio', path: `/portal/clinica/${tenantId}/home` },
      { icon: '', label: 'Profesionales', path: `/portal/clinica/${tenantId}/profesionales` },
      { icon: '', label: 'Usuarios de Salud', path: `/portal/clinica/${tenantId}/usuarios` },
      { icon: '', label: 'Configuración', path: `/portal/clinica/${tenantId}/configuracion` },
    ],
    PROFESIONAL: [
      { icon: '', label: 'Inicio', path: `/portal/clinica/${tenantId}/home` },
      { icon: '', label: 'Pacientes', path: `/portal/clinica/${tenantId}/pacientes` },
      { icon: '', label: 'Documentos', path: `/portal/clinica/${tenantId}/documentos` },
    ]
  };

  const currentMenu = menuItems[userRole] || menuItems.PROFESIONAL;
  const isActive = (path) => location.pathname === path;

  return (
    <div style={styles.container} className="layout-container">
      {/* Sidebar */}
      <aside style={{
        ...styles.sidebar,
        width: sidebarCollapsed ? '70px' : '260px'
      }} className={`layout-sidebar ${sidebarCollapsed ? 'collapsed' : ''} ${mobileMenuOpen ? 'mobile-open' : ''}`}>
        {/* Logo */}
        <div style={styles.logoContainer} className="layout-logo-container">
          {config.logoUrl ? (
            <img 
              src={config.logoUrl} 
              alt="Logo" 
              style={{
                width: '40px',
                height: '40px',
                objectFit: 'contain',
                borderRadius: '8px'
              }}
            />
          ) : (
            <div style={styles.logo}>HCEN</div>
          )}
          {!sidebarCollapsed && (
            <div>
              <div style={styles.logoText}>
                {config.nombrePortal || `Clínica ${tenantId}`}
              </div>
              <div style={styles.logoSubtext}>
                {userRole === 'ADMINISTRADOR' ? 'Admin Portal' : 'Portal Profesional'}
              </div>
            </div>
          )}
        </div>

        {/* Collapse Button */}
        <button
          onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
          style={{
            ...styles.collapseButton,
            backgroundColor: config.colorPrimario
          }}
          title={sidebarCollapsed ? 'Expandir' : 'Contraer'}
        >
          {sidebarCollapsed ? '→' : '←'}
        </button>

        {/* Navigation */}
        <nav style={styles.nav} className="layout-nav">
          {currentMenu.map((item, index) => (
            <button
              key={index}
              onClick={() => {
                navigate(item.path);
                setMobileMenuOpen(false);
              }}
              style={{
                ...styles.navItem,
                ...(isActive(item.path) ? {
                  backgroundColor: `rgba(${hexToRgb(config.colorPrimario)}, 0.2)`,
                  borderLeft: `4px solid ${config.colorPrimario}`
                } : {})
              }}
              title={sidebarCollapsed ? item.label : ''}
              className="layout-nav-item"
            >
              {item.icon && <span style={styles.navIcon}>{item.icon}</span>}
              {!sidebarCollapsed && <span>{item.label}</span>}
            </button>
          ))}
        </nav>

        {/* User Info */}
        <div style={styles.userSection} className="layout-user-section">
          <div style={styles.userInfo}>
            <div style={{
              ...styles.avatar,
              backgroundColor: config.colorPrimario
            }}>
              {username.charAt(0).toUpperCase()}
            </div>
            {!sidebarCollapsed && (
              <div style={styles.userDetails}>
                <div style={styles.userName}>{username}</div>
                <div style={styles.userRole}>
                  {userRole === 'ADMINISTRADOR' ? 'Administrador' : 'Profesional'}
                </div>
              </div>
            )}
          </div>
          <button
            onClick={handleLogout}
            style={{
              ...styles.logoutButton,
              minWidth: sidebarCollapsed ? '36px' : 'auto',
              fontSize: sidebarCollapsed ? '18px' : '14px',
              padding: sidebarCollapsed ? '0' : '8px 12px'
            }}
            title="Cerrar sesión"
          >
            {sidebarCollapsed ? '🚪' : 'Salir'}
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main style={styles.main} className="layout-main">
        {/* Top Bar */}
        <header style={styles.header} className="layout-header">
          <div style={styles.headerContent} className="layout-header-content">
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                style={styles.mobileMenuButton}
                className="mobile-menu-button"
                title="Menú"
              >
                ☰
              </button>
              <h1 style={styles.headerTitle} className="layout-header-title">
                {getPageTitle(location.pathname)}
              </h1>
            </div>
            <div style={styles.headerActions} className="layout-header-actions">
              <span style={styles.tenantBadge} className="layout-tenant-badge">
                Tenant: {tenantId}
              </span>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <div style={styles.content} className="layout-content">
          {children}
        </div>
      </main>
    </div>
  );
}

function getPageTitle(pathname) {
  if (pathname.includes('/home')) return 'Inicio';
  if (pathname.includes('/profesionales')) return 'Gestión de Profesionales';
  if (pathname.includes('/usuarios')) return 'Usuarios de Salud';
  if (pathname.includes('/configuracion')) return 'Configuración';
  if (pathname.includes('/pacientes')) return 'Mis Pacientes';
  if (pathname.includes('/documentos')) return 'Documentos Clínicos';
  return 'Portal';
}

const styles = {
  container: {
    display: 'flex',
    minHeight: '100vh',
    backgroundColor: '#f3f4f6',
    fontFamily: 'system-ui, -apple-system, sans-serif'
  },
  sidebar: {
    backgroundColor: '#1f2937',
    color: 'white',
    display: 'flex',
    flexDirection: 'column',
    transition: 'width 0.3s ease',
    position: 'relative'
  },
  logoContainer: {
    padding: '24px 20px',
    borderBottom: '1px solid rgba(255,255,255,0.1)',
    display: 'flex',
    alignItems: 'center',
    gap: '12px'
  },
  logo: {
    fontSize: '32px'
  },
  logoText: {
    fontSize: '18px',
    fontWeight: '700'
  },
  logoSubtext: {
    fontSize: '12px',
    opacity: 0.7,
    marginTop: '2px'
  },
  collapseButton: {
    position: 'absolute',
    top: '30px',
    right: '-15px',
    width: '30px',
    height: '30px',
    borderRadius: '50%',
    backgroundColor: '#3b82f6', // Se sobrescribe dinámicamente
    color: 'white',
    border: '2px solid #1f2937',
    cursor: 'pointer',
    fontSize: '14px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 10
  },
  nav: {
    flex: 1,
    padding: '20px 0',
    overflowY: 'auto'
  },
  navItem: {
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '14px 20px',
    backgroundColor: 'transparent',
    border: 'none',
    color: 'white',
    fontSize: '15px',
    cursor: 'pointer',
    transition: 'all 0.2s',
    textAlign: 'left'
  },
  navItemActive: {
    // Se aplica dinámicamente con los colores de la configuración
  },
  navIcon: {
    fontSize: '20px'
  },
  userSection: {
    borderTop: '1px solid rgba(255,255,255,0.1)',
    padding: '16px 20px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between'
  },
  userInfo: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px'
  },
  avatar: {
    width: '40px',
    height: '40px',
    borderRadius: '50%',
    backgroundColor: '#3b82f6', // Se sobrescribe dinámicamente
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: '700',
    fontSize: '18px'
  },
  userDetails: {
    flex: 1
  },
  userName: {
    fontSize: '14px',
    fontWeight: '600'
  },
  userRole: {
    fontSize: '12px',
    opacity: 0.7
  },
  logoutButton: {
    height: '36px',
    borderRadius: '8px',
    backgroundColor: 'rgba(239, 68, 68, 0.2)',
    border: 'none',
    color: 'white',
    fontSize: '14px',
    cursor: 'pointer',
    transition: 'all 0.2s',
    padding: '8px 12px',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    fontWeight: '600',
    minWidth: 'auto',
    flexShrink: 0
  },
  main: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden'
  },
  header: {
    backgroundColor: 'white',
    borderBottom: '1px solid #e5e7eb',
    padding: '0 32px'
  },
  headerContent: {
    height: '70px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between'
  },
  headerTitle: {
    margin: 0,
    fontSize: '24px',
    fontWeight: '700',
    color: '#111827'
  },
  headerActions: {
    display: 'flex',
    alignItems: 'center',
    gap: '16px'
  },
  tenantBadge: {
    padding: '6px 12px',
    backgroundColor: '#e0e7ff',
    color: '#3730a3',
    borderRadius: '6px',
    fontSize: '13px',
    fontWeight: '600'
  },
  content: {
    flex: 1,
    overflow: 'auto',
    padding: '32px',
    boxSizing: 'border-box',
    width: '100%',
    minWidth: 0,
    maxWidth: '100%'
  },
  mobileMenuButton: {
    display: 'none',
    backgroundColor: 'transparent',
    border: 'none',
    fontSize: '24px',
    cursor: 'pointer',
    color: '#111827',
    padding: '8px',
    borderRadius: '4px',
    transition: 'background-color 0.2s'
  }
};

// Función auxiliar para convertir hex a RGB
function hexToRgb(hex) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result 
    ? `${parseInt(result[1], 16)}, ${parseInt(result[2], 16)}, ${parseInt(result[3], 16)}`
    : '59, 130, 246'; // Default azul
}

export default Layout;

