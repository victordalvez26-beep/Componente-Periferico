import React from 'react';
import ClinicAdmin from './components/ClinicAdmin';
import './index.css';

export default function App(){
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
  )
}
