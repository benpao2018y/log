package com.benpao.compiler;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对应每一个独立的Java类文件，其中可能包含多个类
 */
@SuppressWarnings({
        "DuplicatedCode", "unused"
})
public class PBJCCompilationUnitCompiler extends PABUnitCompiler implements PIBInnerClassNodes
{
    public static class JCTreePosition
    {
        public final JCTree jcTree;
        public final int startPosition;
        public final int endPosition;

        public final int startLineNum;

        public final int endLineNum;

        protected JCTreePosition(JCTree jcTree, JCTree.JCCompilationUnit compilationUnit)
        {
            this.jcTree = jcTree;
            this.startPosition = jcTree.getStartPosition();
            this.endPosition = jcTree.getEndPosition(compilationUnit.endPositions);
            startLineNum = compilationUnit.getLineMap().getLineNumber(startPosition);
            endLineNum = compilationUnit.getLineMap().getLineNumber(endPosition);
        }
    }

    public static class VarUsageRange
    {
        public final JCTree.JCVariableDecl asJcVariableDecl;
        public final int startPosition;
        public final int endPosition;
        public final int startLineNum;
        public final int endLineNum;

        VarUsageRange(JCTree.JCVariableDecl asJcVariableDecl, int startPosition, int endPosition, int startLineNum, int endLineNum)
        {
            this.asJcVariableDecl = asJcVariableDecl;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.startLineNum = startLineNum;
            this.endLineNum = endLineNum;
        }
    }

    private final Map<JCTree.JCVariableDecl, VarUsageRange> usageRanges = new ConcurrentHashMap<>();
    public final JCTree.JCCompilationUnit asJCCompilationUnit;
    private final Set<PBClassNode> rootClasses = Collections.synchronizedSet(new HashSet<>());
    private final Set<PBClassNode> readOnlyInnerClassNodes = Collections.unmodifiableSet(rootClasses);
    protected final List<JCTree.JCNewClass> anonymousClassNewClass = Collections.synchronizedList(new ArrayList<>());

    protected final List<JCTreePosition> jcTreePositions = Collections.synchronizedList(new ArrayList<>());

    protected final List<PBInnerNode> allInnerNodes = Collections.synchronizedList(new ArrayList<>());

    protected boolean isRead = false;
    private final boolean needEnter;

    public PBJCCompilationUnitCompiler(PABCompiler compiler, JCTree.JCCompilationUnit compilationUnit)
    {
        this(compiler, compilationUnit, true);
    }

    public PBJCCompilationUnitCompiler(PABCompiler compiler, JCTree.JCCompilationUnit compilationUnit, boolean needEnter)
    {
        super(compiler, compilationUnit);
        this.asJCCompilationUnit = compilationUnit;
        this.needEnter = needEnter;
    }

    public Map<JCTree.JCVariableDecl, VarUsageRange> getUsageRanges()
    {
        return usageRanges;
    }

    protected boolean isNeedEnter()
    {
        return needEnter;
    }

    @Override
    public Set<PBClassNode> getReadOnlyInnerClassNodes()
    {
        return readOnlyInnerClassNodes;
    }

    public JCTree findParentJCTree(JCTree jcTree)
    {
        JCTreePosition jcTreePosition = new JCTreePosition(jcTree, asJCCompilationUnit);
        boolean exit = false;
        JCTreePosition mayBeParent = null;

        for (JCTreePosition position : jcTreePositions)
        {
            if (position.jcTree == jcTree)
            {
                exit = true;
            }
            else if (position.startPosition <= jcTreePosition.startPosition && position.endPosition >= jcTreePosition.endPosition)
            {
                if (mayBeParent == null)
                {
                    mayBeParent = position;
                }
                else if (position.startPosition >= mayBeParent.startPosition && position.endPosition <= mayBeParent.endPosition)
                {
                    mayBeParent = position;
                }
            }
        }
        if (!exit)
        {
            return null;
        }

        if (mayBeParent != null)
        {
            return mayBeParent.jcTree;
        }
        return asJCCompilationUnit;
    }

    public JCTreePosition getJCTreePosition(JCTree jcTree)
    {
        for (JCTreePosition jcTreePosition : jcTreePositions)
        {
            if (jcTreePosition.jcTree == jcTree)
            {
                return jcTreePosition;
            }
        }
        return null;
    }

