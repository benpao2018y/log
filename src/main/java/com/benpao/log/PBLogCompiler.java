package com.benpao.log;

import com.benpao.compiler.*;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

@SuppressWarnings("DuplicatedCode")
public class PBLogCompiler extends PABCompiler
{
    static class IBLogType{
        final boolean isClose;
        final String typeStr;
        final String staticMethodName;
        final String methodName;

        private IBLogType(boolean isClose,String typeStr,String staticMethodName,String methodName){
            this.isClose = isClose;
            this.typeStr = typeStr;
            this.staticMethodName = staticMethodName;
            this.methodName = methodName;
        }
    }

    final IBLogType[] logTypes;

    public PBLogCompiler(JavacTask task)
    {
        super(task);
        logTypes = new IBLogType[]{
                new IBLogType(getOptionBoolValue(BLogFields.CLOSE_DEBUG), BLogInfo.class.getName() + ".Type.Debug", "debug", "logDebug"),
                new IBLogType(getOptionBoolValue(BLogFields.CLOSE_INFO), BLogInfo.class.getName() + ".Type.Info", "info", "logInfo"),
                new IBLogType(getOptionBoolValue(BLogFields.CLOSE_WARN), BLogInfo.class.getName() + ".Type.Warn", "warn", "logWarn"),
                new IBLogType(getOptionBoolValue(BLogFields.CLOSE_ERROR), BLogInfo.class.getName() + ".Type.Error", "error", "logError"),
        };
    }

    //<editor-fold desc="创建输出语句">
    private JCTree.JCMethodInvocation createLogMethodInvocation(String logType, JCTree.JCExpression msg, String methodPath, String javaFileName, int lineNum)
    {
        JCTree.JCNewClass newBLogInfo = treeMaker.NewClass(null, null, createJCFieldAccess("com.benpao.log.BLogInfo"),
                com.sun.tools.javac.util.List.of(createJCFieldAccess(logType), msg, treeMaker.Literal(methodPath),
                        treeMaker.Literal(javaFileName), treeMaker.Literal(lineNum)), null);
        JCTree.JCMethodInvocation apply = treeMaker.Apply(com.sun.tools.javac.util.List.nil(),
                createJCFieldAccess("com.benpao.log.BLogConfig.getDefaultBLog"), com.sun.tools.javac.util.List.nil());
        JCTree.JCFieldAccess log1 = treeMaker.Select(apply, names.fromString("log"));

        return treeMaker.Apply(com.sun.tools.javac.util.List.nil(), log1,
                com.sun.tools.javac.util.List.of(newBLogInfo));
    }

    private JCTree.JCMethodInvocation createLogMethodInvocation(String logType, JCTree.JCMethodInvocation jcMethodInvocation)
    {
        PBJCCompilationUnitCompiler pbjcCompilationUnitCompiler = findPBJCCompilationUnitCompiler(jcMethodInvocation);
        //<editor-fold desc="如果找不到所属的单元编译器，直接原封不动的返回">
        if (pbjcCompilationUnitCompiler == null)
        {
            return jcMethodInvocation;
        }
        //</editor-fold>

        //<editor-fold desc="控制开关">
        for (IBLogType type : logTypes)
        {
            if (type.typeStr.equals(logType) && type.isClose){
                JCTree parentJCTree = pbjcCompilationUnitCompiler.findParentJCTree(jcMethodInvocation);
                while (parentJCTree != null && !(parentJCTree instanceof JCTree.JCStatement)){
                    parentJCTree = pbjcCompilationUnitCompiler.findParentJCTree(parentJCTree);
                }
                if (parentJCTree != null){
                    pbjcCompilationUnitCompiler.markDelete((JCTree.JCStatement) parentJCTree);
                    return null;
                }
            }
        }
        //</editor-fold>

        PBInnerNode parentNode = pbjcCompilationUnitCompiler.findParentNode(jcMethodInvocation);
        //<editor-fold desc="如果找不到父节点或者父节点不属于PBClassNodeInnerNode类型，那么直接原样返回">
        if (parentNode == null)
        {
            return jcMethodInvocation;
        }
        if (!(parentNode instanceof PBClassNodeInnerNode))
        {
            log(jcMethodInvocation);
            log(parentNode.getClass());
            return jcMethodInvocation;
        }
        //</editor-fold>

        PBJCCompilationUnitCompiler.JCTreePosition jcTreePosition = pbjcCompilationUnitCompiler.getJCTreePosition(
                jcMethodInvocation);
        //<editor-fold desc="如果找不到位置，那么直接原样返回">
        if (jcTreePosition == null)
        {
            return jcMethodInvocation;
        }
        //</editor-fold>

        return createLogMethodInvocation(logType, jcMethodInvocation.args.get(0),
                ((PBClassNodeInnerNode) parentNode).getPath(), pbjcCompilationUnitCompiler.getJavaFileName(),
                jcTreePosition.startLineNum);
    }
    //</editor-fold>

