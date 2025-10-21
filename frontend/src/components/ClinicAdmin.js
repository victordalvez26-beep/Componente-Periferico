import React, { useEffect, useState } from 'react';

function ClinicAdmin(){
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(false);
  const [payload, setPayload] = useState('{ "clinic": "Mi Clinica", "address": "Av. Siempreviva 123" }');
  const backendBase = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080/nodo-periferico';

  // login form state
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin');

  // nodo status
  const [nodoId, setNodoId] = useState('1');
  const [nodoInfo, setNodoInfo] = useState(null);
  const [nodoLoading, setNodoLoading] = useState(false);
  // multitenant: clinic id configured in this instance
  const [clinicId, setClinicId] = useState(localStorage.getItem('clinicId') || 'clinic-1');

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

  async function doLogin(e){
    e && e.preventDefault();
    try{
      const res = await fetch(`${backendBase}/api/auth/login`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type':'application/json' },
        body: JSON.stringify({ username, password })
      });
      if(res.ok){
        const j = await res.json();
        setSession({ authenticated: true, nombre: j.nombre || j.username, username: j.username });
        alert('Login OK como ' + (j.nombre || j.username));
      } else if (res.status === 401) {
        alert('Credenciales inválidas');
      } else {
        const txt = await res.text().catch(()=>null);
        alert('Error al loguear: ' + res.status + ' ' + txt);
      }
    }catch(err){ console.error(err); alert('Error al hacer login'); }
  }

  async function fetchNodo(){
    if(!nodoId) return;
    setNodoLoading(true);
    try{
      const res = await fetch(`${backendBase}/api/nodos/${encodeURIComponent(nodoId)}`, { credentials: 'include' });
      if(res.ok){
        const j = await res.json();
        // show node only if matches clinicId (simple multitenant check); assume nodo DTO has 'clinicId'
        if (j.clinicId && j.clinicId !== clinicId) {
          setNodoInfo({ notOwned: true });
        } else {
          setNodoInfo(j);
        }
      } else if (res.status === 404){
        setNodoInfo({ notFound: true });
      } else {
        const t = await res.text().catch(()=>null);
        setNodoInfo({ error: `${res.status} ${t}` });
      }
    }catch(e){ console.error(e); setNodoInfo({ error: e.message }); }
    setNodoLoading(false);
  }

  // test-pdf download removed — central/Nodo debe exponer la descarga

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
            <p>No autenticado. Para pruebas, use el login de test a continuación (usuario: <code>admin</code> / pass: <code>admin</code>).</p>
            <form onSubmit={doLogin} style={{display:'flex', gap:8, alignItems:'center'}}>
              <input className="input" value={username} onChange={(e)=>setUsername(e.target.value)} />
              <input className="input" type="password" value={password} onChange={(e)=>setPassword(e.target.value)} />
              <button className="button-primary" type="submit">Login (test)</button>
            </form>
          </div>
        )}
      </div>

      <div style={{height:12}} />

      <div className="card">
        <h3>Configuración de esta Clínica (multitenant)</h3>
        <div className="form-row">
          <input className="input" value={clinicId} onChange={(e)=>setClinicId(e.target.value)} />
          <button className="button-secondary" onClick={()=>{ localStorage.setItem('clinicId', clinicId); alert('clinicId guardado: ' + clinicId); }}>Guardar</button>
        </div>

        <h2>Estado de Activación (Nodo)</h2>
        <div className="form-row">
          <input className="input" value={nodoId} onChange={(e)=>setNodoId(e.target.value)} />
          <button className="button-primary" onClick={fetchNodo} disabled={nodoLoading}>Consultar</button>
        </div>
        <div>
          {nodoLoading && <div>Cargando nodo...</div>}
          {nodoInfo && nodoInfo.notFound && <div style={{color:'#b33'}}>Nodo no encontrado</div>}
          {nodoInfo && nodoInfo.notOwned && <div style={{color:'#b33'}}>Este nodo no pertenece a esta clínica (multitenant)</div>}
          {nodoInfo && nodoInfo.error && <div style={{color:'#b33'}}>Error: {nodoInfo.error}</div>}
          {nodoInfo && !nodoInfo.notFound && !nodoInfo.error && !nodoInfo.notOwned && (
            <div>
              <p><strong>Id:</strong> {nodoInfo.id}</p>
              <p><strong>Nombre:</strong> {nodoInfo.nombre}</p>
              <p><strong>Estado:</strong> <span style={{fontWeight:700, color: nodoInfo.estado === 'ACTIVO' ? '#28a745' : (nodoInfo.estado === 'PENDIENTE' ? '#dc9a00' : '#dc3545')}}>{nodoInfo.estado}</span></p>
            </div>
          )}
        </div>
      </div>

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
