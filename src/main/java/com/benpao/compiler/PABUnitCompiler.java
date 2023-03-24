package com.benpao.compiler;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

import java.util.*;

/**
 * 编译单元基类
 */
@SuppressWarnings("unused")
public abstract class PABUnitCompiler extends TreeTranslator
{
    protected final PABCompiler compiler;
    protected final JCTree asJcTree;

    protected final List<PABUnitCompiler> sons = Collections.synchronizedList(new ArrayList<>());

    protected PABUnitCompiler(PABCompiler compiler, JCTree asJcTree)
    {
        this.compiler = compiler;
        this.asJcTree = asJcTree;
    }

    public void log(Object s)
    {
        compiler.log(s);
    }

    @Override
    public <T extends JCTree> T translate(T t)
    {
        if (t == null)
        {
            return super.translate((T) null);
        }
        parse(t);
        return super.translate(t);
    }

    protected abstract void parse(JCTree jcTree);

    protected void parse()
    {
        asJcTree.accept(this);
        sons.forEach(PABUnitCompiler::parse);
        afterParse();
    }

    protected void afterParse(){

    }

    protected void enter(JCTree.Visitor visitor){
        asJcTree.accept(visitor);
        sons.forEach(pabUnitCompiler -> pabUnitCompiler.enter(visitor));
        afterEnter();
    }

    protected final Set<JCTree.JCStatement> needDeleteJcStatements = Collections.synchronizedSet(new HashSet<>());
    public void markDelete(JCTree.JCStatement jcStatement){
        if (jcStatement != null){
            needDeleteJcStatements.add(jcStatement);
        }
    }

    protected void afterEnter(){
        asJcTree.accept(new TreeTranslator(){
            @Override
            public void visitBlock(JCTree.JCBlock jcBlock)
            {
                ArrayList<JCTree.JCStatement> statsNew = new ArrayList<>();
                jcBlock.stats.forEach(jcStatement ->
                {
                    if (!needDeleteJcStatements.contains(jcStatement)){
                        statsNew.add(jcStatement);
                    }
                });
                jcBlock.stats = com.sun.tools.javac.util.List.from(statsNew);
                super.visitBlock(jcBlock);
            }
        });
    }
}