    //<editor-fold desc="创建输出Lambda">
    private JCTree.JCLambda createLogLambda(String logType,JCTree.JCMemberReference memberReference){
        PBJCCompilationUnitCompiler pbjcCompilationUnitCompiler = findPBJCCompilationUnitCompiler(memberReference);
        //<editor-fold desc="如果找不到所属的单元编译器，直接原封不动的返回">
        if (pbjcCompilationUnitCompiler == null)
        {
            return null;
        }
        //</editor-fold>

        PBInnerNode parentNode = pbjcCompilationUnitCompiler.findParentNode(memberReference);
        //<editor-fold desc="如果找不到父节点或者父节点不属于PBClassNodeInnerNode类型，那么直接原样返回">
        if (parentNode == null)
        {
            return null;
        }
        if (!(parentNode instanceof PBClassNodeInnerNode))
        {
            return null;
        }
        //</editor-fold>

        PBJCCompilationUnitCompiler.JCTreePosition jcTreePosition = pbjcCompilationUnitCompiler.getJCTreePosition(
                memberReference);
        //<editor-fold desc="如果找不到位置，那么直接原样返回">
        if (jcTreePosition == null)
        {
            return null;
        }
        //</editor-fold>

        return createLogLambda(logType, jcTreePosition.startLineNum,memberReference.pos,
                (PBClassNodeInnerNode) parentNode);
    }

    private JCTree.JCLambda createLogLambda(String logType, int lineNum, int pos, PBClassNodeInnerNode classNodeInnerNode)
    {
        Name m = names.fromString("m");
        JCTree.JCMethodInvocation logMethodInvocation = createLogMethodInvocation(logType, treeMaker.Ident(m),
                classNodeInnerNode.getPath(), classNodeInnerNode.belongJCCompilationUnitCompiler.getJavaFileName(),
                lineNum);
        JCTree.JCBlock block = null;
        //<editor-fold desc="控制开关">
        for (IBLogType type : logTypes)
        {
            if (type.typeStr.equals(logType) && type.isClose){
                block = treeMaker.Block(0, List.nil());
                break;
            }
        }
        //</editor-fold>

        if (block == null){
            block = treeMaker.Block(0,
                    com.sun.tools.javac.util.List.of(treeMaker.Exec(logMethodInvocation)));
        }
        JCTree.JCVariableDecl lambdaVar = treeMaker.VarDef(treeMaker.Modifiers(0), m, null, null);
        lambdaVar.pos = pos;
        return treeMaker.Lambda(com.sun.tools.javac.util.List.of(lambdaVar), block);
    }
    //</editor-fold>


