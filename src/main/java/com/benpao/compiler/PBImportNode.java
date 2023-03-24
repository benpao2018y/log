package com.benpao.compiler;

import com.sun.tools.javac.tree.JCTree;

@SuppressWarnings("unused")
public class PBImportNode extends PBInnerNode
{
    protected final JCTree.JCImport asJCImport;
    protected PBImportNode(JCTree.JCImport jcImport, PBJCCompilationUnitCompiler compilationUnitCompiler)
    {
        super(jcImport, compilationUnitCompiler);
        asJCImport = jcImport;
    }

    public String qualidToString(){
        if (asJCImport.qualid != null){
            return asJCImport.qualid.toString();
        }
        return null;
    }

    public boolean isStatic(){
        return asJCImport.isStatic();
    }

    @Override
    protected void parse(JCTree jcTree)
    {

    }
}
