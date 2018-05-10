/* Canto Compiler and Runtime Engine
 * 
 * ParsedAnonymousDefinition.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.parser;

import java.util.*;

import canto.lang.*;

/**
 * Based on code generated by jjtree.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */

public class ParsedAnonymousDefinition extends AnonymousDefinition implements Initializable {
    public ParsedAnonymousDefinition(int id) {
        super();
    }


    /** Accept the visitor. **/
    public Object jjtAccept(CantoParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    public void init() {
        int paramlen = children.length - 1;
        if (paramlen > 0) {
            List<ParameterList> paramLists = new ArrayList<ParameterList>(paramlen);
            for (int i = 0; i < paramlen; i++) {
                paramLists.add((ParameterList) children[i]);
            }
            setParamLists(paramLists);
        }
        setContents(children[paramlen]);
    }
}
