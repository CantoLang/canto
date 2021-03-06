/* Canto Compiler and Runtime Engine
 * 
 * ParsedDefaultStatement.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.parser;

import canto.lang.*;

/**
 * Based on code generated by jjtree.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

public class ParsedDefaultStatement extends Site implements Initializable {
    public ParsedDefaultStatement(int id) {
        super();
    }

    /** Accept the visitor. **/
    public Object jjtAccept(CantoParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    public void init() {
        setName(new NameNode(Name.DEFAULT));
        setContents(children[0]);
    }
}

