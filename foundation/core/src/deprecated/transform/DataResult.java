/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package deprecated.transform;

import java.util.Vector;
import org.eclipse.persistence.sessions.Project;

/**
 * INTERNAL:
 * <p>
 * <b>Purpose</b>:
 * <p> used by ObjectTransformer.
 */
public interface DataResult {
    public void storeObjects(Project project, Vector objects);
}