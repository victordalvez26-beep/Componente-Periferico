import React from 'react';
import './SimplePopup.css';

const SimplePopup = ({ message, onClose, type = 'info' }) => {
  if (!message) return null;

  return (
    <div className="simple-popup-overlay" onClick={onClose}>
      <div className="simple-popup-content" onClick={(e) => e.stopPropagation()}>
        <button 
          className="simple-popup-close" 
          onClick={onClose}
          aria-label="Cerrar"
        >
          ×
        </button>
        <div className="simple-popup-message">{message}</div>
        <div className="simple-popup-actions">
          <button className="simple-popup-button" onClick={onClose}>
            Aceptar
          </button>
        </div>
      </div>
    </div>
  );
};

export default SimplePopup;

