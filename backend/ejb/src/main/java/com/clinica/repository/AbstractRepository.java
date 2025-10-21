package com.clinica.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public abstract class AbstractRepository<T> {

    @PersistenceContext(unitName = "nodoPerifericoPersistenceUnit") // coincide con persistence.xml
    protected EntityManager em;

    private Class<T> entityClass;

    public AbstractRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public void save(T entity) {
        em.persist(entity);
    }
    
    public T findById(Object id) {
        return em.find(entityClass, id);
    }
    
    public void delete(T entity) {
        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }

    public T update(T entity) {
        return em.merge(entity);
    }

    
}
