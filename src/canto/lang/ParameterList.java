/* Canto Compiler and Runtime Engine
 * 
 * ParameterList.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;
import canto.runtime.CantoVisitor;

/**
 * An ParameterList is a list of parameters.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.28 $
 */
public class ParameterList extends ListNode<DefParameter> {
    
    private boolean dynamic = false;

    public ParameterList() {
        super();
    }

    public ParameterList(List<DefParameter> list) {
        super(list);
    }
    
    protected void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }
    
    /** Returns true if this parameter list is dynamic, meaning that instantiations matching 
     *  this parameter list should always be generated rather than retrieved from the cache.
     **/ 
    public boolean isDynamic() {
        return dynamic;
    }

    /** Returns this definition's signature score for the specified name and argument list.
     *  A lower signature score indicates a better match in type and number of arguments;
     *  a perfect match returns a score of zero.  A score greater than or equal to
     *  NO_MATCH means the definition cannot be used with the specified arguments.
     *
     *  The signature score is calculated as follows:
     *
     *     -- compare each passed argument to the corresponding parameter in the
     *        definition's parameter list.
     *
     *     -- if there is no corresponding parameter, or if the argument is not an
     *        instance of (same class as or subclass of) the corresponding
     *        parameter, set the score to NO_MATCH and abort the calculation.
     *
     *     -- if the parameter type is the default type, add PARAM_DEFAULT to the score.
     *        This number is low enough to score lower than a missing argument and high
     *        enough to score higher than any typed parameter, even if only loosely
     *        matched.
     *
     *     -- if the argument is a subclass of the parameter, add one point for every
     *        subclass between the parameter and the argument.
     *
     *     -- after all arguments have been matched, add ARG_MISSING for each remaining
     *        parameter in the definition's parameter list.  ARG_MISSING is high enough
     *        that any elegible definition with the same number of parameters as arguments
     *        will match ahead of a definition with more parameters than arguments
     *        (definitions with fewer parameters will have already been eliminated in step
     *        two above).
     */
    public int getScore(ArgumentList args, Context argContext, Definition resolver) {
        int numArgs = (args == null ? 0 : args.size());
        int numParams = size();

        if (numArgs > numParams) {
            return Definition.NO_MATCH;
        } else if (numArgs == 0 && numParams == 0) {
            return Definition.PERFECT_MATCH;
        }

        int score = 0;
        for (int i = 0; i < numParams; i++) {
            if (i >= numArgs) {
                score += Definition.ARG_MISSING;
            } else {
                
                // start by looking at the parameter type
                DefParameter param = get(i);
                Type paramType = param.getType();
                if (paramType == null || paramType == DefaultType.TYPE || (paramType.isPrimitive() && paramType.getTypeClass(argContext).equals(String.class))) {
                    score += Definition.PARAM_DEFAULT;
                    continue;
                }

                // see if the argument is a value that gets treated specially (e.g. null)
                Object argObj = args.get(i);
                if (argObj == ArgumentList.MISSING_ARG) {
                    score += Definition.ARG_MISSING;
                    continue;
                } else if (argObj == null || argObj instanceof NullValue) {
                    score += Definition.ARG_NULL;
                    continue;
                }

                // figure out the argument type
                Type argType = null;
                AbstractNode arg = (AbstractNode) argObj;
                if (arg instanceof Instantiation) {
                    Instantiation argInstance = (Instantiation) arg;
                    NameNode argName = argInstance.getReferenceName();
                    if (argInstance.isParameterKind()) {
                        Definition argDef = null;
                        Type argParamType = null;
                        int numUnpushes = 0;
                        int limit = argContext.size() - 1;
                        try {
                            boolean argInContainer = argInstance.isContainerParameter(argContext);
                            if (!argInContainer) {
                                while (numUnpushes < limit) {
                                    if (argContext.peek().paramIsPresent(argName, true)) {
                                        break;
                                    }
                                    argContext.unpush();
                                    numUnpushes++;
                                }
                                if (numUnpushes == limit) {
                                    while (numUnpushes-- > 0) {
                                        argContext.repush();
                                    }
                                    score += Definition.ARG_MISSING;
                                    continue;
                                }
                            }
                            argDef = argContext.getParameterDefinition(argName, argInContainer);
                            argParamType = argContext.getParameterType(argName, argInContainer);

                        } catch (Redirection r) {
                            vlog("Caught while resolving argument " + argInstance.getName() + ": " + r);
                        } finally {
                            while (numUnpushes-- > 0) {
                                argContext.repush();
                            }
                        }
                       
                        if (argDef != null) {
                            argType = argDef.getType();
                            if (!argType.equals(argParamType) && argParamType != null) {
                                vlog("argType is " + argType.getName() + " but argParamType is " + argParamType.getName());
                            }
                        }

                    } else {
                        argType = ((Instantiation) arg).getNarrowType(argContext, resolver);
                    }
                } else if (arg instanceof Construction) {
                    argType = ((Construction) arg).getType(argContext, false);
                } else if (arg instanceof Definition) {
                    argType = ((Definition) arg).getType();
                } else if (arg instanceof PrimitiveValue) {
                    argType = ((PrimitiveValue) arg).getType();
                }

                // finally, compare arg and param types
                score = computeScore(score, paramType, argType, argContext);
                if (score == Definition.NO_MATCH) {
                    break;
                }
            }
        }
        return score;
    }

    private int computeScore(int score, Type paramType, Type argType, Context context) {
        if (paramType.equals(argType)) {
            return score;

        } else if (paramType == null || paramType == DefaultType.TYPE || (paramType.isPrimitive() && paramType.getTypeClass(context).equals(String.class))) {
            return (score + Definition.PARAM_DEFAULT);

        } else if (argType == null || argType.equals(DefaultType.TYPE)) {
            return Definition.NO_MATCH;

        } else if (argType.inheritsCollection() && paramType.getDims().size() > 0) {
            Type argBaseType = argType.getBaseType();
            Type paramBaseType = paramType.getBaseType();
            // if this match increases the score, decrement it so that exactly 
            // matching untyped collections match more closely than a collection and
            // an untyped parameter
            int newScore = computeScore(score, paramBaseType, argBaseType, context);
            if (newScore > score && newScore < Definition.NO_MATCH) {
                newScore--;
            }
            return newScore;

        // this will give an edge to params that are collections
        } else if (argType.inheritsCollection() || paramType.inheritsCollection()) {
             score++;
        }

        int levels = argType.levelsBelow(paramType, context);
        if (levels < 0) {
            return Definition.NO_MATCH;
        } else {
            return (score + levels);
        }
        
    }
    
    /** Returns true if a parameter in this list matches the argument, or if the argument
     *  doesn't need a match (i.e., if it isn't a named construction or an expression that
     *  contains a named construction).
     */
    public boolean matchesArg(Construction arg) {
        if (arg instanceof ValueGenerator) {
            if (arg instanceof Instantiation) {
                String name = ((Instantiation) arg).getName();
                if (name == null || name.length() == 0) {
                    return true;
                }
                Iterator<DefParameter> it = iterator();
                while (it.hasNext()) {
                    DefParameter param = it.next();
                    if (name.equals(param.getName())) {
                        return true;
                    }
                }
                return false;
            } else {
                AbstractNode node = (AbstractNode) arg;
                Iterator<DefParameter> it = iterator();
                while (it.hasNext()) {
                    DefParameter param = it.next();
                    String paramName = param.getReferenceName();
                    if (paramName != null && paramName.length() > 0) {
                        NameChecker checker = new NameChecker(paramName);
                        Object data = node.jjtAccept(checker, "");
                        if (data != null || paramName.equals(data)) {
                            return true;
                        }
                    }
                }
                return false;
            }

        } else {
            return true;
        }
    }

}

class NameChecker extends CantoVisitor {

    private String name;

    public NameChecker(String name) {
        this.name = name;
    }


    /** handleNode will return one of three values: the name being checked, if the node
     *  (or a subnode) contains that name; null, if the node (or a subnode) contains a
     *  different name; or the passed data parameter, if the neither the node nor any
     *  subnode contains a name.
     */
    public Object handleNode(CantoNode node, Object data) {
        if (node instanceof Instantiation) {
            if (name.equals(((Instantiation) node).getName())) {
                return name;
            } else {
                return null;
            }

        } else if (node instanceof Name) {
            if (name.equals(((Name) node).getName())) {
                return name;
            } else {
                return null;
            }
        }
        AbstractNode[] children = ((AbstractNode) node).children;
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                Object childData = children[i].jjtAccept(this, data);
                if (name.equals(childData)) {
                    return name;
                } else if (childData == null) {
                    data = null;
                }
            }
        }
        return data;
    }
}
