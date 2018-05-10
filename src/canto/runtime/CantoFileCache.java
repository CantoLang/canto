/* Canto Compiler and Runtime Engine
 * 
 * CantoFileCache.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.io.*;
import java.util.*;

/**
 * A CantoFileCache stores values in the form of Canto source code.
 * 
 * 
 * @author Michael St. Hippolyte
 */

abstract public class CantoFileCache extends AbstractMap {

    File file;
    String siteName;
 
    /** Constructs a file-based persistent cache.
     */
    public CantoFileCache(File file, String siteName) {
    }

}

