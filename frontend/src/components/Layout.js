import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, useParams } from 'react-router-dom';

function Layout({ children }) {
  const { tenantId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [userRole, setUserRole] = useState(null);
  const [username, setUsername] = useState('');
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

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

  const menuItems = {
    ADMINISTRADOR: [
      { icon: '🏠', label: 'Inicio', path: `/portal/clinica/${tenantId}/home` },
      { icon: '🩺', label: 'Profesionales', path: `/portal/clinica/${tenantId}/profesionales` },
      { icon: '👥', label: 'Usuarios de Salud', path: `/portal/clinica/${tenantId}/usuarios` },
      { icon: '📄', label: 'Documentos', path: `/portal/clinica/${tenantId}/documentos` },
      { icon: '⚙️', label: 'Configuración', path: `/portal/clinica/${tenantId}/configuracion` },
    ],
    PROFESIONAL: [
      { icon: '🏠', label: 'Inicio', path: `/portal/clinica/${tenantId}/home` },
      { icon: '👤', label: 'Pacientes', path: `/portal/clinica/${tenantId}/pacientes` },
      { icon: '📄', label: 'Documentos', path: `/portal/clinica/${tenantId}/documentos` },
    ]
  };

  const currentMenu = menuItems[userRole] || menuItems.PROFESIONAL;
  const isActive = (path) => location.pathname === path;

  return (
    <div style={styles.container}>
      {/* Sidebar */}
      <aside style={{
        ...styles.sidebar,
        width: sidebarCollapsed ? '70px' : '260px'
      }}>
        {/* Logo */}
        <div style={styles.logoContainer}>
          <div style={styles.logo}>🏥</div>
          {!sidebarCollapsed && (
            <div>
              <div style={styles.logoText}>Clínica {tenantId}</div>
              <div style={styles.logoSubtext}>
                {userRole === 'ADMINISTRADOR' ? 'Admin Portal' : 'Portal Profesional'}
              </div>
            </div>
          )}
        </div>

        {/* Collapse Button */}
        <button
          onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
          style={styles.collapseButton}
          title={sidebarCollapsed ? 'Expandir' : 'Contraer'}
        >
          {sidebarCollapsed ? '→' : '←'}
        </button>

        {/* Navigation */}
        <nav style={styles.nav}>
          {currentMenu.map((item, index) => (
            <button
              key={index}
              onClick={() => navigate(item.path)}
              style={{
                ...styles.navItem,
                ...(isActive(item.path) ? styles.navItemActive : {})
              }}
              title={sidebarCollapsed ? item.label : ''}
            >
              <span style={styles.navIcon}>{item.icon}</span>
              {!sidebarCollapsed && <span>{item.label}</span>}
            </button>
          ))}
        </nav>

        {/* User Info */}
        <div style={styles.userSection}>
          <div style={styles.userInfo}>
            <div style={styles.avatar}>
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
            style={styles.logoutButton}
            title="Cerrar sesión"
          >
            🚪
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main style={styles.main}>
        {/* Top Bar */}
        <header style={styles.header}>
          <div style={styles.headerContent}>
            <h1 style={styles.headerTitle}>
              {getPageTitle(location.pathname)}
            </h1>
            <div style={styles.headerActions}>
              <span style={styles.tenantBadge}>
                Tenant: {tenantId}
              </span>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <div style={styles.content}>
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
    fontFamily: 'system-ui, -apple-system, sans-serif',
    width: '100%',
    overflow: 'hidden'
  },
  sidebar: {
    backgroundColor: '#1f2937',
    color: 'white',
    display: 'flex',
    flexDirection: 'column',
    transition: 'width 0.3s ease',
    position: 'relative',
    minHeight: '100vh',
    overflow: 'hidden'
  },
  logoContainer: {
    padding: '20px 16px',
    borderBottom: '1px solid rgba(255,255,255,0.1)',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    minHeight: '70px',
    boxSizing: 'border-box'
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
    backgroundColor: '#3b82f6',
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
    padding: '16px 0',
    overflowY: 'auto',
    overflowX: 'hidden'
  },
  navItem: {
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px 16px',
    backgroundColor: 'transparent',
    borderTop: 'none',
    borderRight: 'none',
    borderBottom: 'none',
    borderLeft: '4px solid transparent',
    color: 'white',
    fontSize: '14px',
    cursor: 'pointer',
    transition: 'all 0.2s',
    textAlign: 'left',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis'
  },
  navItemActive: {
    backgroundColor: 'rgba(59, 130, 246, 0.2)',
    borderLeft: '4px solid #3b82f6'
  },
  navIcon: {
    fontSize: '20px'
  },
  userSection: {
    borderTop: '1px solid rgba(255,255,255,0.1)',
    padding: '16px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '12px',
    minHeight: '70px',
    boxSizing: 'border-box'
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
    backgroundColor: '#3b82f6',
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
    width: '36px',
    height: '36px',
    borderRadius: '8px',
    backgroundColor: 'rgba(239, 68, 68, 0.2)',
    border: 'none',
    color: 'white',
    fontSize: '18px',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  main: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden',
    minWidth: 0
  },
  header: {
    backgroundColor: 'white',
    borderBottom: '1px solid #e5e7eb',
    padding: '0 24px',
    position: 'sticky',
    top: 0,
    zIndex: 10,
    boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
  },
  headerContent: {
    height: '70px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '16px'
  },
  headerTitle: {
    margin: 0,
    fontSize: 'clamp(18px, 3vw, 24px)',
    fontWeight: '700',
    color: '#111827',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis'
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
    padding: '24px',
    boxSizing: 'border-box'
  }
};

export default Layout;

