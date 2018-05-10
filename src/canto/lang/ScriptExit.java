/* Canto Compiler and Runtime Engine
 * 
 * ScriptExit.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 *  A ScriptExit is a Redirection thrown by instantiating exit(n).
 *
 **/

public class ScriptExit extends Redirection {
    private static final long serialVersionUID = 1L;

    
    public static void exit(int exitCode, boolean preserveOutput) throws ScriptExit {
        throw new ScriptExit(exitCode, preserveOutput);
    }
    
    private String textOut = "";
    private boolean preserveOutput = false;
    
    public ScriptExit(int exitCode, boolean preserveOutput) {
        super(exitCode, "", "Exiting with code " + exitCode);
        this.preserveOutput = preserveOutput;
    }
    
    public void setTextOut(String textOut) {
        this.textOut = textOut; 
    }

    public String getTextOut() {
    	return textOut;
    }

    public boolean getPreserveOutput() {
    	return preserveOutput;
    }

}


