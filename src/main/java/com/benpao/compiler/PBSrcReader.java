package com.benpao.compiler;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作为一个副编译器，主要是通过直接读取源代码来解析语法树，只有在主编译器遇上极端情况下才会被调用
 */
@SuppressWarnings("unused")
public class PBSrcReader
{
    private final File srcRootDir;
    private final boolean readAllSrcRootDir;
    private JavaCompiler javaCompiler;
    private TreePathScanner<Void, Void> scanner;
    protected PABCompiler mainCompiler;
    protected PBSrcReader(PABCompiler mainCompiler)
    {
        this.mainCompiler = mainCompiler;
        String optionValue = mainCompiler.getOptionValue(PBFields.SRC_ROOT_DIR);
        if (optionValue != null)
        {
            File file = new File(optionValue);
            if (file.isDirectory())
            {
                srcRootDir = file;
            }
            else
            {
                srcRootDir = null;
            }
        }
        else
        {
            srcRootDir = null;
        }
        readAllSrcRootDir = mainCompiler.getOptionBoolValue(PBFields.READ_ALL_SRC_ROOT_DIR);

        try
        {
            javaCompiler = ToolProvider.getSystemJavaCompiler().getClass().newInstance();
            scanner = new TreePathScanner<Void, Void>() {
                @Override
                public Void visitCompilationUnit(CompilationUnitTree node, Void aVoid) {
                    if (node instanceof JCTree.JCCompilationUnit){
                        JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) node;
                        PBJCCompilationUnitCompiler compilationUnitCompiler = new PBJCCompilationUnitCompiler(mainCompiler,compilationUnit,false);
                        compilationUnitCompiler.parse();
                        mainCompiler.compilationUnitCompilers.add(compilationUnitCompiler);
                    }
                    return super.visitCompilationUnit(node, aVoid);
                }
            };
        }
        catch (Throwable e)
        {
            log("副编译器创建JavaCompiler失败\r\n" + mainCompiler.throwableToString(e));
        }
    }

    protected final Map<String,PBClassNode> classNodeMap = new ConcurrentHashMap<>();

    protected void read(File... files){
        try(StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, null, null))
        {
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files));
            // 创建编译任务
            JavacTask task = (JavacTask) javaCompiler.getTask(null, fileManager, null, null, null, compilationUnits);
            // 解析源代码并生成语法树
            Iterable<? extends CompilationUnitTree> asts = task.parse();

            for (CompilationUnitTree ast : asts) {
                TreePath path = new TreePath(ast);
                scanner.scan(path, null);
            }
        }
        catch (Throwable e)
        {
            log(mainCompiler.throwableToString(e));
        }
    }

    private void listJavaFile(File dir, List<File> javaFiles){
        File[] files = dir.listFiles();
        if (files == null){
            return;
        }
        for (File file : files)
        {
            if (file.isDirectory()){
                listJavaFile(file,javaFiles);
            }
            else if (file.getPath().endsWith(".java")){
                javaFiles.add(file);
            }
        }
    }

    protected synchronized PBClassNode readClassNodeFromSrc(String fullClassName){
        if (srcRootDir == null){
            log("没有指定 " + PBFields.SRC_ROOT_DIR + " 或者指定的值不正确!所以阅读器无法工作...\r\n*这可能会导致某些特殊情况下解析类继承关系的丢失!");
            return null;
        }
        PBClassNode classNode = classNodeMap.get(fullClassName);
        if (classNode == null){
            if (readAllSrcRootDir){
                ArrayList<File> files = new ArrayList<>();
                listJavaFile(srcRootDir,files);
                File[] filesArr = new File[files.size()];
                for (int i = 0; i < files.size(); i++)
                {
                    filesArr[i] = files.get(i);
                }
                read(filesArr);
                classNode = findPbClassNodeOnce(fullClassName);
            }
            else{
                int index = fullClassName.indexOf("$");
                String fileName;
                if (index == -1){
                    fileName = fullClassName.replace(".", "/") + ".java";
                }
                else{
                    fileName = fullClassName.substring(0,index).replace(".", "/") + ".java";
                }

                File file = new File(srcRootDir,fileName);
                if (file.exists()){
                    read(file);
                    classNode = findPbClassNodeOnce(fullClassName);
                }
                else{
                    return null;
                }
            }
        }
        return classNode;
    }

    private PBClassNode findPbClassNodeOnce(String fullClassName){
        PBClassNode classNode = null;
        for (PBJCCompilationUnitCompiler compilationUnitCompiler : mainCompiler.compilationUnitCompilers)
        {
            if (compilationUnitCompiler.isNeedEnter() || compilationUnitCompiler.isRead){
                continue;
            }
            for (PBClassNode readOnlyInnerClassNode : compilationUnitCompiler.getReadOnlyInnerClassNodes())
            {
                if (readOnlyInnerClassNode.getClassName().equals(fullClassName)){
                    classNode = readOnlyInnerClassNode;
                }
                classNodeMap.put(readOnlyInnerClassNode.getClassName(),readOnlyInnerClassNode);
            }
            compilationUnitCompiler.isRead = true;
        }
        return classNode;
    }

    protected void log(Object s){
        mainCompiler.log(s);
    }
}