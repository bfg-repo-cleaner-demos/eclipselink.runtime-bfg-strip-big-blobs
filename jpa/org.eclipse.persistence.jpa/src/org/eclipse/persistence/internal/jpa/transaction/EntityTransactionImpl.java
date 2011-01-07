/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.transaction;

import javax.persistence.RollbackException;

import org.eclipse.persistence.exceptions.TransactionException;
import org.eclipse.persistence.internal.jpa.transaction.EntityTransactionWrapper;
import org.eclipse.persistence.internal.localization.ExceptionLocalization;

/**
 * JDK 1.5 version of the EntityTransaction.  Differs from base version only in that
 * it takes a JDK 1.5 version of the EntityTransactionWrapper.
 *
 * @see org.eclipse.persistence.internal.jpa.transaction.EntityTransactionImpl
 */
public class EntityTransactionImpl implements javax.persistence.EntityTransaction {
    protected EntityTransactionWrapper wrapper;
    
    protected boolean active = false;
    
    protected boolean rollbackOnly = false;

    public EntityTransactionImpl(EntityTransactionWrapper wrapper) {
        this.wrapper = wrapper;
    }
    
    /**
     * Start the current transaction. This can only be invoked if {@link #isActive()} returns
     * <code>false</code>.
     * @throws IllegalStateException if isActive() is true.
     */
    public void begin(){
        if (isActive()){
            throw new IllegalStateException(TransactionException.transactionIsActive().getMessage());
        }
        
        // always extended 
        this.wrapper.localUOW = this.wrapper.getEntityManager().getActivePersistenceContext(null);
        this.wrapper.localUOW.setShouldTerminateTransaction(false);
       
        this.active = true;
    }

  /**
   * Commit the current transaction, writing any un-flushed changes to the database.
   * This can only be invoked if {@link #isActive()} returns <code>true</code>.
   * @throws IllegalStateException if isActive() is false.
   * @throws PersistenceException if the commit fails.
   */
  public void commit(){
    try{
      commitInternal();
    }catch (org.eclipse.persistence.exceptions.EclipseLinkException tlException ) {
        //put here to avoid EJB3.0 dependencies in TopLink for jdk 1.4 
      throw new javax.persistence.RollbackException(tlException);
    }
  } 
  
  /**
   * Commit the current transaction, writing any un-flushed changes to the database.
   * This can only be invoked if {@link #isActive()} returns <code>true</code>.
   * @throws IllegalStateException if isActive() is false.
   */
  protected void commitInternal(){
    if (!isActive()){
      throw new IllegalStateException(TransactionException.transactionNotActive().getMessage());
    }
    try {
        if (this.wrapper.localUOW != null){
            this.wrapper.localUOW.setShouldTerminateTransaction(true);
            if (! this.rollbackOnly){
                if (this.wrapper.localUOW.shouldResumeUnitOfWorkOnTransactionCompletion()){
                    this.wrapper.localUOW.commitAndResume();
                    return;
                }else{
                  this.wrapper.localUOW.commit();
                  // all change sets and are cleared, but the cache is kept
                  this.wrapper.localUOW.clearForClose(false);
                }
            } else {
                throw new RollbackException(ExceptionLocalization.buildMessage("rollback_because_of_rollback_only"));
            }
        }
    }catch (RuntimeException ex){
        if (this.wrapper.localUOW != null){
            wrapper.getEntityManager().removeExtendedPersistenceContext();
            this.wrapper.localUOW.release();
            this.wrapper.localUOW.getParent().release();
        }
        if(! this.rollbackOnly) {
            throw new RollbackException(ex);
        } else {
            // it's a RollbackException
            throw ex;
        }
    } finally {
        this.active = false;
        this.rollbackOnly = false;
        wrapper.setLocalUnitOfWork(null);            
    }
  }

  /**
   * Roll back the current transaction, discarding any changes that have happened
   * in this transaction. This can only be invoked if {@link #isActive()} returns
   * <code>true</code>.
   * @throws IllegalStateException if isActive() is false.
   */
  public void rollback(){
    if (!isActive()){
      throw new IllegalStateException(TransactionException.transactionNotActive().getMessage());
    }
    try{
        if (wrapper.getLocalUnitOfWork() != null){
            this.wrapper.localUOW.setShouldTerminateTransaction(true);
            this.wrapper.localUOW.release();
            this.wrapper.localUOW.getParent().release();
        }
    }finally{
        this.active = false;
        this.rollbackOnly = false;
        wrapper.getEntityManager().removeExtendedPersistenceContext();
        wrapper.setLocalUnitOfWork(null);            
    }
  }

  /**
   * Mark the current transaction so that the only possible
   * outcome of the transaction is for the transaction to be
   * rolled back.
   * @throws IllegalStateException if isActive() is false.
   */
  public void setRollbackOnly(){
      if (!isActive()){
          throw new IllegalStateException(TransactionException.transactionNotActive().getMessage());
      }
      
      this.rollbackOnly = true;
  }

/**
 * Here incase a user does not commit or rollback an enityTransaction but just
 * throws it away.  If we do not rollback the txn the connection will go
 * back into the pool.
 */
protected void finalize() throws Throwable{
    try{
        if (isActive())
            this.rollback();
    }finally{
        super.finalize();
    }
}

/**
* Determine whether the current transaction has been marked
* for rollback.
* @throws IllegalStateException if isActive() is false.
*/
public boolean getRollbackOnly(){
    if (!isActive()){
        throw new IllegalStateException(TransactionException.transactionNotActive().getMessage());
    }
    return this.rollbackOnly;
}

    /**
   * Check to see if the current transaction is in progress.
   */
  public boolean isActive(){
      return this.active;
  }
}
