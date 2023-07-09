package com.benpao.compiler;

import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Modifier;


/**
 * 类属性节点
 */
@SuppressWarnings("unused")
public class PBPropertyNode extends PBClassNodeInnerNode implements PIBProperty
{
    public final JCTree.JCVariableDecl asJcVariableDecl;

    protected PBPropertyNode(JCTree.JCVariableDecl variableDecl, PBClassNode classNode)
    {
        super(variableDecl, classNode);
        asJcVariableDecl = variableDecl;
    }

    @Override
    public boolean isPublic() {

        return asJcVariableDecl.getModifiers().getFlags().contains(Modifier.PUBLIC);
    }

    @Override
    public boolean isProtected()
    {
        return asJcVariableDecl.getModifiers().getFlags().contains(Modifier.PROTECTED);
    }

    @Override
    public boolean isPrivate()
    {
        return asJcVariableDecl.getModifiers().getFlags().contains(Modifier.PRIVATE);
    }

    @Override
    public boolean isStatic()
    {
        return asJcVariableDecl.getModifiers().getFlags().contains(Modifier.STATIC);
    }

    @Override
    public String getPath()
    {
        return belongClassNode.getClassName() + "$" + asJcVariableDecl.getName();
    }

    @Override
    public PBInnerNode getParent()
    {
        return belongClassNode;
    }

    @Override
    protected void parse(JCTree jcTree)
    {
        PBInnerNode parentNode = belongJCCompilationUnitCompiler.findParentNode(jcTree);

        if (parentNode == this)
        {
            if (jcTree instanceof JCTree.JCClassDecl)
            {
                PBClassNode classNode = new PBClassNode((JCTree.JCClassDecl) jcTree, belongClassNode);
                addPBInnerNode(classNode);
            }
        }
    }

    @Override
    public String getPropertyName()
    {
        return asJcVariableDecl.getName().toString();
    }

    @Override
    public PBClass getPropertyType()
    {
        return belongClassNode.parseStrToPBClass(asJcVariableDecl.getType().toString());
    }
}