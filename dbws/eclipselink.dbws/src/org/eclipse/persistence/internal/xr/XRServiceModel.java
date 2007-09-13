/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/

package org.eclipse.persistence.internal.xr;

// Javase imports
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// Java extension imports

// EclipseLink imports

/**
 * <p><b>INTERNAL</b>: internal DBWS model object allows conformity across APIs
 *
 * @author Mike Norman - michael.norman@oracle.com
 * @since Oracle TopLink 11.x.x
 */
public class XRServiceModel {

  protected String name;
  protected Map<String, Operation> operations = new HashMap<String, Operation>();
  protected String sessionsFile;

  public String getName() {
      return name;
  }
  public void setName(String name) {
      this.name = name;
  }

  public Map<String, Operation> getOperations() {
      return operations;
  }
  public void setOperations(Map<String, Operation> operations) {
      this.operations = operations;
  }
  public Collection<Operation> getOperationsList() {
    return operations.values();
  }
  public Operation getOperation(String operationName) {
    return operations.get(operationName);
  }

  public String getSessionsFile() {
      return sessionsFile;
  }
  public void setSessionsFile(String sessionsFile) {
      this.sessionsFile = sessionsFile;
  }
}