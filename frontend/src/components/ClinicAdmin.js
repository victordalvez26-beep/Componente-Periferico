import React, { useEffect, useState } from 'react';

function ClinicAdmin(){
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(false);
  const [payload, setPayload] = useState('{ "clinic": "Mi Clinica", "address": "Av. Siempreviva 123" }');
  const backendBase = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080/nodo-periferico';

  useEffect(()=>{ checkSession(); },[]);

  async function checkSession(){
    try{
      const res = await fetch(`${backendBase}/api/auth/session`, { credentials: 'include' });
      if(res.ok){
        const json = await res.json();
        setSession(json);
      } else {
        setSession(null);
      }
    }catch(e){ console.error(e); setSession(null); }
  }

  async function doLogout(){
    await fetch(`${backendBase}/api/auth/logout`, { credentials: 'include' });
    setSession(null);
  }

  async function sendAlta(){
    setLoading(true);
    try{
      const body = { payload };
      const res = await fetch(`${backendBase}/api/clinica/alta`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      if(res.status === 202){
        alert('Alta enviada (202 Accepted).');
      } else {
        const t = await res.text();
        alert('Error: ' + res.status + ' ' + t);
      }
    }catch(e){ console.error(e); alert('Error al enviar'); }
    setLoading(false);
  }

  return (
    <div>
      <div className="card">
        <h2>Sesión</h2>
        {session && session.authenticated ? (
          <div>
            <p>Autenticado como: <strong>{session.nombre || session.username}</strong></p>
            <button className="button-secondary" onClick={doLogout}>Cerrar sesión</button>
          </div>
        ) : (
          <div>
            <p>No autenticado. Para pruebas, inicia sesión contra el endpoint central o usa mocks.</p>
            <a className="link" href={`${backendBase}/auth`}>Abrir login</a>
          </div>
        )}
      </div>

      <div style={{height:12}} />

      <div className="card">
        <h2>Alta de Clínica</h2>
        <div className="form-row">
          <textarea className="input" rows={6} value={payload} onChange={(e)=>setPayload(e.target.value)} />
        </div>
        <div style={{display:'flex', gap:8}}>
          <button className="button-primary" onClick={sendAlta} disabled={loading}>{loading ? 'Enviando...':'Enviar Alta'}</button>
          <button className="button-secondary" onClick={()=>setPayload('{ "clinic": "Mi Clinica", "address": "Av. Siempreviva 123" }')}>Reset</button>
        </div>
      </div>
    </div>
  )
}

export default ClinicAdmin;
