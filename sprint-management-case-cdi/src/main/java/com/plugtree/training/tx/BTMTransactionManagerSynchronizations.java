package com.plugtree.training.tx;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.transaction.TransactionManager;

import org.jboss.seam.transaction.TransactionManagerSynchronizations;

import bitronix.tm.TransactionManagerServices;

@ApplicationScoped
@Alternative
public class BTMTransactionManagerSynchronizations extends TransactionManagerSynchronizations{

  @Override
  public TransactionManager getTransactionManager() {
    return TransactionManagerServices.getTransactionManager();
  }
  
}
