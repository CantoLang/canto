package canto.compiler.visitor;

import canto.lang.AbstractNode;

public class CantoCompilerVisitDestination {
    
    public void visit( CantoCompilerVisitor visitor ) {
        visitor.visit( (AbstractNode)this );
    }

}
