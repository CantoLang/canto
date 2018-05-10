package canto.compiler.visitor;

import canto.lang.AbstractNode;
import canto.lang.NullValue;

public class IncompleteDefinitionVisitor extends CantoCompilerVisitor {

    boolean foundIncompleteDefinition = false;

    public void visit( AbstractNode target ) {
        if ( target instanceof NullValue ) {
            NullValue nvTarget = (NullValue)target;
            foundIncompleteDefinition = nvTarget.isAbstract(null);
        }
    }
    
    public boolean isIncomplete() {
        return foundIncompleteDefinition;
    }

}
