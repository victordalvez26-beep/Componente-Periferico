import React, { useEffect, useState } from 'react';

function ClinicAdmin(){
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(false);
  const [payload, setPayload] = useState('{ "clinic": "Mi Clinica", "address": "Av. Siempreviva 123" }');
  // Use environment override if provided; otherwise use relative path so CRA proxy (package.json -> proxy)
  // or production reverse proxy can route correctly. This avoids hardcoded port 8080.
  const backendBase = process.env.REACT_APP_BACKEND_URL || '/nodo-periferico';

  // login form state
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin');

  // nodo status
  const [nodoId, setNodoId] = useState('1');
  const [nodoInfo, setNodoInfo] = useState(null);
  const [nodoLoading, setNodoLoading] = useState(false);
  // professional workflow
  const [patients, setPatients] = useState([]);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [patientDocs, setPatientDocs] = useState([]);
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

  // if logged in as PROFESSIONAL, load patients
  useEffect(()=>{
    if(session && session.authenticated && Array.isArray(session.roles) && session.roles.includes('PROFESIONAL')){
      loadPatients();
    }
  }, [session]);

  async function loadPatients(){
    try{
      // The backend exposes /api/profesional/pacientes/{username}?clinicId=...
      const username = session ? (session.username || 'prof1') : 'prof1';
      const res = await fetch(`${backendBase}/api/profesional/pacientes/${encodeURIComponent(username)}?clinicId=${encodeURIComponent(localStorage.getItem('clinicId') || 'clinic-1')}`, { credentials: 'include' });
      if(res.ok){
        const j = await res.json();
        setPatients(Array.isArray(j) ? j : []);
      }
    }catch(e){ console.error('Error loading patients', e); }
  }

  async function selectPatient(p){
    setSelectedPatient(p);
    setPatientDocs([]);
    try{
      // call backend proxy that will call RNDC and politicas
      const profId = session ? session.username : null;
      const res = await fetch(`${backendBase}/api/profesional/paciente/${encodeURIComponent(p.ci)}/documentos?profesionalId=${encodeURIComponent(profId)}`, { credentials: 'include' });
      if(res.ok){
        const j = await res.json();
        setPatientDocs(Array.isArray(j) ? j : []);
      } else {
        const t = await res.text().catch(()=>null);
        alert('Error cargando documentos: ' + res.status + ' ' + t);
      }
    }catch(e){ console.error('Error fetching docs', e); alert('Error al cargar documentos'); }
  }

  async function solicitarAcceso(doc){
    try{
      const body = {
        codDocumPaciente: selectedPatient.ci,
        tipoDocumento: doc.tipoDocumento,
        profesionalSolicitante: session ? session.username : 'prof1'
      };
      const res = await fetch(`${backendBase}/api/profesional/solicitudes`, {
        method: 'POST', credentials: 'include', headers: {'Content-Type':'application/json'}, body: JSON.stringify(body)
      });
      if(res.ok) alert('Solicitud enviada'); else { const t = await res.text().catch(()=>null); alert('Error: '+res.status+' '+t); }
    }catch(e){ console.error(e); alert('Error al enviar solicitud'); }
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

      {/* Professional panel */}
      {session && session.authenticated && Array.isArray(session.roles) && session.roles.includes('PROFESIONAL') && (
        <div className="card">
          <h2>Panel Profesional</h2>
          <div>
            <h3>Pacientes</h3>
            {patients.length === 0 ? <div>No hay pacientes</div> : (
              <ul>
                {patients.map((p, i) => (
                  <li key={i}>
                    {p.nombre} ({p.ci}) <button className="button-secondary" onClick={()=>selectPatient(p)}>Ver documentos</button>
                  </li>
                ))}
              </ul>
            )}

            {selectedPatient && (
              <div style={{marginTop:12}}>
                <h4>Documentos de {selectedPatient.nombre} ({selectedPatient.ci})</h4>
                {patientDocs.length === 0 ? <div>No se encontraron documentos</div> : (
                  <ul>
                    {patientDocs.map((d,idx) => (
                      <li key={idx}>
                        {d.tipoDocumento} - {d.formatoDocumento}
                        <div style={{display:'inline-block', marginLeft:8}}>
                          <a className="link" href={d.uriDocumento || '#'} target="_blank" rel="noreferrer">Ver/Descargar</a>
                          <button className="button-secondary" style={{marginLeft:8}} onClick={()=>solicitarAcceso(d)}>Solicitar acceso</button>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </div>
        </div>
      )}

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
