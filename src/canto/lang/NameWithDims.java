/* Canto Compiler and Runtime Engine
 * 
 * NameWithDims.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

/**
 * A NameWithDims is an identifier and one or more dimension descriptors (Dim objects).
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */
public class NameWithDims extends NameWithParams {

    private List<Dim> dims;

    public NameWithDims() {
        super();
    }

    public NameWithDims(String name, List<Dim> dims) {
        super(name, null);
        this.dims = dims;
    }

    public NameWithDims(String name, List<ParameterList> paramLists, List<Dim> dims) {
        super(name, paramLists);
        this.dims = dims;
    }

    public boolean isPrimitive() {
        return false;
    }

    public List<Dim> getDims() {
        return dims;
    }

    protected void setDims(List<Dim> dims) {
        this.dims = dims;
    }
}