    //<editor-fold desc="判断invocation是否实际上是执行到的IBLog的非静态方法methodName">
    private boolean isSrcFromIBLogMethod(JCTree.JCMethodInvocation invocation, String methodName)
    {
        JCTree.JCExpression meth = invocation.meth;
        if (meth instanceof JCTree.JCFieldAccess)
        {
            JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) meth;
            if (!methodName.equals(fieldAccess.name.toString()))
            {
                return false;
            }
            JCTree.JCExpression selected = fieldAccess.selected;

            PBJCCompilationUnitCompiler pbjcCompilationUnitCompiler = findPBJCCompilationUnitCompiler(invocation);
            if (pbjcCompilationUnitCompiler == null)
            {
                return false;
            }

            PBClassNode parentClassNode = pbjcCompilationUnitCompiler.findParentClassNode(invocation);
            if (parentClassNode == null)
            {
                return false;
            }

            if ("this".equals(selected.toString()))
            {
                if (parentClassNode.isSonOf(IBLog.class))
                {
                    if (parentClassNode.isClassMethodExit(methodName, Object.class))
                    {
                        return false;
                    }

                    for (PBClass pbClass : parentClassNode.getImplementsList())
                    {
                        if (pbClass.isSonOf(IBLog.class) && pbClass.isDeclaredMethod(methodName, Object.class))
                        {
                            return false;
                        }
                    }
                    return true;
                }
            }
            else
            {
                String selectedStr = selected.toString();
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
                        return false;
                    }
                }

