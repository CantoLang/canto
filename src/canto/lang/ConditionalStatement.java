/* Canto Compiler and Runtime Engine
 * 
 * ConditionalStatement.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * A ConditionalStatement is an <code>if</code> or <code>with</code> statement,
 * optionally with an <code>else</code> clause.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.15 $
 */
public class ConditionalStatement extends AbstractConstruction implements ConstructionContainer, ConstructionGenerator {

    private ValueSource condition;
    private Block body;
    private Block elseBody;
    private ConditionalStatement elseIf;

    public ConditionalStatement() {
        super();
    }

    public ConditionalStatement(ValueSource condition, Block body, Block elseBody) {
        super();
        setIfElse(condition, body, elseBody);
    }

    public ConditionalStatement(ValueSource condition, Block body, ConditionalStatement elseIf) {
        super();
        setIfElseIf(condition, body, elseIf);
    }

    public boolean isPrimitive() {
        return false;
    }

    public boolean isDynamic() {
        return true;
    }

    public boolean isStatic() {
        return false;
    }

    /** if **/
    protected void setIf(ValueSource condition, Block body) {
        this.condition = condition;
        if (body != null && (body.getNumChildren() > 0 || body.isDynamic())) {
            this.body = body;
        } else {
            this.body = null;
        }
        elseBody = null;
        elseIf = null;
    }

    /** if - else **/
    protected void setIfElse(ValueSource condition, Block body, Block elseBody) {
        this.condition = condition;
        if (body != null && (body.getNumChildren() > 0 || body.isDynamic())) {
            this.body = body;
        } else {
            this.body = null;
        }
        if (elseBody != null && (elseBody.getNumChildren() > 0 || elseBody.isDynamic())) {
            this.elseBody = elseBody;
        } else {
            this.elseBody = null;
        }
        elseIf = null;
    }

    /** if - else if **/
    protected void setIfElseIf(ValueSource condition, Block body, ConditionalStatement elseIf) {
        this.condition = condition;
        if (body != null && (body.getNumChildren() > 0 || body.isDynamic())) {
            this.body = body;
        } else {
            this.body = null;
        }
        elseBody = null;
        this.elseIf = elseIf;
    }

    public Block getBody() {
        return body;
    }

    public CantoNode getElse() {
        if (elseIf == null) {
            return elseBody;
        } else {
            return elseIf;
        }
    }

    public List<Construction> getConstructions(Context context) {
        List<Construction> constructions = Context.newArrayList(2 * TYPICAL_LIST_SIZE, Construction.class);
        if (condition instanceof ConstructionContainer) {
            List<Construction> conditionConstructions = ((ConstructionContainer) condition).getConstructions(context);
            if (conditionConstructions != null) {
                constructions.addAll(conditionConstructions);
            }
        } else if (condition instanceof Construction) {
            constructions.add((Construction) condition);
        }
        if (body != null) {
            List<Construction> bodyConstructions = body.getConstructions(context);
            if (bodyConstructions != null) {
                constructions.addAll(bodyConstructions);
            }
        }
        if (elseBody != null) {
            List<Construction> elseConstructions = elseBody.getConstructions(context);
            if (elseConstructions != null) {
                constructions.addAll(elseConstructions);
            }
        } else if (elseIf != null) {
            List<Construction> elseIfConstructions = elseIf.getConstructions(context);
            if (elseIfConstructions != null) {
                constructions.addAll(elseIfConstructions);
            }
        }
        return constructions;
    }

    public List<Construction> generateConstructions(Context context) throws Redirection {
        List<Construction> constructions = Context.newArrayList(TYPICAL_LIST_SIZE, Construction.class);
        if (booleanValueOf(condition, context)) {
            if (body != null) {
                if (body instanceof ConstructionGenerator) {
                    constructions.addAll(((ConstructionGenerator) body).generateConstructions(context));
                } else {
                    constructions.addAll(body.getConstructions(context));
                }
            }
        } else {
            if (elseBody != null) {
                if (elseBody instanceof ConstructionGenerator) {
                    constructions.addAll(((ConstructionGenerator) elseBody).generateConstructions(context));
                } else {
                    constructions.addAll(elseBody.getConstructions(context));
                }
            } else if (elseIf != null) {
                constructions.addAll(elseIf.generateConstructions(context));
            }
        }
        ((ArrayList<Construction>) constructions).trimToSize();
        return constructions;
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        Object data = null;
        if (valueOf(condition, context).getBoolean()) {
            if (body != null) {
                data = body.getData(context);
            }
        } else {
            if (elseBody != null) {
                data = elseBody.getData(context);
            } else if (elseIf != null) {
                data = elseIf.generateData(context, def);
            }
        }
        return data;
    }

    public Type getType(Context context, boolean generate) {
        if (generate) {
            try {
                if (valueOf(condition, context).getBoolean()) {
                    if (body != null && body.getNumChildren() > 0) {
                        return body.getType(context, generate);
                    } else {
                        return DefaultType.TYPE;                    }
                } else {
                    if (elseBody != null && elseBody.getNumChildren() > 0) {
                        return elseBody.getType(context, generate);
                    } else if (elseIf != null) {
                        return elseIf.getType(context, generate);
                    } else {
                        return DefaultType.TYPE;                    }
                }
            } catch (Redirection r) {
                return DefaultType.TYPE;
            }
        } else {
            if (body == null || body.getNumChildren() == 0 || ((elseBody == null || elseBody.getNumChildren() == 0) && elseIf == null)) {
                return DefaultType.TYPE;
            }
            Type bodyType = body.getType(context, generate);
            Type elseType;
            if (elseBody != null && elseBody.getNumChildren() > 0) {
                elseType = elseBody.getType(context, generate);
            } else if (elseIf != null) {
                elseType = elseIf.getType(context, generate);
            } else {
                return DefaultType.TYPE;
            }
            return findCommonType(context, bodyType, elseType);
        }
    }
    
    private Type findCommonType(Context context, Type type1, Type type2) {
        if (type1 == null || type2 == null) {
            return DefaultType.TYPE;
        } else if (type1.equals(PrimitiveType.VOID) || type2.equals(PrimitiveType.VOID)) {
            return PrimitiveType.VOID;
        } else if (type1.equals(DefaultType.TYPE) || type2.equals(DefaultType.TYPE)) {
            return DefaultType.TYPE;
        } else if (type2.isTypeOf(type1, context)) {
            return type1;
        } else if (type1.isTypeOf(type2, context)) {
            return type2;
        } else {
            return findCommonType(context, type1.getSuper(), type2.getSuper());
        }
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        if (condition instanceof WithPredicate) {
            sb.append("with ");
        } else {
            sb.append("if ");
        }
        sb.append(condition.toString());
        sb.append(' ');
        sb.append(body.toString(prefix));
        if (elseBody != null) {
            sb.append(prefix);
            sb.append("else ");
            sb.append(elseBody.toString(prefix));
        } else if (elseIf != null) {
            sb.append(prefix);
            sb.append("else ");
            sb.append(elseIf.toString(prefix));
        }
        return sb.toString();
    }
}
