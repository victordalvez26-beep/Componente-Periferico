import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';

function HomePage() {
  const { tenantId } = useParams();
  const [stats, setStats] = useState({
    profesionales: 0,
    usuarios: 0,
    documentos: 0,
    consultas: 0
  });
  const [actividades, setActividades] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingActividades, setLoadingActividades] = useState(true);

  useEffect(() => {
    fetchStats();
    fetchActividadReciente();
  }, [tenantId]);

  const fetchStats = async () => {
    try {
      const token = localStorage.getItem('token');
      const backendBase = process.env.REACT_APP_BACKEND_URL || '';
      const res = await fetch(`${backendBase}/hcen-web/api/stats/${tenantId}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (res.ok) {
        const data = await res.json();
        setStats(data);
      }
    } catch (err) {
      console.error('Error fetching stats:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchActividadReciente = async () => {
    try {
      const token = localStorage.getItem('token');
      const backendBase = process.env.REACT_APP_BACKEND_URL || '';
      const res = await fetch(`${backendBase}/hcen-web/api/stats/${tenantId}/actividad-reciente?limite=10`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (res.ok) {
        const data = await res.json();
        setActividades(data);
      }
    } catch (err) {
      console.error('Error fetching actividad reciente:', err);
    } finally {
      setLoadingActividades(false);
    }
  };

  const formatRelativeTime = (fecha) => {
    if (!fecha) return 'Hace un momento';
    
    const ahora = new Date();
    const fechaActividad = new Date(fecha);
    const diffMs = ahora - fechaActividad;
    const diffMinutos = Math.floor(diffMs / 60000);
    const diffHoras = Math.floor(diffMs / 3600000);
    const diffDias = Math.floor(diffMs / 86400000);
    
    if (diffMinutos < 1) {
      return 'Hace un momento';
    } else if (diffMinutos < 60) {
      return `Hace ${diffMinutos} ${diffMinutos === 1 ? 'minuto' : 'minutos'}`;
    } else if (diffHoras < 24) {
      return `Hace ${diffHoras} ${diffHoras === 1 ? 'hora' : 'horas'}`;
    } else if (diffDias === 1) {
      return 'Ayer';
    } else if (diffDias < 7) {
      return `Hace ${diffDias} días`;
    } else {
      return fechaActividad.toLocaleDateString('es-UY', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
      });
    }
  };

  const statCards = [
    {
      title: 'Profesionales',
      value: stats.profesionales,
      icon: '🩺',
      color: '#3b82f6',
      bgColor: '#eff6ff'
    },
    {
      title: 'Usuarios de Salud',
      value: stats.usuarios,
      icon: '👥',
      color: '#10b981',
      bgColor: '#d1fae5'
    },
    {
      title: 'Documentos Clínicos',
      value: stats.documentos,
      icon: '📄',
      color: '#f59e0b',
      bgColor: '#fef3c7'
    },
    {
      title: 'Consultas Hoy',
      value: stats.consultas,
      icon: '📊',
      color: '#8b5cf6',
      bgColor: '#ede9fe'
    }
  ];

  return (
    <div>
      {/* Welcome Banner */}
      <div style={styles.banner}>
        <div>
          <h2 style={styles.bannerTitle}>
            ¡Bienvenido a la Clínica {tenantId}!
          </h2>
          <p style={styles.bannerSubtitle}>
            {new Date().toLocaleDateString('es-UY', {
              weekday: 'long',
              year: 'numeric',
              month: 'long',
              day: 'numeric'
            })}
          </p>
        </div>
        <div style={styles.bannerIcon}>🏥</div>
      </div>

      {/* Stats Grid */}
      <div style={styles.statsGrid}>
        {statCards.map((card, index) => (
          <div key={index} style={{...styles.statCard, borderLeft: `4px solid ${card.color}`}}>
            <div style={{...styles.statIcon, backgroundColor: card.bgColor}}>
              {card.icon}
            </div>
            <div>
              <div style={styles.statValue}>
                {loading ? '...' : card.value}
              </div>
              <div style={styles.statLabel}>{card.title}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Recent Activity */}
      <div style={styles.section}>
        <h3 style={styles.sectionTitle}>📌 Actividad Reciente</h3>
        <div style={styles.activityCard}>
          {loadingActividades ? (
            <div style={styles.loadingText}>Cargando actividad reciente...</div>
          ) : actividades.length === 0 ? (
            <div style={styles.emptyText}>No hay actividad reciente</div>
          ) : (
            actividades.map((actividad, index) => (
              <div key={index} style={styles.activityItem}>
                <div style={styles.activityIcon}>{actividad.icono || '📌'}</div>
                <div>
                  <div 
                    style={styles.activityText}
                    dangerouslySetInnerHTML={{ __html: actividad.texto }}
                  />
                  <div style={styles.activityTime}>
                    {formatRelativeTime(actividad.fecha)}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Quick Actions */}
      <div style={styles.section}>
        <h3 style={styles.sectionTitle}>⚡ Acciones Rápidas</h3>
        <div style={styles.actionsGrid}>
          <button style={styles.actionButton}>
            <span style={styles.actionIcon}>➕</span>
            <span>Agregar Profesional</span>
          </button>
          <button style={styles.actionButton}>
            <span style={styles.actionIcon}>👤</span>
            <span>Registrar Usuario</span>
          </button>
          <button style={styles.actionButton}>
            <span style={styles.actionIcon}>📄</span>
            <span>Nuevo Documento</span>
          </button>
          <button style={styles.actionButton}>
            <span style={styles.actionIcon}>⚙️</span>
            <span>Configuración</span>
          </button>
        </div>
      </div>

      {/* Integration Status */}
      <div style={styles.section}>
        <h3 style={styles.sectionTitle}>🔗 Estado de Integración</h3>
        <div style={styles.integrationCard}>
          <div style={styles.integrationItem}>
            <div style={styles.integrationLabel}>INUS (Índice Nacional de Usuarios)</div>
            <div style={styles.integrationStatus}>
              <span style={styles.statusDot} />
              <span>Conectado</span>
            </div>
          </div>
          <div style={styles.integrationItem}>
            <div style={styles.integrationLabel}>RNDC (Registro Nacional Documentos Clínicos)</div>
            <div style={styles.integrationStatus}>
              <span style={styles.statusDot} />
              <span>Conectado</span>
            </div>
          </div>
          <div style={styles.integrationItem}>
            <div style={styles.integrationLabel}>Políticas de Acceso</div>
            <div style={styles.integrationStatus}>
              <span style={styles.statusDot} />
              <span>Activo</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

const styles = {
  banner: {
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    borderRadius: '12px',
    padding: '32px',
    color: 'white',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '32px'
  },
  bannerTitle: {
    margin: '0 0 8px 0',
    fontSize: '28px',
    fontWeight: '700'
  },
  bannerSubtitle: {
    margin: 0,
    fontSize: '16px',
    opacity: 0.9
  },
  bannerIcon: {
    fontSize: '64px'
  },
  statsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
    gap: '20px',
    marginBottom: '32px'
  },
  statCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '24px',
    display: 'flex',
    alignItems: 'center',
    gap: '20px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  statIcon: {
    width: '56px',
    height: '56px',
    borderRadius: '12px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '28px'
  },
  statValue: {
    fontSize: '32px',
    fontWeight: '700',
    color: '#111827',
    marginBottom: '4px'
  },
  statLabel: {
    fontSize: '14px',
    color: '#6b7280'
  },
  section: {
    marginBottom: '32px'
  },
  sectionTitle: {
    margin: '0 0 16px 0',
    fontSize: '20px',
    fontWeight: '700',
    color: '#111827'
  },
  activityCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '24px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  activityItem: {
    display: 'flex',
    alignItems: 'start',
    gap: '16px',
    padding: '16px 0',
    borderBottom: '1px solid #f3f4f6'
  },
  activityIcon: {
    fontSize: '24px'
  },
  activityText: {
    fontSize: '15px',
    color: '#374151',
    marginBottom: '4px'
  },
  activityTime: {
    fontSize: '13px',
    color: '#9ca3af'
  },
  loadingText: {
    padding: '24px',
    textAlign: 'center',
    color: '#6b7280',
    fontSize: '14px'
  },
  emptyText: {
    padding: '24px',
    textAlign: 'center',
    color: '#9ca3af',
    fontSize: '14px',
    fontStyle: 'italic'
  },
  actionsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
    gap: '16px'
  },
  actionButton: {
    backgroundColor: 'white',
    border: '2px solid #e5e7eb',
    borderRadius: '12px',
    padding: '20px',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    fontSize: '15px',
    fontWeight: '600',
    color: '#374151',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  actionIcon: {
    fontSize: '24px'
  },
  integrationCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '24px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
  },
  integrationItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '16px 0',
    borderBottom: '1px solid #f3f4f6'
  },
  integrationLabel: {
    fontSize: '15px',
    color: '#374151',
    fontWeight: '500'
  },
  integrationStatus: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontSize: '14px',
    color: '#059669',
    fontWeight: '600'
  },
  statusDot: {
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    backgroundColor: '#10b981'
  }
};

export default HomePage;

