package org.eclipse.persistence.tools.workbench.test;

import org.eclipse.persistence.tools.PackageRenamer;

public class TlRenamer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		PackageRenamer renamer = new PackageRenamer(new String[] {
				"C:/Temp/tlrename.properties",
				"C:/eclipse/workspace/eclipse.mwtest",
				"C:/Temp/output/eclipse.mwtest",
				"C:/Temp/mwtesttlrename.log"});
		renamer.run();

	}

}