    private final Map<JCTree, PBInnerNode> findParentNodeCache = new ConcurrentHashMap<>();

    /**
     * 这个是查找最近一层的父节点，在语法树上也许不是最近的一层，因为没有对所有的语法树元素进行节点包装
     *
     * @param jcTree jcTree
     * @return 最近一层父节点
     */
    public PBInnerNode findParentNode(JCTree jcTree)
    {
        PBInnerNode pbInnerNode = findParentNodeCache.get(jcTree);
        if (pbInnerNode != null)
        {
            return pbInnerNode;
        }

        JCTreePosition jcTreePosition = new JCTreePosition(jcTree, asJCCompilationUnit);
        boolean exit = false;
        for (JCTreePosition position : jcTreePositions)
        {
            if (position.jcTree == jcTree)
            {
                exit = true;
                break;
            }
        }
        if (!exit)
        {
            return null;
        }

        PBInnerNode mayBeParent = null;
        for (PBInnerNode innerNode : allInnerNodes)
        {

            if (innerNode.asJcTree == jcTree)
            {
                continue;
            }
            if (innerNode.startPosition <= jcTreePosition.startPosition && innerNode.endPosition >= jcTreePosition.endPosition)
            {
                if (mayBeParent == null)
                {
                    mayBeParent = innerNode;
                }
                else if (mayBeParent.startPosition <= innerNode.startPosition && mayBeParent.endPosition >= innerNode.endPosition)
                {
                    mayBeParent = innerNode;
                }
            }
        }
        if (mayBeParent != null)
        {
            findParentNodeCache.put(jcTree, mayBeParent);
        }
        return mayBeParent;
    }

    private final Map<JCTree, PBClassNode> findParentClassNodeCache = new ConcurrentHashMap<>();

    public PBClassNode findParentClassNode(JCTree jcTree)
    {
        PBClassNode classNode = findParentClassNodeCache.get(jcTree);
        if (classNode != null)
        {
            return classNode;
        }
        PBInnerNode parentNode = findParentNode(jcTree);
        while (parentNode != null && !(parentNode instanceof PBClassNode))
        {
            parentNode = parentNode.getParent();
        }
        classNode = (PBClassNode) parentNode;
        if (classNode != null)
        {
            findParentClassNodeCache.put(jcTree, classNode);
        }
        return classNode;
    }

    private final Map<JCTree, PBMethodNode> findParentMethodNodeCache = new ConcurrentHashMap<>();

    public PBMethodNode findParentMethodNode(JCTree jcTree)
    {
        PBMethodNode pbMethodNode = findParentMethodNodeCache.get(jcTree);
        if (pbMethodNode != null)
        {
            return pbMethodNode;
        }
        PBInnerNode parentNode = findParentNode(jcTree);
        while (parentNode != null && !(parentNode instanceof PBMethodNode))
        {
            parentNode = parentNode.getParent();
        }
        pbMethodNode = (PBMethodNode) parentNode;
        if (pbMethodNode != null)
        {
            findParentMethodNodeCache.put(jcTree, pbMethodNode);
        }
        return pbMethodNode;
    }

    public VarUsageRange findCanUseVarByName(String varName, JCTree jcTree)
    {
        JCTreePosition jcTreePosition = getJCTreePosition(jcTree);
        if (jcTreePosition == null)
        {
            return null;
        }
        VarUsageRange varUsageRange = null;

        for (VarUsageRange value : usageRanges.values())
        {
            if (!value.asJcVariableDecl.getName().toString().equals(varName))
            {
                continue;
            }
            if (value.startPosition <= jcTreePosition.startPosition && value.endPosition >= jcTreePosition.endPosition)
            {
                if (varUsageRange == null)
                {
                    varUsageRange = value;
                }
                else if (value.startPosition >= varUsageRange.startPosition)
                {
                    varUsageRange = value;
                }
            }
        }
        return varUsageRange;
    }

    private List<PBImportNode> pbImportNodes;

