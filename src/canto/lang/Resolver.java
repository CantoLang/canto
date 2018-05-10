/* Canto Compiler and Runtime Engine
 * 
 * Resolver.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */


package canto.lang;

import java.util.ArrayList;
import java.util.List;

import canto.runtime.*;

public class Resolver extends CantoVisitor {

    public Resolver() {}
    
    @SuppressWarnings("unchecked")
	public Object handleNode(CantoNode node, Object data) {
        if (node instanceof ForStatement) {
            ParameterList params = ((ForStatement) node).getParameters();
            if (data == null) {
                super.handleNode(node, params);
            } else {
                List<DefParameter> oldList = (List<DefParameter>) data;
                ArrayList<DefParameter> newList = new ArrayList<DefParameter>(oldList.size() + params.size());
                newList.addAll(oldList);
                newList.addAll(params);
                super.handleNode(node, new ParameterList(newList));
            }
            return data;
        } else if (node instanceof Instantiation) {
            ((Instantiation) node).resolve(data);
        } else if (node instanceof Type) {
            ((Type) node).resolve();
        } else if (node instanceof NamedDefinition) {
            // resolve children first
            super.handleNode(node, data);
            ((NamedDefinition) node).resolveKeeps();
            if (node instanceof CollectionDefinition) {
            	((CollectionDefinition) node).resolveDims();
            }
            return data;
        }
        return super.handleNode(node, data);
    }
}
