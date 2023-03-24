package com.benpao.compiler;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({
        "unused",
        "DuplicatedCode"
})
public class PBClassNode extends PBInnerNode implements PIBClass<PBClassNode,PBPropertyNode,PBMethodNode>
{


    public final JCTree.JCClassDecl asJCClassDecl;
    protected PBClassNode outsideClassNode = null;
    //<editor-fold desc="只有当前类是匿名类才有值">
    private JCTree.JCNewClass parentJcNewClass;
    //</editor-fold>



    protected final List<PBPropertyNode> propertyNodes = Collections.synchronizedList(new ArrayList<>());
    protected final List<PBMethodNode> methodNodes = Collections.synchronizedList(new ArrayList<>());

    protected PBClassNode(JCTree.JCClassDecl classDecl, PBJCCompilationUnitCompiler compilationUnitCompiler)
    {
        super(classDecl, compilationUnitCompiler);
        asJCClassDecl = classDecl;
        tryParentJcNewClass();
    }

    protected PBClassNode(JCTree.JCClassDecl classDecl, PBClassNode outsideClass)
    {
        super(classDecl, outsideClass.belongJCCompilationUnitCompiler);
        asJCClassDecl = classDecl;
        outsideClassNode = outsideClass;
        tryParentJcNewClass();
    }

    private void tryParentJcNewClass()
    {
        Name simpleName = asJCClassDecl.getSimpleName();
        if (simpleName == null || simpleName.toString()
                .trim()
                .length() == 0)
        {
            JCTree parentJCTree = belongJCCompilationUnitCompiler.findParentJCTree(asJCClassDecl);
            if (parentJCTree instanceof JCTree.JCNewClass)
            {
                parentJcNewClass = (JCTree.JCNewClass) parentJCTree;
            }
            else
            {
                parentJcNewClass = null;
            }
        }
        else
        {
            parentJcNewClass = null;
        }
    }

    public PBClassNode getOutsideClassNode()
    {
        return outsideClassNode;
    }

    @Override
    public boolean isPublic()
    {
        return (asJCClassDecl.mods.flags & Flags.PUBLIC) != 0;
    }

    @Override
    public boolean isProtected()
    {
        return (asJCClassDecl.mods.flags & Flags.PROTECTED) != 0;
    }

    @Override
    public boolean isPrivate()
    {
        return (asJCClassDecl.mods.flags & Flags.PRIVATE) != 0;
    }

    public boolean isStatic()
    {
        return (asJCClassDecl.mods.flags & Flags.STATIC) != 0;
    }

    public boolean isInterface()
    {
        return (asJCClassDecl.mods.flags & Flags.INTERFACE) == Flags.INTERFACE;
    }

    @Override
    public String getPackage()
    {
        return belongJCCompilationUnitCompiler.getPackageName();
    }

    @Override
    public PIBMethod getClassMethod(String name, Class<?>... parameterTypes)
    {
        PBMethodNode declaredMethod = getDeclaredMethod(name, parameterTypes);
        if (declaredMethod != null){
            return declaredMethod;
        }
        for (PBClass pbClass : getExtendsList())
        {
            PBMethod declaredMethodFather = pbClass.getDeclaredMethod(name, parameterTypes);
            if (declaredMethodFather != null){
                if (pbClass.isSamePackage(this)){
                    if (!declaredMethodFather.isPrivate()){
                        return declaredMethodFather;
                    }
                }
                else if (declaredMethodFather.isPublic() || declaredMethodFather.isProtected()){
                    return declaredMethodFather;
                }
            }
        }
        return null;
    }

    public PBMethodNode getDeclaredMethod(String name, Class<?>... parameterTypes){
        for (PBMethodNode methodNode : methodNodes)
        {
            if (methodNode.getMethodName()
                    .equals(name) && methodNode.matchParameters(parameterTypes))
            {
                return methodNode;
            }
        }
        return null;
    }

