/* Canto Compiler and Runtime Engine
 * 
 * SimpleDebugger.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime.debugger;

import canto.lang.Chunk;
import canto.runtime.Context;
import canto.runtime.CantoDebugger;

public class SimpleDebugger implements CantoDebugger {
	
	private SimpleDebuggerFrame frame;
	
	public SimpleDebugger() {}

	public void constructed(Chunk chunk, Context context, Object data) {
	}

	public void getting(Chunk chunk, Context context) {
	}

	public void retrievedFromKeep(String name, Context context, Object data) {
	}

	public void close() {
	}

	public void setFocus() {
	}

}
