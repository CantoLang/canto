/* Canto Compiler and Runtime Engine
 * 
 * CoreSource.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package cantocore;


/**
 * This is a static convenience class for autoloading core source.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */
public final class CoreSource {
    public static String[] corePaths = { "core.can", "core_ui.can", "core_js.can", "core_platform_java.can", "core_sandbox.can" };

    public static String[] getCorePaths() {
        return corePaths;
    }

}