    public List<PBImportNode> getImports()
    {
        if (pbImportNodes == null)
        {
            com.sun.tools.javac.util.List<JCTree.JCImport> imports = asJCCompilationUnit.getImports();
            pbImportNodes = new ArrayList<>();
            for (JCTree.JCImport anImport : imports)
            {
                if (anImport.qualid != null && !anImport.isStatic())
                {
                    pbImportNodes.add(new PBImportNode(anImport, this));
                }
            }
        }
        return pbImportNodes;
    }

    private List<PBImportNode> staticPBImportNodes;

    public List<PBImportNode> getStaticImports()
    {
        if (staticPBImportNodes == null)
        {
            com.sun.tools.javac.util.List<JCTree.JCImport> imports = asJCCompilationUnit.getImports();
            staticPBImportNodes = new ArrayList<>();
            for (JCTree.JCImport anImport : imports)
            {
                if (anImport.qualid != null && anImport.isStatic())
                {
                    staticPBImportNodes.add(new PBImportNode(anImport, this));
                }
            }
        }
        return staticPBImportNodes;
    }

    protected String getPackageName()
    {
        return asJCCompilationUnit.getPackageName().toString();
    }

    public String getJavaFileName()
    {
        String name = asJCCompilationUnit.getSourceFile().getName().replace("\\", "/");
        int i = name.lastIndexOf("/");
        name = name.substring(i + 1);
        return name;
    }

    /**
     * @param jcTree jcTree
     * @return 该节点是否可以包含局部变量
     */
    public boolean isCanHaveLocalVariable(JCTree jcTree)
    {
        if (jcTree instanceof JCTree.JCBlock)
        {
            return true;
        }
        if (jcTree instanceof JCTree.JCForLoop)
        {
            return true;
        }
        if (jcTree instanceof JCTree.JCLambda)
        {
            return true;
        }

        //noinspection RedundantIfStatement
        if (jcTree instanceof JCTree.JCTry)
        {
            return true;
        }


        return false;
    }

    @Override
    protected void parse(JCTree jcTree)
    {
        JCTreePosition jcTreePosition = new JCTreePosition(jcTree, asJCCompilationUnit);
        jcTreePositions.add(jcTreePosition);
        compiler.jcTreeMap.put(jcTree, this);
        if (jcTree instanceof JCTree.JCClassDecl)
        {
            JCTree parent = findParentJCTree(jcTree);
            if (parent == asJCCompilationUnit)
            {
                PBClassNode classNode = new PBClassNode((JCTree.JCClassDecl) jcTree, this);
                compiler.classNodes.add(classNode);
                rootClasses.add(classNode);
                sons.add(classNode);
                allInnerNodes.add(classNode);
            }
        }
    }

    @Override
    protected void afterParse()
    {
        asJcTree.accept(new TreeTranslator()
        {
            @Override
            public void visitVarDef(JCTree.JCVariableDecl variableDecl)
            {
                if (variableDecl.getType() != null)
                {
                    JCTreePosition jcTreePosition = getJCTreePosition(variableDecl);
                    PBInnerNode parentNode = findParentNode(variableDecl);

                    if (parentNode instanceof PBClassNode)
                    {
                        usageRanges.put(variableDecl,
                                new VarUsageRange(variableDecl, parentNode.startPosition, parentNode.endPosition,
                                        parentNode.startLineNum, parentNode.endLineNum));
                    }
                    else
                    {
                        JCTree parentJCTree = findParentJCTree(variableDecl);
                        while (!isCanHaveLocalVariable(parentJCTree) && parentJCTree != null)
                        {
                            parentJCTree = findParentJCTree(parentJCTree);
                        }
                        if (parentJCTree != null)
                        {
                            JCTreePosition parentPosition = new JCTreePosition(parentJCTree, asJCCompilationUnit);
                            usageRanges.put(variableDecl, new VarUsageRange(variableDecl, jcTreePosition.startPosition,
                                    parentPosition.endPosition, jcTreePosition.startLineNum,
                                    parentPosition.endLineNum));
                        }
                        else
                        {
                            usageRanges.put(variableDecl, new VarUsageRange(variableDecl, jcTreePosition.startPosition,
                                    parentNode.endPosition, jcTreePosition.startLineNum, parentNode.endLineNum));
                        }
                    }
                }
                super.visitVarDef(variableDecl);
            }
        });
    }
}
