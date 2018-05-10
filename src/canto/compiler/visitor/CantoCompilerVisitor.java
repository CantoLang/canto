package canto.compiler.visitor;

import java.util.Iterator;

import canto.lang.AbstractNode;
import canto.lang.CantoNode;

abstract public class CantoCompilerVisitor {
    abstract public void visit( AbstractNode target );
    public boolean done() {
        return false;
    }
    
    public void traverse( AbstractNode n ) {
        traverse( n.getChildren() );
    }
    
    void traverse( Iterator<CantoNode> i ) {
        while ( i.hasNext() ) {
            if ( done() ) {
                return;
            }
            AbstractNode n = (AbstractNode)i.next();
            visit( n );
            traverse( n.getChildren() );
        }
    }
}
