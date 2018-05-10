/* Canto Compiler and Runtime Engine
 * 
 * NameWithParams.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Iterator;
import java.util.List;

/**
 * A NameWithParams is an identifier and an associated list of paramter lists.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */
public class NameWithParams extends NameNode {

    private List<ParameterList> paramLists;

    public NameWithParams() {
        super();
    }

    public NameWithParams(String name, List<ParameterList> paramLists) {
        super(name);
        setParamLists(paramLists);
    }

    /** Returns <code>false</code> */
    public boolean isPrimitive() {
        return false;
    }

    public int getNumParamLists() {
        if (paramLists == null) {
            return 0;
        } else {
            return paramLists.size();
        }
    }

    public List<ParameterList> getParamLists() {
        return paramLists;
    }

    public void setParamLists(List<ParameterList> paramLists) {
        this.paramLists = paramLists;
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(super.getName());
        if (paramLists != null) {
            Iterator<ParameterList> it = paramLists.iterator();
            while (it.hasNext()) {
                AbstractNode node = (AbstractNode) it.next();
                sb.append(node.toString());
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
        }
        return sb.toString();
    }
}
