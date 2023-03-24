package com.benpao.compiler;

import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Modifier;
import java.util.*;

@SuppressWarnings("unused")
public class PBMethodNode extends PBClassNodeInnerNode implements PIBMethod
{
    public final JCTree.JCMethodDecl asJcMethodDecl;
    public final JCTree.JCBlock asJcBlock;

    private final Set<JCTree.JCVariableDecl> srcParameters = Collections.synchronizedSet(new LinkedHashSet<>());

    private final Set<JCTree.JCVariableDecl> innerParameters = Collections.synchronizedSet(new LinkedHashSet<>());
    private List<String> parameterTypes = null;

    protected PBMethodNode(JCTree.JCMethodDecl methodDecl, PBClassNode classNode)
    {
        super(methodDecl, classNode);
        asJcMethodDecl = methodDecl;
        asJcBlock = null;
    }

    protected PBMethodNode(JCTree.JCBlock jcBlock, PBClassNode classNode)
    {
        super(jcBlock, classNode);
        asJcBlock = jcBlock;
        asJcMethodDecl = null;
    }

    private HashMap<String, PBClass> parameters;

    protected synchronized Map<String, PBClass> getParameters()
    {
        if (parameters == null)
        {
            parameters = new HashMap<>();
            for (JCTree.JCVariableDecl srcParameter : srcParameters)
            {
                parameters.put(srcParameter.getName()
                        .toString(), belongClassNode.parseStrToPBClass(srcParameter.getType()
                        .toString()));
            }
        }
        return parameters;
    }

    protected List<String> getParameterTypes()
    {
        if (parameterTypes == null)
        {
            parameterTypes = new ArrayList<>();
            for (JCTree.JCVariableDecl srcParameter : srcParameters)
            {
                String type = srcParameter.getType()
                        .toString();
                int index = type.indexOf("[");
                int arrayDimension = 0;
                if (index != -1)
                {
                    String typeArray = type.substring(index);
                    type = type.substring(0, index);
                    char[] chars = typeArray.toCharArray();
                    for (char aChar : chars)
                    {
                        if (aChar == '[')
                        {
                            arrayDimension++;
                        }
                    }
                }

                //<editor-fold desc="说明参数不是数组">
                if (arrayDimension == 0)
                {
                    //<editor-fold desc="处理基本类型">
                    if (PBClass.isPrimitiveType(type))
                    {
                        parameterTypes.add(type);
                    }
                    //</editor-fold>
                    else
                    {
                        PBClass pbClass = belongClassNode.parseStrToPBClass(type);
                        if (pbClass != null)
                        {
                            parameterTypes.add(pbClass.getClassName());
                        }
                        else
                        {
                            parameterTypes.add(type);
                        }
                    }
                }
                //</editor-fold>
                else
                {
                    StringBuilder prefix = new StringBuilder();
                    for (int i = 0; i < arrayDimension; i++)
                    {
                        prefix.append("[");
                    }

                    if (PBClass.isPrimitiveType(type))
                    {
                        parameterTypes.add(prefix + PBClass.PRIMITIVE_TYPES.get(type));
                    }
                    else
                    {
                        PBClass pbClass = belongClassNode.parseStrToPBClass(type);
                        if (pbClass != null)
                        {
                            parameterTypes.add(prefix + "L" + pbClass.getClassName() + ";");
                        }
                        else
                        {
                            parameterTypes.add(prefix + "L" + type + ";");
                        }
                    }
                }
            }
        }
        return parameterTypes;
    }

    public synchronized boolean matchParameters(Class<?>... classes)
    {
        List<String> parameterTypes1 = getParameterTypes();
        if (parameterTypes1.size() == classes.length)
        {
            for (int i = 0; i < classes.length; i++)
            {
                if (!classes[i].getName()
                        .equals(parameterTypes1.get(i)))
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * @return 是否为构造方法
     */
    public boolean isConstructor()
    {
        return "<init>".equals(getMethodName());
    }

    /**
     * 在类内部像下面的代码块，理解为静态构造
     * static {
     * ......
     * }
     *
     * @return 是否为静态构造方法
     */
    public boolean isStaticConstructor()
    {
        //noinspection SpellCheckingInspection
        return "<clinit>".equals(getMethodName());
    }

    @Override
    public String getMethodName()
    {
        if (asJcMethodDecl != null)
        {
            return asJcMethodDecl.getName()
                    .toString();
        }
        else if (asJcBlock != null)
        {
            if (asJcBlock.isStatic())
            {
                //noinspection SpellCheckingInspection
                return "<clinit>";
            }
            else
            {
                return "<init>";
            }
        }

        return null;
    }



    @Override
    public String getPath()
    {
        return belongClassNode.getClassName() + "." + getMethodName();
    }

    @Override
    public PBInnerNode getParent()
    {
        return belongClassNode;
    }

    @Override
    protected void parse(JCTree jcTree)
    {
        if (jcTree instanceof JCTree.JCClassDecl)
        {
            PBClassNode classNode = new PBClassNode((JCTree.JCClassDecl) jcTree, belongClassNode);
            addPBInnerNode(classNode);
        }
        else if (jcTree instanceof JCTree.JCVariableDecl)
        {
            JCTree parentJCTree = belongJCCompilationUnitCompiler.findParentJCTree(jcTree);
            if (parentJCTree == asJcMethodDecl)
            {
                JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) jcTree;
                srcParameters.add(variableDecl);
            }


        }
    }

    @Override
    public boolean isPublic()
    {
        if (asJcMethodDecl != null)
        {
            return asJcMethodDecl.getModifiers()
                    .getFlags()
                    .contains(Modifier.PUBLIC);
        }
        return false;
    }

    @Override
    public boolean isProtected()
    {
        if (asJcMethodDecl != null)
        {
            return asJcMethodDecl.getModifiers()
                    .getFlags()
                    .contains(Modifier.PROTECTED);
        }
        return false;
    }

    @Override
    public boolean isPrivate()
    {
        if (asJcMethodDecl != null){
            return asJcMethodDecl.getModifiers()
                    .getFlags()
                    .contains(Modifier.PRIVATE);
        }
        return false;
    }

    @Override
    public boolean isStatic()
    {
        if (asJcMethodDecl != null)
        {
            return asJcMethodDecl.getModifiers()
                    .getFlags()
                    .contains(Modifier.STATIC);
        }
        return isStaticConstructor();
    }

    @Override
    public boolean isDefault()
    {
        if (asJcMethodDecl != null){
            return asJcMethodDecl.getModifiers()
                    .getFlags()
                    .contains(Modifier.DEFAULT);
        }
        return false;
    }
}