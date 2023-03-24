package com.benpao.compiler;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.tools.javac.tree.JCTree;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({
        "unused"
})
public abstract class PABCompiler extends PABJavacTask
{
    protected final List<PBJCCompilationUnitCompiler> compilationUnitCompilers = Collections.synchronizedList(new ArrayList<>());
    protected final Set<PBClassNode> classNodes = Collections.synchronizedSet(new HashSet<>());
    protected final Map<JCTree,PBJCCompilationUnitCompiler> jcTreeMap = new ConcurrentHashMap<>();
    private boolean isCompiled = false;

    protected PBSrcReader srcReader;

    protected PABCompiler(JavacTask task)
    {
        super(task);
        srcReader = new PBSrcReader(this);
    }

    //<editor-fold desc="全局获取">
    /**
     * @param classFullName 类名全写
     * @param pbClasses 可能出现的泛型参数列表
     * @return 获取的结果
     */
    protected PBClass tryGet(String classFullName, PBClass[] pbClasses)
    {
        PBClassNode classNode = tryGetClassNode(classFullName);
        if (classNode != null)
        {
            return new PBClass(classNode, pbClasses);
        }

        Class<?> aClass = tryGetClass(classFullName);
        if (aClass != null)
        {
            return new PBClass(aClass, pbClasses);
        }
        return null;
    }

    protected static Class<?> tryGetClass(String classFullName)
    {
        try
        {
            return Class.forName(classFullName);
        }
        catch (Throwable e)
        {
            return null;
        }
    }

    protected PBClassNode tryGetClassNode(String classFullName)
    {
        for (PBClassNode classNode : classNodes)
        {
            if (classNode.getClassName()
                    .equals(classFullName))
            {
                return classNode;
            }
        }
        return srcReader.readClassNodeFromSrc(classFullName);
    }
    //</editor-fold>

    //<editor-fold desc="根据字符串创建一个JCFieldAccess对象">
    protected JCTree.JCFieldAccess createJCFieldAccess(String str)
    {
        if (str == null)
        {
            return null;
        }
        JCTree.JCIdent ident = null;
        JCTree.JCFieldAccess fieldAccess = null;
        int index = str.indexOf(".");
        while (index != -1)
        {
            String strNow = str.substring(0, index);
            if (ident == null)
            {
                ident = treeMaker.Ident(names.fromString(strNow));
            }
            else
            {
                if (fieldAccess == null)
                {
                    fieldAccess = treeMaker.Select(ident, names.fromString(strNow));
                }
                else
                {
                    fieldAccess = treeMaker.Select(fieldAccess, names.fromString(strNow));
                }
            }

            str = str.substring(index + 1);
            index = str.indexOf(".");
        }

        if (ident == null)
        {
            return null;
        }
        if (fieldAccess == null)
        {
            return treeMaker.Select(ident, names.fromString(str));
        }
        else
        {
            return treeMaker.Select(fieldAccess, names.fromString(str));
        }
    }
    //</editor-fold>

    protected PBJCCompilationUnitCompiler findPBJCCompilationUnitCompiler(JCTree jcTree){
        return jcTreeMap.get(jcTree);
    }

    protected JCTree.JCIdent getLastJCIdent(JCTree.JCFieldAccess fieldAccess){
        JCTree.JCExpression selected = fieldAccess.selected;
        JCTree.JCIdent last;
        while (true)
        {
            if (selected instanceof JCTree.JCFieldAccess)
            {
                selected = ((JCTree.JCFieldAccess) selected).selected;
            }
            else if (selected instanceof JCTree.JCIdent)
            {
                last = (JCTree.JCIdent) selected;
                break;
            }
            else
            {
                return null;
            }
        }
        return last;
    }

    @Override
    public void started(TaskEvent e)
    {
        if (e.getKind() == TaskEvent.Kind.ENTER && !isCompiled)
        {
            //<editor-fold desc="这样抽离出来再处理是为了防止发生ConcurrentModificationException">
            ArrayList<PBJCCompilationUnitCompiler> pBJCCompilationUnitCompilers = new ArrayList<>(compilationUnitCompilers);
            pBJCCompilationUnitCompilers.forEach(compilationUnitCompiler ->
            {
                if (compilationUnitCompiler.isNeedEnter()){
                    log("开始处理 " + compilationUnitCompiler.asJCCompilationUnit.sourcefile.toUri() + " ...");
                    try
                    {
                        compilationUnitCompiler.enter(this);
                    }
                    catch (Throwable ex)
                    {
                        log("enter 异常:");
                        log(throwableToString(ex));
                    }

                    if (getOptionBoolValue(PBFields.SHOW_COMPILED_RESULT)){
                        log("处理结果如下:\r\n" + compilationUnitCompiler.asJCCompilationUnit);
                    }
                    else{
                        log("处理结果隐藏,如果想显示可以添加配置<arg>-A"+PBFields.SHOW_COMPILED_RESULT+"=1</arg>");
                    }
                }
            });
            //</editor-fold>
            isCompiled = true;
        }
    }

    @Override
    public void finished(TaskEvent e)
    {
        if (e.getKind() == TaskEvent.Kind.PARSE)
        {
            CompilationUnitTree compilationUnit = e.getCompilationUnit();
            if (compilationUnit instanceof JCTree.JCCompilationUnit)
            {
                PBJCCompilationUnitCompiler jcCompilationUnitCompiler = new PBJCCompilationUnitCompiler(this, (JCTree.JCCompilationUnit) compilationUnit);
                log("开始解析 " + jcCompilationUnitCompiler.asJCCompilationUnit.sourcefile.toUri() + " ...");
                try
                {
                    jcCompilationUnitCompiler.parse();
                    compilationUnitCompilers.add(jcCompilationUnitCompiler);
                }
                catch (Throwable ex)
                {
                    log("解析异常:");
                    log(throwableToString(ex));
                }
            }
        }
    }
}