                PBJCCompilationUnitCompiler.VarUsageRange canUseVarByName = pbjcCompilationUnitCompiler.findCanUseVarByName(
                        last.getName().toString(), last);
                if (canUseVarByName != null)
                {
                    String[] split = selectedStr.split("\\.");

                    PBClass pbClass = parentClassNode.parseStrToPBClass(
                            canUseVarByName.asJcVariableDecl.getType().toString());
                    if (pbClass == null){
                        return false;
                    }

                    for (int i = 1; i < split.length; i++)
                    {
                        PBProperty declaredProperty = pbClass.getDeclaredProperty(split[i]);
                        if (declaredProperty == null){
                            return false;
                        }
                        pbClass = declaredProperty.getPropertyType();
                    }

                    if (pbClass.isSonOf(IBLog.class)){
                        if (pbClass.isClassMethodExit(methodName, Object.class))
                        {
                            return false;
                        }

                        return !pbClass.isInterfaceDefaultMethodExit(methodName, Object.class);
                    }
                }
            }
        }
        else if (meth instanceof JCTree.JCIdent)
        {
            if (!methodName.equals(((JCTree.JCIdent) meth).name.toString()))
            {
                return false;
            }

            PBJCCompilationUnitCompiler pbjcCompilationUnitCompiler = findPBJCCompilationUnitCompiler(invocation);
            if (pbjcCompilationUnitCompiler == null)
            {
                return false;
            }

            PBClassNode parentClassNode = pbjcCompilationUnitCompiler.findParentClassNode(invocation);
            if (parentClassNode == null)
            {
                return false;
            }

            if (parentClassNode.isSonOf(IBLog.class))
            {
                if (parentClassNode.isClassMethodExit(methodName, Object.class))
                {
                    return false;
                }

                for (PBClass pbClass : parentClassNode.getImplementsList())
                {
                    if (pbClass.isSonOf(IBLog.class) && pbClass.isDeclaredMethod(methodName, Object.class))
                    {
                        return false;
                    }
                }
                return true;
            }
            else if (!parentClassNode.isStatic())
            {
                PBClassNode outsideClassNode = parentClassNode.getOutsideClassNode();
                while (outsideClassNode != null)
                {
                    if (outsideClassNode.isSonOf(IBLog.class))
                    {
                        if (outsideClassNode.isClassMethodExit(methodName, Object.class))
                        {
                            return false;
                        }

                        for (PBClass pbClass : outsideClassNode.getImplementsList())
                        {
                            if (pbClass.isSonOf(IBLog.class) && pbClass.isDeclaredMethod(methodName, Object.class))
                            {
                                return false;
                            }
                        }
                        if (outsideClassNode.isStatic())
                        {
                            break;
                        }

                        return true;
                    }
                    outsideClassNode = outsideClassNode.getOutsideClassNode();
                }
            }
        }
        return false;
    }

    //</editor-fold>
    //<editor-fold desc="判断invocation是否实际上是执行到的IBLog的静态方法methodName">
    private boolean isSrcFromIBLogStaticMethod(JCTree.JCMethodInvocation invocation, String methodName)
    {
        JCTree.JCExpression meth = invocation.meth;
        if (meth instanceof JCTree.JCFieldAccess)
        {
            JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) meth;
            if (!methodName.equals(fieldAccess.name.toString()))
            {
                return false;
            }
            JCTree.JCIdent last= getLastJCIdent(fieldAccess);
            if (last == null){
                return false;
            }
            PBJCCompilationUnitCompiler pbjcCompilationUnitCompiler = findPBJCCompilationUnitCompiler(last);
            if (pbjcCompilationUnitCompiler == null)
            {
                return false;
            }

            PBJCCompilationUnitCompiler.VarUsageRange canUseVarByName = pbjcCompilationUnitCompiler.findCanUseVarByName(
                    last.getName().toString(), last);
            if (canUseVarByName != null)
            {
                return false;
            }

            PBClassNode parentClassNode = pbjcCompilationUnitCompiler.findParentClassNode(last);
            if (parentClassNode == null)
            {
                return false;
            }
            for (PBClass pbClass : parentClassNode.getExtendsList())
            {
                PBProperty declaredProperty = pbClass.getDeclaredProperty(last.getName().toString());
                if (declaredProperty != null)
                {
                    if (pbClass.isSamePackage(parentClassNode))
                    {
                        if (!declaredProperty.isPrivate())
                        {
                            return false;
                        }
                    }
                    else if (declaredProperty.isPublic() || declaredProperty.isProtected())
                    {
                        return false;
                    }
                }
            }
            PBClass pbClass = parentClassNode.parseStrToPBClass(fieldAccess.selected.toString());
            if (pbClass == null)
            {
                return false;
            }
            return pbClass.getClassName().equals(IBLog.class.getName());
        }
        else if (meth instanceof JCTree.JCIdent)
        {
            if (!methodName.equals(((JCTree.JCIdent) meth).name.toString()))
            {
                return false;
            }
            PBJCCompilationUnitCompiler pbjcCompilationUnitCompiler = findPBJCCompilationUnitCompiler(invocation);
            if (pbjcCompilationUnitCompiler == null)
            {
                return false;
            }

            boolean have = false;
            for (PBImportNode staticImport : pbjcCompilationUnitCompiler.getStaticImports())
            {
                String qualidToString = staticImport.qualidToString();
                if ((IBLog.class.getName() + ".*").equals(
                        qualidToString) || (IBLog.class.getName() + "." + methodName).equals(qualidToString))
                {
                    have = true;
                    break;
                }
            }
            if (!have)
            {
                return false;
            }

            PBClassNode parentClassNode = pbjcCompilationUnitCompiler.findParentClassNode(invocation);
            if (parentClassNode == null)
            {
                return false;
            }

            //<editor-fold desc="检测类节点以及往上继承的是否有该方法">
            if (parentClassNode.isClassMethodExit(methodName, Object.class))
            {
                return false;
            }
            //</editor-fold>

            //<editor-fold desc="检测所有外层类节点以及往上继承的是否有该方法">
            if (!parentClassNode.isStatic())
            {
                PBClassNode outsideClassNode = parentClassNode.getOutsideClassNode();
                while (outsideClassNode != null)
                {
                    if (outsideClassNode.isClassMethodExit(methodName, Object.class))
                    {
                        return false;
                    }
                    if (outsideClassNode.isStatic())
                    {
                        break;
                    }
                    outsideClassNode = outsideClassNode.getOutsideClassNode();
                }
            }

            //</editor-fold>
            return true;
        }
        return false;
    }
    //</editor-fold>

    protected JCTree.JCMethodInvocation tryDealLogMethod(JCTree.JCMethodInvocation invocation)
    {
        if (invocation.args.size() != 1)
        {
            return invocation;
        }

        for (IBLogType logType : logTypes)
        {
            if (isSrcFromIBLogStaticMethod(invocation, logType.staticMethodName))
            {
                return createLogMethodInvocation(logType.typeStr, invocation);
            }

            if (isSrcFromIBLogMethod(invocation, logType.methodName))
            {
                return createLogMethodInvocation(logType.typeStr, invocation);
            }
        }
        return invocation;
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation invocation)
    {
        JCTree.JCMethodInvocation translate = tryDealLogMethod(invocation);
        if (translate != invocation){
            if (translate != null){
                result = translate;
            }
            else{
                super.visitApply(invocation);
            }
        }
        else{
            super.visitApply(invocation);
        }
    }

    @Override
    public void visitSelect(JCTree.JCFieldAccess jcFieldAccess)
    {
        super.visitSelect(jcFieldAccess);
    }

    @Override
    public void visitIdent(JCTree.JCIdent jcIdent)
    {
        super.visitIdent(jcIdent);
    }
    private boolean isSrcFromIBLogMethod(JCTree.JCMemberReference memberReference, String methodName){
        if (!memberReference.name.toString().equals(methodName)){
            return false;
        }
        JCTree.JCExpression expr = memberReference.expr;
        if (expr instanceof JCTree.JCFieldAccess){
            JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) expr;
            JCTree.JCIdent lastJCIdent = getLastJCIdent(fieldAccess);
            if (lastJCIdent == null){
                return false;
            }

            PBJCCompilationUnitCompiler pbjcCompilationUnitCompiler = findPBJCCompilationUnitCompiler(lastJCIdent);
            if (pbjcCompilationUnitCompiler == null)
            {
                return false;
            }

            PBClassNode parentClassNode = pbjcCompilationUnitCompiler.findParentClassNode(lastJCIdent);
            if (parentClassNode == null)
            {
                return false;
            }

            PBJCCompilationUnitCompiler.VarUsageRange canUseVarByName = pbjcCompilationUnitCompiler.findCanUseVarByName(lastJCIdent.getName().toString(), lastJCIdent);
            if (canUseVarByName != null)
            {
                String[] split = fieldAccess.toString().split("\\.");

                PBClass pbClass = parentClassNode.parseStrToPBClass(
                        canUseVarByName.asJcVariableDecl.getType().toString());
                if (pbClass == null){
                    return false;
                }

                for (int i = 1; i < split.length; i++)
                {
                    PBProperty declaredProperty = pbClass.getDeclaredProperty(split[i]);
                    if (declaredProperty == null){
                        return false;
                    }
                    pbClass = declaredProperty.getPropertyType();
                }

                if (pbClass.isSonOf(IBLog.class)){
                    if (pbClass.isClassMethodExit(methodName, Object.class))
                    {
                        return false;
                    }

                    return !pbClass.isInterfaceDefaultMethodExit(methodName, Object.class);
                }
            }
        }
        else if (expr instanceof JCTree.JCIdent)
        {
            JCTree.JCIdent ident = (JCTree.JCIdent) expr;
            PBJCCompilationUnitCompiler pbjcCompilationUnitCompiler = findPBJCCompilationUnitCompiler(memberReference);
            if (pbjcCompilationUnitCompiler == null)
            {
                return false;
            }

            PBClassNode parentClassNode = pbjcCompilationUnitCompiler.findParentClassNode(ident);
            if (parentClassNode == null)
            {
                return false;
            }

            if ("this".equals(ident.toString()))
            {
                if (parentClassNode.isSonOf(IBLog.class))
                {
                    if (parentClassNode.isClassMethodExit(methodName, Object.class))
                    {
                        return false;
                    }

                    for (PBClass pbClass : parentClassNode.getImplementsList())
                    {
                        if (pbClass.isSonOf(IBLog.class) && pbClass.isDeclaredMethod(methodName, Object.class))
                        {
                            return false;
                        }
                    }
                    return true;
                }
            }
            else
            {
                PBJCCompilationUnitCompiler.VarUsageRange canUseVarByName = pbjcCompilationUnitCompiler.findCanUseVarByName(ident.getName().toString(), ident);
                if (canUseVarByName != null)
                {
                    PBClass pbClass = parentClassNode.parseStrToPBClass(
                            canUseVarByName.asJcVariableDecl.getType().toString());
                    if (pbClass.isSonOf(IBLog.class)){
                        if (pbClass.isClassMethodExit(methodName, Object.class))
                        {
                            return false;
                        }
                        return !pbClass.isInterfaceDefaultMethodExit(methodName, Object.class);
                    }
                }
                return false;
            }
        }
        return false;
    }

    private boolean isSrcFromIBLogStaticMethod(JCTree.JCMemberReference memberReference, String methodName){
        if (!memberReference.name.toString().equals(methodName)){
            return false;
        }
        JCTree.JCExpression expr = memberReference.expr;
        if (expr instanceof JCTree.JCFieldAccess){
            JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) expr;
            JCTree.JCIdent last = getLastJCIdent(fieldAccess);
            if (last == null){
                return false;
            }
            PBJCCompilationUnitCompiler pbjcCompilationUnitCompiler = findPBJCCompilationUnitCompiler(last);
            if (pbjcCompilationUnitCompiler == null)
            {
                return false;
            }

            PBJCCompilationUnitCompiler.VarUsageRange canUseVarByName = pbjcCompilationUnitCompiler.findCanUseVarByName(last.getName().toString(), last);
            if (canUseVarByName != null)
            {
                return false;
            }

            PBClassNode parentClassNode = pbjcCompilationUnitCompiler.findParentClassNode(last);
            if (parentClassNode == null)
            {
                return false;
            }
            for (PBClass pbClass : parentClassNode.getExtendsList())
            {
                PBProperty declaredProperty = pbClass.getDeclaredProperty(last.getName().toString());
                if (declaredProperty != null)
                {
                    if (pbClass.isSamePackage(parentClassNode))
                    {
                        if (!declaredProperty.isPrivate())
                        {
                            return false;
                        }
                    }
                    else if (declaredProperty.isPublic() || declaredProperty.isProtected())
                    {
                        return false;
                    }
                }
            }
            PBClass pbClass = parentClassNode.parseStrToPBClass(fieldAccess.toString());
            if (pbClass == null)
            {
                return false;
            }
            return pbClass.getClassName().equals(IBLog.class.getName());
        }
        else if (expr instanceof JCTree.JCIdent){
            JCTree.JCIdent ident = (JCTree.JCIdent) expr;
            PBJCCompilationUnitCompiler pbjcCompilationUnitCompiler = findPBJCCompilationUnitCompiler(memberReference);
            if (pbjcCompilationUnitCompiler == null)
            {
                return false;
            }

            PBJCCompilationUnitCompiler.VarUsageRange canUseVarByName = pbjcCompilationUnitCompiler.findCanUseVarByName(ident.getName().toString(), ident);
            if (canUseVarByName != null)
            {
                return false;
            }

            PBClassNode parentClassNode = pbjcCompilationUnitCompiler.findParentClassNode(ident);
            if (parentClassNode == null)
            {
                return false;
            }
            for (PBClass pbClass : parentClassNode.getExtendsList())
            {
                PBProperty declaredProperty = pbClass.getDeclaredProperty(ident.getName().toString());
                if (declaredProperty != null)
                {
                    if (pbClass.isSamePackage(parentClassNode))
                    {
                        if (!declaredProperty.isPrivate())
                        {
                            return false;
                        }
                    }
                    else if (declaredProperty.isPublic() || declaredProperty.isProtected())
                    {
                        return false;
                    }
                }
            }

            PBClass pbClass = parentClassNode.parseStrToPBClass(ident.toString());
            if (pbClass == null)
            {
                return false;
            }
            return pbClass.getClassName().equals(IBLog.class.getName());
        }
        return false;
    }

    protected JCTree.JCLambda tryDealLogMethod(JCTree.JCMemberReference memberReference){
        for (IBLogType logType : logTypes)
        {
            if (isSrcFromIBLogStaticMethod(memberReference, logType.staticMethodName))
            {
                return createLogLambda(logType.typeStr, memberReference);
            }

            if (isSrcFromIBLogMethod(memberReference, logType.methodName))
            {
                return createLogLambda(logType.typeStr, memberReference);
            }
        }
        return null;
    }

    @Override
    public void visitReference(JCTree.JCMemberReference jcMemberReference)
    {
        JCTree.JCLambda jcLambda = tryDealLogMethod(jcMemberReference);
        if (jcLambda != null){
            result = jcLambda;
        }
        else{
            super.visitReference(jcMemberReference);
        }
    }
}