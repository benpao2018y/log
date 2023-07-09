package com.benpao.compiler;

import com.sun.tools.javac.tree.JCTree;

import java.util.*;

@SuppressWarnings("unused")
public abstract class PBInnerNode extends PABUnitCompiler implements PIBInnerClassNodes
{
    public final PBJCCompilationUnitCompiler belongJCCompilationUnitCompiler;
    public final int startPosition;
    public final int endPosition;
    public final int startLineNum;
    public final int endLineNum;
    private final Set<PBClassNode> innerClassNodes = Collections.synchronizedSet(new HashSet<>());
    private final Set<PBClassNode> readOnlyInnerClassNodes = Collections.unmodifiableSet(innerClassNodes);

    protected PBInnerNode(JCTree jcTree,PBJCCompilationUnitCompiler compilationUnitCompiler)
    {
        super(compilationUnitCompiler.compiler, jcTree);

        startPosition = jcTree.getStartPosition();
        startLineNum = compilationUnitCompiler.asJCCompilationUnit.getLineMap()
                .getLineNumber(startPosition);
        endPosition = jcTree.getEndPosition(compilationUnitCompiler.asJCCompilationUnit.endPositions);
        endLineNum = compilationUnitCompiler.asJCCompilationUnit.getLineMap()
                .getLineNumber(endPosition);
        belongJCCompilationUnitCompiler = compilationUnitCompiler;
    }

    private PBInnerNode pbInnerNode;
    public PBInnerNode getParent(){
        if (pbInnerNode == null){
            pbInnerNode = belongJCCompilationUnitCompiler.findParentNode(asJcTree);
        }
        return pbInnerNode;
    }

    protected boolean isNeedEnter()
    {
        return belongJCCompilationUnitCompiler.isNeedEnter();
    }

    protected void addPBInnerNode(PBInnerNode pbInnerNode){
        sons.add(pbInnerNode);
        belongJCCompilationUnitCompiler.addAllInnerNode(pbInnerNode);
        if (pbInnerNode instanceof PBClassNode){
            PBClassNode classNode = (PBClassNode) pbInnerNode;
            innerClassNodes.add(classNode);
            compiler.classNodes.add(classNode);
        }
    }

    @Override
    public Set<PBClassNode> getReadOnlyInnerClassNodes()
    {
        return readOnlyInnerClassNodes;
    }
}
