import React from 'react';
import './SimplePopup.css';

const SimplePopup = ({ message, onClose, type = 'info' }) => {
  if (!message) return null;

  // Asegurar que el mensaje sea un string (no un objeto JSON)
  let messageText = message;
  if (typeof message === 'object') {
    // Si es un objeto, intentar extraer el mensaje
    if (message.mensaje && typeof message.mensaje === 'string') {
      messageText = message.mensaje;
    } else if (message.error && typeof message.error === 'string') {
      messageText = message.error;
    } else {
      // Si no se puede extraer, convertir a string (no ideal, pero mejor que mostrar [object Object])
      messageText = JSON.stringify(message);
    }
  } else if (typeof message !== 'string') {
    messageText = String(message);
  }

  // Determinar si es un mensaje de "ya tiene acceso"
  const isAccessMessage = messageText && (
    messageText.includes('Ya tiene acceso') || 
    messageText.includes('ya tiene acceso') ||
    messageText.includes('No es necesario solicitar')
  );

  return (
    <div className="simple-popup-overlay" onClick={onClose}>
      <div className={`simple-popup-content ${isAccessMessage ? 'simple-popup-info' : ''}`} onClick={(e) => e.stopPropagation()}>
        <button 
          className="simple-popup-close" 
          onClick={onClose}
          aria-label="Cerrar"
        >
          ×
        </button>
        {isAccessMessage && (
          <div className="simple-popup-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="12" cy="12" r="10" stroke="#3b82f6" strokeWidth="2" fill="none"/>
              <path d="M9 12l2 2 4-4" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
        )}
        <div className={`simple-popup-message ${isAccessMessage ? 'simple-popup-message-info' : ''}`}>
          {messageText}
        </div>
        <div className="simple-popup-actions">
          <button className="simple-popup-button" onClick={onClose}>
            Entendido
          </button>
        </div>
      </div>
    </div>
  );
};

export default SimplePopup;

