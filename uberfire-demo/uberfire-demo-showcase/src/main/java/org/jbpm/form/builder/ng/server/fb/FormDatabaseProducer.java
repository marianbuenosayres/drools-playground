/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jbpm.form.builder.ng.server.fb;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;

/**
 *
 */

public class FormDatabaseProducer {

    private EntityManagerFactory emf = null;
    
    @PersistenceUnit(unitName = "org.jbpm.form.builder")
    @ApplicationScoped
    @Produces
    public EntityManagerFactory getEmf() {
        if (emf == null) {
            emf = Persistence.createEntityManagerFactory("org.jbpm.form.builder");
        }
        return emf;
    }

    @Produces
    @ApplicationScoped
    public EntityManager getEntityManager() {
        EntityManager em = getEmf().createEntityManager();
        em.getTransaction().begin();
        return em;
    }

    @ApplicationScoped
    public void commitAndClose(@Disposes EntityManager em) {
        try {
            em.getTransaction().commit();
            em.close();
        } catch (Exception e) {

        }
    }
}
