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

  // search term for patients
  const [patientQuery, setPatientQuery] = useState('');

  async function loadPatients(q){
    try{
      // The backend exposes /api/profesional/pacientes/{username}?clinicId=...
      const username = session ? (session.username || 'prof1') : 'prof1';
      const clinic = encodeURIComponent(localStorage.getItem('clinicId') || 'clinic-1');
      const qparam = q ? `&q=${encodeURIComponent(q)}` : '';
      const res = await fetch(`${backendBase}/api/profesional/pacientes/${encodeURIComponent(username)}?clinicId=${clinic}${qparam}`, { credentials: 'include' });
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

  function onSearchPatients(){
    loadPatients(patientQuery);
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

      {/* Simple login-first flow and professional panel */}
      {session && session.authenticated && Array.isArray(session.roles) && session.roles.includes('PROFESIONAL') ? (
        <div className="card">
          <h2>Panel Profesional</h2>
          <div style={{display:'flex', gap:8, alignItems:'center'}}>
            <input className="input" placeholder="Buscar paciente por nombre" value={patientQuery} onChange={(e)=>setPatientQuery(e.target.value)} />
            <button className="button-primary" onClick={onSearchPatients}>Buscar</button>
            <button className="button-secondary" onClick={()=>{ setPatientQuery(''); loadPatients(); }}>Limpiar</button>
          </div>
          <div style={{marginTop:12}}>
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
      ) : null}

      {/* Removed clinic configuration and nodo alta UI — handled in backend for this app */}
    </div>
  )
}

export default ClinicAdmin;
