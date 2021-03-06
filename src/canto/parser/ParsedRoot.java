/* Canto Compiler and Runtime Engine
 * 
 * ParsedRoot.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */


package canto.parser;


/**
 * Based on code generated by jjtree.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */
public class ParsedRoot extends ParsedNode {
    public ParsedRoot(int id) {
        super();
    }

    /** Accept the visitor. **/
    public Object jjtAccept(CantoParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
