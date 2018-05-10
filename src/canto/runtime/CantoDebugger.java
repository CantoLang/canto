/* Canto Compiler and Runtime Engine
 * 
 * CantoDebugger.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import canto.lang.*;

/**
 * This interface defines a debugging api for Canto.  All of the methods in this
 * interface are callback methods, called by the Canto server before and after 
 * various steps in its operation.  
 */
public interface CantoDebugger {
      public void getting(Chunk chunk, Context context);
      public void constructed(Chunk chunk, Context context, Object data);
      public void retrievedFromKeep(String name, Context context, Object data);

      public void setFocus();
      public void close();
}


