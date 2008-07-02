/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
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
package org.eclipse.persistence.tools.workbench.test.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import junit.framework.TestCase;
import org.eclipse.persistence.tools.workbench.mappingsio.ProjectIOManager;
import org.eclipse.persistence.tools.workbench.mappingsmodel.project.MWProject;
import org.eclipse.persistence.tools.workbench.utility.io.FileTools;
/**
 * Test the Ant extension project runners.
 * Generates an Employee project in resource, tests the project validation routine
 * exports the deployment descriptor for the generated MWP.
 * resource/ant/build.xml will perform the real Ant extension test, and will
 * rely on the MWP project and on the TopLink deployment descriptor XML 
 * generated by this test suite.
 */
public abstract class ProjectRunnerTests extends TestCase {
    protected File tempDir;
    protected String projectFileName;
    protected PrintStream log;
	private FileOutputStream logFile;
	
	public final static String MW = "mw";
	
	public ProjectRunnerTests( String name) {
        super( name);
    }
	/**
	 * Setup the temp directory in resource.
	 * Generates a MWP.
	 */
	protected void setUp() throws Exception {
		super.setUp();

		File resourceDir = FileTools.resourceFile( "/ant");
		this.tempDir = emptyTemporaryDirectory( resourceDir.getAbsolutePath() + "/temp");
		this.logFile =  new FileOutputStream( tempDir.getAbsolutePath() + "/log.txt");
		this.log = new PrintStream( logFile);
		
		MWProject project =  this.buildProject();
		this.postBuildProject( project);
		projectFileName= this.buildAndWriteProject( project);
	}

	protected void tearDown() throws Exception {
		super.tearDown();

		this.log.close();
		this.logFile.close();
	}
	/**
	 * Generates a MWP.
	 */
	private String buildAndWriteProject( MWProject project) throws Exception {

		project.setSaveDirectory( new File( this.tempDir, MW));
		
		new ProjectIOManager().write( project);
		
		return project.saveFile().getAbsolutePath();
	}
	
	protected abstract MWProject buildProject() throws Exception;

	/**
	 * Post building MW project.
	 */
	protected abstract void postBuildProject( MWProject project);
	
	private File emptyTemporaryDirectory( String baseDir) {
		File dir = new File( baseDir);
		if( dir.exists())
		    FileTools.deleteDirectoryContents( dir);
		else
			dir.mkdirs();
		return dir;
	}
}
