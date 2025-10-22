// seed-more.js
// Adds three documents for the demo patient (ci = "1.234.567-8"): two public, one restricted.

const docs = [
  {
    _id: "doc-meta-10",
    metadataId: "meta-0010",
    filename: "consulta-no-restringida-1.pdf",
    contentType: "application/pdf",
    size: 11111,
    downloadUrl: "http://example.com/downloads/consulta-no-restringida-1.pdf",
    nodoRUT: "12345678-9",
    pacienteCi: "1.234.567-8",
    restricted: false,
    allowedProfesionales: [],
    createdAt: new Date().toISOString()
  },
  {
    _id: "doc-meta-11",
    metadataId: "meta-0011",
    filename: "consulta-no-restringida-2.pdf",
    contentType: "application/pdf",
    size: 22222,
    downloadUrl: "http://example.com/downloads/consulta-no-restringida-2.pdf",
    nodoRUT: "12345678-9",
    pacienteCi: "1.234.567-8",
    restricted: false,
    allowedProfesionales: [],
    createdAt: new Date().toISOString()
  },
  {
    _id: "doc-meta-12",
    metadataId: "meta-0012",
    filename: "consulta-restringida.pdf",
    contentType: "application/pdf",
    size: 33333,
    downloadUrl: "http://example.com/downloads/consulta-restringida.pdf",
    nodoRUT: "12345678-9",
    pacienteCi: "1.234.567-8",
    restricted: true,
    // allowedProfesionales starts empty -> requiere solicitud/aceptacion
    allowedProfesionales: [],
    createdAt: new Date().toISOString()
  }
];

const db = (typeof db !== 'undefined') ? db.getSiblingDB('hcen_documents_db') : (new Mongo().getDB('hcen_documents_db'));
if (!db.getCollectionNames().includes('documents')) db.createCollection('documents');
const col = db.getCollection('documents');

docs.forEach(d => col.updateOne({ _id: d._id }, { $set: d }, { upsert: true }));
print('Seed-more finished. Count now: ' + col.countDocuments());
