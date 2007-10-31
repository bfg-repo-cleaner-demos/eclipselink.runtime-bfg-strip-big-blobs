package org.eclipse.persistence.testing.sdo.util;

//import com.sun.tools.javac.Main;

public class CompileUtil {

	private static CompileUtil _instance;
	
	private CompileUtil() {
	}
	
	public static synchronized CompileUtil instance() {
		if (_instance == null) {
			_instance = new CompileUtil();
		}
		return _instance;
	}

	public int compile(String classpath, Object[] javaFiles) {
        String[] args = new String[javaFiles.length + 3];
        args[0] = "javac";
        args[1] = "-cp";
        args[2] = classpath;
        System.arraycopy(javaFiles, 0, args, 3, javaFiles.length);

        int exitVal = -1;

		try {
			Process proc = Runtime.getRuntime().exec(args);
            exitVal = proc.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return exitVal;
	}

    public int compileOld(String classpath, Object[] javaFiles) {
        String[] args = new String[javaFiles.length + 2];
        args[0] = "-cp";
        args[1] = classpath;
        System.arraycopy(javaFiles, 0, args, 2, javaFiles.length);

        //int returnVal = Main.compile(args);
        //return returnVal;
        return -1;
    }
	
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException(); 
	}
	
}
