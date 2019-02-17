/* Canto Compiler and Runtime Engine
 * 
 * Exec.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Class for executing OS commands from Canto.
 *
 * @author Michael St. Hippolyte
 */

public class Exec {

    public static Exec execFactory(String command, Map<String, String> envObj, Object dirObj) {
        if (dirObj == null) {
            if (envObj == null) {
                return new Exec(command);
            } else {
                return new Exec(command, envObj);
            }
        } else if (dirObj instanceof CantoFile) {
            return new Exec(command, envObj, (CantoFile) dirObj);

        } else if (dirObj instanceof File) {
            return new Exec(command, envObj, (File) dirObj);
            
        } else {
            return new Exec(command, envObj, new File(dirObj.toString()));
        }
    }
    
	private ProcessRunner processRunner = null;
    private ExecReader normalOut = null;
    private ExecReader errorOut = null;
    private BufferedWriter normalIn = null;
    private int exitVal = -1;
    private Exception execException = null;
    
    private List<String> commands;
    private Map<String, String> env;
    private File dir;
    

    public Exec(String command) {
        this(command, null, (File) null);
    }
    
    public Exec(String command, Map<String, String> env) {
        this(command, env, (File) null);
    }
    
    public Exec(String command, Map<String, String> env, File dir) {
        this.commands = Utils.tokenize(command, null, false);
        this.env = env;
        this.dir = dir;
        exec();
    }

    public Exec(String command, Map<String, String> env, CantoFile dir) {
        this(command, env, dir.toFile());
    }

    private void exec() {
        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            if (env != null && env.size() > 0) {
                pb.environment().putAll(env);
            }
            if (dir != null && dir.exists()) {
                pb.directory(dir);
            }
            Process process = pb.start();
            errorOut = new ExecReader(process.getErrorStream());
            errorOut.start();
            normalOut = new ExecReader(process.getInputStream());
            normalOut.start();
            normalIn = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            processRunner = new ProcessRunner(process);
            processRunner.start();

        } catch (Exception e) {
            execException = e;
        }
    }
    
    public String out() {
        return normalOut == null ? "" : normalOut.out();
    }
    
    public boolean has_out() {
        return normalOut != null && normalOut.hasOutput();
    }

    public String err() {
        return errorOut == null ? "" : errorOut.out();
    }
    
    public boolean has_err() {
        return errorOut != null && errorOut.hasOutput();
    }
    
    public void read_in(String str) throws IOException {
        if (normalIn != null) {
            normalIn.write(str);
        }
    }

    public int exit_val() {
        return exitVal;
    }
    
    public String exception() {
        return execException.toString();
    }
    
    public boolean is_running() {
        if (normalOut != null && normalOut.isRunning()) {
            return true;
        } else if (errorOut != null && errorOut.isRunning()) {
            return true;
        }
        
        return processRunner != null && processRunner.isRunning();
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String cmd: commands) {
            sb.append(cmd);
            sb.append(" ");
        }
        int len = sb.length();
        
        // trim the last space if present
        if (len > 0) {
            sb.setLength(len - 1);
        }
        return sb.toString();
    }
    
    class ProcessRunner extends Thread {
        Process process;
        boolean started = false;
        boolean ended = false;
        
        ProcessRunner(Process process) {
            this.process = process;
        }
        
        public boolean isRunning() {
            return (isAlive() && !ended);
        }
        
        public void run() {
            ended = false;
            started = true;
            try {
                exitVal = process.waitFor();
            } catch (Exception e) {
                execException = e;
            }
            ended = true;
        }
    }
    
    class ExecReader extends Thread {
        InputStream in;
        StringBuffer buf = new StringBuffer();
        boolean started = false;
        boolean ended = false;

        ExecReader(InputStream in) {
            this.in = in;
        }
        
        public boolean isRunning() {
        	return (isAlive() && !ended);
        }
        
        public void run() {
            ended = false;
            started = true;
            try {
                BufferedReader bufferedIn = new BufferedReader( new InputStreamReader(in) ); 
                for (String line = bufferedIn.readLine(); line != null; line = bufferedIn.readLine()) {
                	synchronized (buf) {
                        buf.append(line);
                        buf.append('\n');
                	}
                }
                
            } catch (IOException ioe) {
                buf.append("\nException reading command output: " + ioe.toString());
            }
            ended = true;
        }
        
        public String out() {
            if (buf == null) {
                return null;
            } else {
                String str = buf.toString();
                clear();
                return str;
        	}
        }
        
        public boolean hasOutput() {
            return (buf != null && buf.length() > 0);
        }
        
        public void clear() {
        	synchronized (buf) {
                buf.delete(0, buf.length());
        	}
        }
    }
    
}
