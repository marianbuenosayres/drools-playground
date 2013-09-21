package com.plugtree.training.tx;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import org.jboss.seam.transaction.AbstractUserTransaction;
import org.jboss.seam.transaction.DefaultTransaction;
import org.jboss.seam.transaction.Synchronizations;
import org.jboss.solder.core.Veto;

@ApplicationScoped
@DefaultTransaction
@Veto
public class CMTBTMTransaction extends AbstractUserTransaction {

	private BitronixTransactionManager btm;

	@Inject
	public void init(Synchronizations sync) {
		setSynchronizations(sync);
		btm = TransactionManagerServices.getTransactionManager();
	}

	public void begin() throws NotSupportedException, SystemException {
		btm.begin();
		getSynchronizations().afterTransactionBegin();
	}

	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
			IllegalStateException, SystemException {
		boolean success = false;
		Synchronizations synchronizations = getSynchronizations();
		synchronizations.beforeTransactionCommit();
		try {
			btm.commit();
			success = true;
		} finally {
			synchronizations.afterTransactionCompletion(success);
		}
	}

	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		try {
			btm.rollback();
		} finally {
			getSynchronizations().afterTransactionCompletion(false);
		}
	}

	public int getStatus() throws SystemException {
		return btm.getStatus();
	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		btm.setRollbackOnly();
	}

	public void setTransactionTimeout(int timeout) throws SystemException {
		btm.setTransactionTimeout(timeout);
	}

	@Override
	public void registerSynchronization(Synchronization sync) {
		Synchronizations synchronizations = getSynchronizations();
		if (synchronizations.isAwareOfContainerTransactions()) {
			synchronizations.registerSynchronization(sync);
		} else {
			throw new UnsupportedOperationException("cannot register synchronization with container transaction, use <transaction:ejb-transaction/>");
		}
	}
}
