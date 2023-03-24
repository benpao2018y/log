package com.benpao.compiler;

import com.sun.tools.javac.tree.JCTree;

@SuppressWarnings("unused")
public abstract class PBClassNodeInnerNode extends PBInnerNode
{
    protected PBClassNode belongClassNode;

    protected PBClassNodeInnerNode(JCTree jcTree, PBClassNode classNode)
    {
        super(jcTree, classNode.belongJCCompilationUnitCompiler);
        belongClassNode = classNode;
    }
    public abstract String getPath();

    @Override
    protected void parse(JCTree jcTree)
    {

    }
}
