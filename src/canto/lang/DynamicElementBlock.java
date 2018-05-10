/* Canto Compiler and Runtime Engine
 * 
 * DynamicElementBlock.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.parser.CantoParser;
import canto.parser.ParseException;
import canto.runtime.Context;

/**
 * A DynamicElementBlock is a CantoBlock which supports the ConstructionGenerator interface.
 * DynamicElementBlocks are used in Canto constructs such as collection comprehensions.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.16 $
 */

public class DynamicElementBlock extends DynamiCantoBlock implements ConstructionGenerator {

    public DynamicElementBlock() {
        super();
    }
    
    protected DynamicElementBlock(DynamiCantoBlock proto, Context context) {
        super(proto, context);
    }

    public Object initForContext(Context context, ArgumentList args, List<Index> indexes) {
        if (isInitialized(context)) {
            return this;
        } else {
            return new DynamicElementBlock(this, context);
        }
    }

    public AbstractNode getContents() {
        return generatedNode;
    }
    
    public String generateCode(Context context) throws Redirection {
        AbstractNode delegatedNode = children[0];
        if (delegatedNode instanceof Chunk) {
            return ((Chunk) delegatedNode).getText(context);
        } else {
            return delegatedNode.toString();
        }
    }

    protected AbstractNode generateNode(CantoParser parser) throws ParseException {
        return parser.generateElementExpression();
    }
    

    /** A DynamicElementBlock represents one or more elements; each construction in the
     *  returned array corresponds to a single element.
     */

    public List<Construction> generateConstructions(Context context)  throws Redirection {
        List<Construction> constructions = Context.newArrayList(TYPICAL_LIST_SIZE, Construction.class);
        int numChildren = (children == null ? 0 : children.length);
        for (int i = 0; i < numChildren; i++) {
            CantoNode node = children[i];
            if (node instanceof TableElement) {
                TableElement tel = (TableElement) node;
                Object element = tel.getElement();
                if (tel.isDynamic() || element instanceof Instantiation) {
                    Value key = tel.isDynamic() ? tel.getDynamicKey(context) : tel.getKey();
                    element = element instanceof Instantiation ? new ResolvedInstance((Instantiation) element, context, false) : element;
                    tel = new TableElement(getOwner(), key, element);
                }
                constructions.add(tel);
            } else if (node instanceof ConstructionGenerator) {
                constructions.addAll(((ConstructionGenerator) node).generateConstructions(context));
            } else if (node instanceof ResolvedInstance) {
                constructions.add((Construction) node);
            } else if (node instanceof Instantiation) {
                constructions.add(new ResolvedInstance((Instantiation) node, context, false));
            } else if (node instanceof Expression) {
                constructions.add(((Expression) node).resolveExpression(context));
            } else if (node instanceof Construction) {
                constructions.add((Construction) node);
            } else if (node instanceof ConstructionContainer) {
                constructions.addAll(((ConstructionContainer) node).getConstructions(context));
            }
        }
        ((ArrayList<Construction>) constructions).trimToSize();
//        if (constructions.size() > 1) {
//            AbstractNode[] nodes = new AbstractNode[constructions.size()];
//            Iterator<Construction> cit = constructions.iterator();
//            int i = 0;
//            while (cit.hasNext()) {
//                Construction c = cit.next();
//                if (c instanceof AbstractNode) {
//                    nodes[i++] = (AbstractNode) c;
//                } else {
//                    nodes[i++] = new PrimitiveValue(c);
//                }
//            }
//            
//            Block block = new CantoBlock(nodes);
//            block.setOwner(getOwner());
//            constructions = new SingleItemList<Construction>(block);
//        }
        return constructions;
    }

}