    @Override
    public PBPropertyNode getDeclaredProperty(String name)
    {
        for (PBPropertyNode propertyNode : propertyNodes)
        {
            if (propertyNode.asJcVariableDecl.getName().toString().equals(name)){
                return propertyNode;
            }
        }
        return null;
    }

    @Override
    public boolean isSonOf(PBClassNode classNode)
    {
        for (PBClass pbClass : getExtendsList())
        {
            if (classNode.getClassName()
                    .equals(pbClass.getClassName()))
            {
                return true;
            }
        }

        for (PBClass pbClass : getImplementsList())
        {
            if (classNode.getClassName()
                    .equals(pbClass.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSonOf(Class<?> classOrInterface)
    {
        for (PBClass pbClass : getExtendsList())
        {
            if (classOrInterface.getName()
                    .equals(pbClass.getClassName()))
            {
                return true;
            }
        }

        for (PBClass pbClass : getImplementsList())
        {
            if (classOrInterface.getName()
                    .equals(pbClass.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    protected boolean isCanUseClassMemberMethod(Class<?> classOrInterface)
    {
        if (isSonOf(classOrInterface))
        {
            return true;
        }
        else if (!isStatic() && outsideClassNode != null)
        {
            return outsideClassNode.isCanUseClassMemberMethod(classOrInterface);
        }
        return false;
    }

    /**
     * 查找pbClassOrT的父类
     *
     * @param pbClass 子类
     * @return 父类
     */
    protected PBClass getFather(PBClass pbClass)
    {
        if (pbClass.asClass != null)
        {
            Type genericSuperclass = pbClass.asClass.getGenericSuperclass();
            if (genericSuperclass != null)
            {
                return parseStrToPBClass(genericSuperclass.getTypeName(), pbClass);
            }
        }
        else if (pbClass.asPBClassNode != null)
        {
            if (pbClass.asPBClassNode.asJCClassDecl.extending != null)
            {
                return parseStrToPBClass(pbClass.asPBClassNode.asJCClassDecl.extending.toString());
            }
            return new PBClass(Object.class);
        }
        return null;
    }

    protected boolean isInstanceof(PBClass pbClass, Class<?> aClass)
    {
        if (pbClass.asClass != null)
        {
            return aClass.isAssignableFrom(pbClass.asClass);
        }
        else if (pbClass.asPBClassNode != null)
        {
            for (PBClass classOrT : pbClass.asPBClassNode.getExtendsList())
            {
                if (aClass.getName()
                        .equals(classOrT.getClassName()))
                {
                    return true;
                }
            }

            for (PBClass classOrT : pbClass.asPBClassNode.getImplementsList())
            {
                if (aClass.getName()
                        .equals(classOrT.getClassName()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    protected String getSimpleName()
    {
        if (parentJcNewClass == null)
        {
            return asJCClassDecl.getSimpleName()
                    .toString();
        }
        else
        {
            return parentJcNewClass.clazz.toString();
        }
    }

    public String getClassName()
    {
        if (outsideClassNode == null)
        {
            return belongJCCompilationUnitCompiler.asJCCompilationUnit.getPackageName() + "." + asJCClassDecl.getSimpleName();
        }
        else
        {
            return outsideClassNode.getClassName() + "$" + asJCClassDecl.getSimpleName();
        }
    }

    private PBClass[] getPbClassesFromTStr(String tStr, PBClass sonFrom)
    {
        tStr = tStr.substring(1);
        tStr = tStr.substring(0, tStr.length() - 1);
        int loc = tStr.indexOf(",");
        ArrayList<String> ts = new ArrayList<>();
        while (loc != -1)
        {
            ts.add(tStr.substring(0, loc)
                    .trim());
            tStr = tStr.substring(loc + 1);
            loc = tStr.indexOf(",");
        }
        ts.add(tStr.trim());
        PBClass[] pbClasses = new PBClass[ts.size()];
        PBClass[] tsSon = null;
        if (sonFrom != null)
        {
            tsSon = sonFrom.getSrcTs();
        }

        for (int i = 0; i < ts.size(); i++)
        {
            String s = ts.get(i);
            //<editor-fold desc="类似class A<T,A> extends Main<A>这种，Main后面的范型A依然是范型，而不是A类，这里主要处理这种情况">
            boolean sonHave = false;
            if (tsSon != null)
            {
                for (PBClass pbClass : tsSon)
                {
                    if (s.equals(pbClass.asTName))
                    {
                        sonHave = true;
                        break;
                    }
                }
            }
            //</editor-fold>
            if (!sonHave)
            {
                pbClasses[i] = parseStrToPBClass(s);
                if (pbClasses[i] == null){
                    int extendsIndex = s.indexOf(PBClass.EXTENDS);
                    if (extendsIndex != -1){
                        String tName = s.substring(0,extendsIndex).trim();
                        String extendsClass = s.substring(extendsIndex + PBClass.EXTENDS.length()).trim();
                        pbClasses[i] = new PBClass(tName,parseStrToPBClass(extendsClass),null);
                    }
                    else{
                        int superIndex = s.indexOf(PBClass.SUPER);
                        if (superIndex != -1){
                            String tName = s.substring(0,superIndex).trim();
                            String superClass = s.substring(superIndex + PBClass.SUPER.length()).trim();
                            pbClasses[i] = new PBClass(tName,null,parseStrToPBClass(superClass));
                        }
                    }
                }
            }
            else
            {
                pbClasses[i] = null;
            }

            if (pbClasses[i] == null)
            {
                pbClasses[i] = new PBClass(s);
            }
        }
        return pbClasses;
    }

    //<editor-fold desc="将各种全写，简写的字符串形式的类名转化为PBClassOrT">
    public PBClass parseStrToPBClass(String name)
    {
        return parseStrToPBClass(name, null);
    }

    PBClass parseStrToPBClass(String name, PBClass sonFrom)
    {
        if (PBClass.isPrimitiveType(name)){
            return new PBClass(name);
        }

        if ("this".equals(name)){
            return new PBClass(this);
        }

        //<editor-fold desc="检查是否是泛型，如果是泛型进行相关处理">
        PBClass[] pbClasses = new PBClass[0];
        int i = name.indexOf('<');
        if (i != -1)
        {
            pbClasses = getPbClassesFromTStr(name.substring(i), sonFrom);
            name = name.substring(0, i);
        }
        //</editor-fold>
        PBClass tryGet;
        PBClassNode classNodeTry = null;

        //<editor-fold desc="说明是全写">
        tryGet = compiler.tryGet(name, pbClasses);
        if (tryGet != null)
        {
            return tryGet;
        }
        //</editor-fold>
        //<editor-fold desc="说明非全写">
        else
        {
            int index = name.indexOf(".");
            //<editor-fold desc="不包含'.' 表示不存在复杂的内部类等形式">
            if (index == -1)
            {
                //<editor-fold desc="尝试从同一个文件中查找">
                for (PBInnerNode allInnerNode : belongJCCompilationUnitCompiler.allInnerNodes)
                {
                    if (allInnerNode instanceof PBClassNode)
                    {
                        PBClassNode classNode = (PBClassNode) allInnerNode;
                        if (classNode.getClassName()
                                .endsWith("." + name) || classNode.getClassName()
                                .endsWith("$" + name))
                        {
                            return new PBClass(classNode, pbClasses);
                        }
                    }
                }
                //</editor-fold>

                //<editor-fold desc="根据Import信息(不含'*'号的Import)查找">
                for (PBImportNode anImport : belongJCCompilationUnitCompiler.getImports())
                {
                    String qualidToString = anImport.qualidToString();
                    if (qualidToString.endsWith("." + name))
                    {
                        tryGet = compiler.tryGet(qualidToString, pbClasses);
                        if (tryGet != null)
                        {
                            return tryGet;
                        }
                        else
                        {
                            log("有明确Import信息的情况下，寻找不到" + qualidToString);
                        }
                    }
                }
                //</editor-fold>

                //<editor-fold desc="从同一个包下面查找">
                String tryName = belongJCCompilationUnitCompiler.getPackageName() + "." + name;
                tryGet = compiler.tryGet(tryName, pbClasses);
                if (tryGet != null)
                {
                    return tryGet;
                }
                //</editor-fold>

                //<editor-fold desc="根据包含'*'号的Import信息查找">
                for (PBImportNode anImport : belongJCCompilationUnitCompiler.getImports())
                {
                    String qualidToString = anImport.qualidToString();
                    if (qualidToString.endsWith(".*"))
                    {
                        qualidToString = qualidToString.substring(0, qualidToString.length() - 1);
                        tryName = qualidToString + name;
                        tryGet = compiler.tryGet(tryName, pbClasses);
                        if (tryGet != null)
                        {
                            return tryGet;
                        }
                    }
                }
                //</editor-fold>

                //<editor-fold desc="从外层类的父亲或者接口的内部类中寻找">
                PBClassNode outsideClass = this.outsideClassNode;
                while (outsideClass != null)
                {
                    for (PBClass pbClass : outsideClass.getExtendsList())
                    {
                        tryName = pbClass.getClassName() + "$" + name;
                        tryGet = compiler.tryGet(tryName, pbClasses);
                        if (tryGet != null)
                        {
                            return tryGet;
                        }
                    }

                    for (PBClass pbClass : outsideClass.getImplementsList())
                    {
                        tryName = pbClass.getClassName() + "$" + name;
                        tryGet = compiler.tryGet(tryName, pbClasses);
                        if (tryGet != null)
                        {
                            return tryGet;
                        }
                    }
                    outsideClass = outsideClass.outsideClassNode;
                }
                //</editor-fold>

                //<editor-fold desc="从java.lang包下查找，因为java.lang不需要引用">
                tryName = "java.lang." + name;
                Class<?> aClass = PABCompiler.tryGetClass(tryName);
                if (aClass != null)
                {
                    return new PBClass(aClass, pbClasses);
                }
                //</editor-fold>
            }
            //</editor-fold>
            else
            {
                String left = name.substring(0, index);
                String right = name.substring(index);
                //<editor-fold desc="根据Import信息(不含'*'号的Import)查找">
                for (PBImportNode anImport : belongJCCompilationUnitCompiler.getImports())
                {
                    String qualidToString = anImport.qualidToString();
                    if (qualidToString.endsWith(left))
                    {
                        String tryName = qualidToString + right.replace(".", "$");
                        tryGet = compiler.tryGet(tryName, pbClasses);
                        if (tryGet != null)
                        {
                            return tryGet;
                        }
                    }
                }
                //</editor-fold>

                //<editor-fold desc="从同一个包下面查找">
                String tryName = belongJCCompilationUnitCompiler.getPackageName() + "." + name.replace(".", "$");
                tryGet = compiler.tryGet(tryName, pbClasses);
                if (tryGet != null)
                {
                    return tryGet;
                }
                //</editor-fold>

                //<editor-fold desc="根据包含'*'号的Import信息查找">
                for (PBImportNode anImport : belongJCCompilationUnitCompiler.getImports())
                {
                    String qualidToString = anImport.qualidToString();
                    if (qualidToString.endsWith(".*"))
                    {
                        String leftNow = qualidToString.substring(0, qualidToString.length() - 1);
                        tryName = leftNow + name.replace(".", "$");
                        tryGet = compiler.tryGet(tryName, pbClasses);
                        if (tryGet != null)
                        {
                            return tryGet;
                        }
                    }
                }
                //</editor-fold>
            }
        }
        //</editor-fold>
        return null;
    }
    //</editor-fold>

    //<editor-fold desc="继承关系处理">
    private void parseExtendsList(PBClass pbClass, List<PBClass> pbClasses)
    {
        if (pbClass.asClass != null)
        {
            Type genericSuperclass = pbClass.asClass.getGenericSuperclass();
            if (genericSuperclass != null)
            {
                PBClass pbClassFather = parseStrToPBClass(genericSuperclass.getTypeName(), pbClass);
                if (pbClassFather != null)
                {
                    pbClassFather.mixT(pbClass);
                    pbClasses.add(pbClassFather);
                    parseExtendsList(pbClassFather, pbClasses);
                }
                else
                {
                    log("通过反射出来的父类" + genericSuperclass.getTypeName() + "解析失败!");
                }
            }
        }
        else if (pbClass.asPBClassNode != null)
        {
            JCTree.JCExpression extending = pbClass.asPBClassNode.asJCClassDecl.extending;
            if (extending != null)
            {
                PBClass pbClassFather = pbClass.asPBClassNode.parseStrToPBClass(extending.toString(), pbClass);
                if (pbClassFather != null)
                {
                    pbClassFather.mixT(pbClass);
                    pbClasses.add(pbClassFather);
                    pbClass.asPBClassNode.parseExtendsList(pbClassFather, pbClasses);
                }
                else
                {
                    log("通过语法树获取的父类" + extending + "解析失败!");
                }
            }
            else
            {
                pbClasses.add(new PBClass(Object.class));
            }
        }
    }
    private ArrayList<PBClass> extendsList;
    @Override
    public List<PBClass> getExtendsList()
    {
        if (extendsList == null)
        {
            extendsList = new ArrayList<>();
            if (asJCClassDecl.extending != null)
            {
                PBClass father = parseStrToPBClass(asJCClassDecl.extending.toString(), new PBClass(this));
                extendsList.add(father);
                parseExtendsList(father, extendsList);
            }
            else
            {
                extendsList.add(new PBClass(Object.class));
            }
        }
        return extendsList;
    }

    @Override
    public PIBMethod getInterfaceDefaultMethod(String name, Class<?>... parameterTypes)
    {
        return null;
    }
    //</editor-fold>

    //<editor-fold desc="接口关系处理">
    private ArrayList<PBClass> implementsList;

    private void parseFatherImplementsList(PBClass classWithT, List<PBClass> pbClasses)
    {
        if (classWithT.asClass != null)
        {
            parseImplementsList(classWithT, pbClasses);
            Type genericSuperclass = classWithT.asClass.getGenericSuperclass();
            if (genericSuperclass != null)
            {
                PBClass pbClass = parseStrToPBClass(genericSuperclass.getTypeName(), classWithT);
                if (pbClass != null)
                {
                    pbClass.mixT(classWithT);
                    parseFatherImplementsList(pbClass, pbClasses);
                }
                else
                {
                    log("通过反射出来的父类" + genericSuperclass.getTypeName() + "解析失败!");
                }
            }
        }
        else if (classWithT.asPBClassNode != null)
        {
            parseImplementsList(classWithT,pbClasses);
            JCTree.JCExpression extending = classWithT.asPBClassNode.asJCClassDecl.extending;
            if (extending != null)
            {
                PBClass pbClass = classWithT.asPBClassNode.parseStrToPBClass(extending.toString(), classWithT);
                if (pbClass != null)
                {
                    pbClass.mixT(classWithT);
                    classWithT.asPBClassNode.parseFatherImplementsList(pbClass, pbClasses);
                }
                else
                {
                    log("通过语法树获取的父类" + extending + "解析失败!");
                }
            }
        }
    }

    private void parseImplementsList(PBClass classWithT, List<PBClass> pbClasses)
    {
        if (classWithT.asClass != null)
        {
            Type[] genericInterfaces = classWithT.asClass.getGenericInterfaces();
            for (Type genericInterface : genericInterfaces)
            {
                PBClass pbClass = parseStrToPBClass(genericInterface.getTypeName(), classWithT);
                if (pbClass != null)
                {
                    pbClass.mixT(classWithT);
                    pbClasses.add(pbClass);
                    parseImplementsList(pbClass, pbClasses);
                }
                else
                {
                    log("通过反射出来的父接口" + genericInterface.getTypeName() + "解析失败!");
                }
            }
        }
        else if (classWithT.asPBClassNode != null)
        {
            com.sun.tools.javac.util.List<JCTree.JCExpression> implementing = classWithT.asPBClassNode.asJCClassDecl.implementing;
            if (implementing != null)
            {
                for (JCTree.JCExpression jcExpression : implementing)
                {
                    PBClass pbClass = classWithT.asPBClassNode.parseStrToPBClass(jcExpression.toString(), classWithT);
                    if (pbClass != null)
                    {
                        pbClass.mixT(classWithT);
                        pbClasses.add(pbClass);
                        classWithT.asPBClassNode.parseImplementsList(pbClass, pbClasses);
                    }
                    else
                    {
                        log(this.getClassName() + "通过语法树获取的父接口" + jcExpression + "解析失败!");
                    }
                }
            }
        }
    }

    @Override
    public List<PBClass> getImplementsList()
    {
        if (implementsList == null)
        {
            implementsList = new ArrayList<>();
            ArrayList<PBClass> implementsListCache = new ArrayList<>();
            //<editor-fold desc="从当前类的直接父接口层层往上迭代">
            for (JCTree.JCExpression jcExpression : asJCClassDecl.implementing)
            {
                PBClass implement = parseStrToPBClass(jcExpression.toString(), new PBClass(this));
                if (implement != null)
                {
                    implementsListCache.add(implement);
                    parseImplementsList(implement, implementsListCache);
                }
                else
                {
                    log(getClassName() + "解析接口[" + jcExpression + "]失败!");
                }
            }
            //</editor-fold>

            if (asJCClassDecl.extending != null)
            {
                PBClass father = parseStrToPBClass(asJCClassDecl.extending.toString(), new PBClass(this));
                parseFatherImplementsList(father, implementsListCache);
            }

            //<editor-fold desc="去重">
            for (PBClass pbClass : implementsListCache)
            {
                boolean contains = false;
                for (PBClass classOrT : implementsList)
                {
                    if (classOrT.getClassName() != null && classOrT.getClassName()
                            .equals(pbClass.getClassName()))
                    {
                        contains = true;
                        break;
                    }
                }
                if (!contains)
                {
                    implementsList.add(pbClass);
                }
            }
            //</editor-fold>
        }
        return implementsList;
    }
    //</editor-fold>
    protected PBClass[] getTS()
    {
        if (asJCClassDecl.typarams != null)
        {
            PBClass[] pbClasses = new PBClass[asJCClassDecl.typarams.size()];
            for (int i = 0; i < pbClasses.length; i++)
            {
                JCTree.JCTypeParameter jcTypeParameter = asJCClassDecl.typarams.get(i);
                pbClasses[i] = new PBClass(jcTypeParameter.toString()
                        .trim());
            }
            return pbClasses;
        }
        return new PBClass[0];
    }

    @Override
    protected void parse(JCTree jcTree)
    {
        JCTree parent = belongJCCompilationUnitCompiler.findParentJCTree(jcTree);
        if (parent == asJCClassDecl)
        {
            if (jcTree instanceof JCTree.JCVariableDecl)
            {
                PBPropertyNode pbPropertyNode = new PBPropertyNode((JCTree.JCVariableDecl) jcTree, this);
                propertyNodes.add(pbPropertyNode);
                addPBInnerNode(pbPropertyNode);
            }
            else if (jcTree instanceof JCTree.JCMethodDecl)
            {
                PBMethodNode pbMethodNode = new PBMethodNode((JCTree.JCMethodDecl) jcTree, this);
                methodNodes.add(pbMethodNode);
                addPBInnerNode(pbMethodNode);
            }
            else if (jcTree instanceof JCTree.JCBlock)
            {
                PBMethodNode pbMethodNode = new PBMethodNode((JCTree.JCBlock) jcTree, this);
                methodNodes.add(pbMethodNode);
                addPBInnerNode(pbMethodNode);
            }
            else if (jcTree instanceof JCTree.JCClassDecl)
            {
                PBClassNode classNode = new PBClassNode((JCTree.JCClassDecl) jcTree, this);
                addPBInnerNode(classNode);
            }
        }
    }
}