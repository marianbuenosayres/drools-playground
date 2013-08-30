package com.plugtree.training;

import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.drools.persistence.jpa.marshaller.JPAPlaceholderResolverStrategy;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;

public class TestObjectMarshallingStrategy extends JPAPlaceholderResolverStrategy {

	private final Environment env;

	public TestObjectMarshallingStrategy(Environment env) {
		super(env);
		this.env = env;
	}

	@Override
	public void write(ObjectOutputStream os, Object object) throws IOException {
		EntityManagerFactory emf = (EntityManagerFactory) env.get(EnvironmentName.ENTITY_MANAGER_FACTORY);
        EntityManager em = emf.createEntityManager();
        if (getClassIdValue(object) == null) {
        	em.persist(object);
        } else {
        	em.merge(object);
        }
		super.write(os, object);
	}
	
	@Override
	public byte[] marshal(Context context, ObjectOutputStream os, Object object)
			throws IOException {
		EntityManagerFactory emf = (EntityManagerFactory) env.get(EnvironmentName.ENTITY_MANAGER_FACTORY);
        EntityManager em = emf.createEntityManager();
        if (getClassIdValue(object) == null) {
        	em.persist(object);
        } else {
        	em.merge(object);
        }
		return super.marshal(context, os, object);
	}
}
