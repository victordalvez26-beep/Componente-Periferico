// init-mongo.js
// Seed script to create a collection and insert sample document metadata

const documents = [
  {
    _id: "doc-meta-1",
    metadataId: "meta-0001",
    filename: "informe-laboratorio-0001.pdf",
    contentType: "application/pdf",
    size: 234567,
    downloadUrl: "http://example.com/downloads/informe-laboratorio-0001.pdf",
    nodoRUT: "12345678-9",
    createdAt: new Date().toISOString()
  },
  {
    _id: "doc-meta-2",
    metadataId: "meta-0002",
    filename: "radiografia-0002.jpg",
    contentType: "image/jpeg",
    size: 98765,
    downloadUrl: "http://example.com/downloads/radiografia-0002.jpg",
    nodoRUT: "87654321-0",
    createdAt: new Date().toISOString()
  },
  {
    _id: "doc-meta-3",
    metadataId: "meta-0003",
    filename: "resumen-consulta-0003.txt",
    contentType: "text/plain",
    size: 1234,
    downloadUrl: "http://example.com/downloads/resumen-consulta-0003.txt",
    nodoRUT: "12345678-9",
    createdAt: new Date().toISOString()
  }
];

print('Seeding hcen_documents_db.documents with sample metadata...');

// When executed via mongosh against a host, use getSiblingDB to select the target DB
const db = (typeof db !== 'undefined') ? db.getSiblingDB('hcen_documents_db') : (new Mongo().getDB('hcen_documents_db'));

// create collection if not exists
if (!db.getCollectionNames().includes('documents')) {
  db.createCollection('documents');
}

const col = db.getCollection('documents');

// upsert sample documents
documents.forEach(doc => {
  col.updateOne({ _id: doc._id }, { $set: doc }, { upsert: true });
});

print('Seeding finished. Documents in collection: ' + col.countDocuments());
