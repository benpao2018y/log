package com.benpao.compiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("unused")
public class PBClass implements PIBClass<PBClass, PBProperty, PBMethod>
{
    public static final HashMap<String, String> PRIMITIVE_TYPES = new HashMap<>();

    static
    {
        PRIMITIVE_TYPES.put(boolean.class.getName(), "Z");
        PRIMITIVE_TYPES.put(byte.class.getName(), "B");
        PRIMITIVE_TYPES.put(short.class.getName(), "S");
        PRIMITIVE_TYPES.put(int.class.getName(), "I");
        PRIMITIVE_TYPES.put(long.class.getName(), "J");
        PRIMITIVE_TYPES.put(float.class.getName(), "F");
        PRIMITIVE_TYPES.put(double.class.getName(), "D");
        PRIMITIVE_TYPES.put(char.class.getName(), "C");
    }

    public static boolean isPrimitiveType(String typeName)
    {
        for (String s : PBClass.PRIMITIVE_TYPES.keySet())
        {
            if (s.equals(typeName))
            {
                return true;
            }
        }
        return false;
    }

    protected final String asTName;
    protected final Class<?> asClass;
    protected final PBClassNode asPBClassNode;

    protected final PBClass[] ts;

    protected final PBClass extendsClass;
    protected final PBClass superClass;

    protected static final String EXTENDS = " extends ";
    protected static final String SUPER = " super ";

    protected PBClass(Class<?> classNode, PBClass... pbClasses)
    {
        asClass = classNode;
        ts = pbClasses;
        asPBClassNode = null;
        asTName = null;
        extendsClass = null;
        superClass = null;
    }

    protected PBClass(PBClassNode classNode, PBClass... pbClasses)
    {
        asPBClassNode = classNode;
        ts = pbClasses;
        asClass = null;
        asTName = null;
        extendsClass = null;
        superClass = null;
    }

    protected PBClass(String tName)
    {
        this(tName, null, null);
    }

    protected PBClass(String tName, PBClass extendsClass, PBClass superClass)
    {
        asTName = tName;
        asPBClassNode = null;
        ts = null;
        asClass = null;
        if (extendsClass != null)
        {
            this.extendsClass = extendsClass;
            this.superClass = null;
        }
        else
        {
            this.extendsClass = null;
            this.superClass = superClass;
        }
    }

    public boolean isPrimitiveType()
    {
        if (asTName != null)
        {
            return isPrimitiveType(asTName);
        }
        return false;
    }

    @Override
    public String getPackage()
    {
        if (asClass != null)
        {
            return asClass.getPackage().getName();
        }
        else if (asPBClassNode != null){
            return asPBClassNode.getPackage();
        }
        return null;
    }

    @Override
    public PIBMethod getClassMethod(String name, Class<?>... parameterTypes)
    {
        PBMethod declaredMethod = getDeclaredMethod(name, parameterTypes);
        if (declaredMethod != null)
        {
            return declaredMethod;
        }

        if (asClass != null)
        {
            Class<?> superclass = asClass.getSuperclass();
            while (superclass != null)
            {
                //noinspection CatchMayIgnoreException
                try
                {
                    Method declaredMethodFather = superclass.getDeclaredMethod(name, parameterTypes);
                    if (superclass.getPackage().getName().equals(getPackage()))
                    {
                        if (!Modifier.isPrivate(declaredMethodFather.getModifiers()))
                        {
                            return new PBMethod(declaredMethodFather);
                        }
                    }
                    else if (Modifier.isPublic(declaredMethodFather.getModifiers()) && Modifier.isProtected(
                            declaredMethodFather.getModifiers()))
                    {
                        return new PBMethod(declaredMethodFather);
                    }
                }
                catch (NoSuchMethodException e)
                {

                }
                superclass = superclass.getSuperclass();
            }
        }
        else if (asPBClassNode != null)
        {
            return asPBClassNode.getClassMethod(name, parameterTypes);
        }
        return null;
    }


    public PBMethod getDeclaredMethod(String name, Class<?>... parameterTypes)
    {
        if (asClass != null)
        {
            try
            {
                return new PBMethod(asClass.getDeclaredMethod(name, parameterTypes));
            }
            catch (NoSuchMethodException e)
            {
                return null;
            }
        }
        else if (asPBClassNode != null)
        {
            PBMethodNode declaredMethod = asPBClassNode.getDeclaredMethod(name, parameterTypes);
            if (declaredMethod != null){
                return new PBMethod(declaredMethod);
            }
        }
        return null;
    }

    @Override
    public PBProperty getDeclaredProperty(String name)
    {
        if (asClass != null)
        {
            try
            {
                return new PBProperty(asClass.getDeclaredField(name));
            }
            catch (NoSuchFieldException e)
            {
                return null;
            }
        }
        else if (asPBClassNode != null)
        {
            PBPropertyNode declaredProperty = asPBClassNode.getDeclaredProperty(name);
            if (declaredProperty != null){
                return new PBProperty(declaredProperty);
            }
        }
        return null;
    }

    @Override
    public boolean isSonOf(PBClass pbClass)
    {
        if (asClass != null)
        {
            //<editor-fold desc="这里只要形参pbClass内核是PBClassNode那么一定是返回false，所以只看类和是Class<?>的情况,至于原因是因为一个Class<?>的父级还未编译的话，这个Class<?>是无法获取出来的，所以不存在这种情况">
            if (pbClass.asClass != null && !asClass.equals(pbClass.asClass))
            {
                return pbClass.asClass.isAssignableFrom(asClass);
            }
            //</editor-fold>
        }
        else if (asPBClassNode != null)
        {
            for (PBClass pbClassNow : asPBClassNode.getExtendsList())
            {
                if (pbClassNow.getClassName().equals(pbClass.getClassName()))
                {
                    return true;
                }
            }

            for (PBClass pbClassNow : asPBClassNode.getImplementsList())
            {
                if (pbClassNow.getClassName().equals(pbClass.getClassName()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isSonOf(Class<?> aClass)
    {
        if (asClass != null){
            return !asClass.equals(aClass) && aClass.isAssignableFrom(aClass);
        }
        else if (asPBClassNode != null){
            return asPBClassNode.isSonOf(aClass);
        }
        return false;
    }

    @Override
    public List<PBClass> getImplementsList()
    {
        if (asClass != null){

        }
        else if (asPBClassNode != null){
            return asPBClassNode.getImplementsList();
        }
        return new ArrayList<>();
    }

    @Override
    public List<PBClass> getExtendsList()
    {
        if (asClass != null){
            Type genericSuperclass = asClass.getGenericSuperclass();
            }
        else if (asPBClassNode != null){
            return asPBClassNode.getExtendsList();
        }
        return new ArrayList<>();
    }

    @Override
    public PIBMethod getInterfaceDefaultMethod(String name, Class<?>... parameterTypes)
    {
        return null;
    }

    /**
     * 把子类的T融合进来
     *
     * @param pbClass 作为子类
     */
    protected void mixT(PBClass pbClass)
    {
        if (ts == null || pbClass.ts == null)
        {
            return;
        }
        PBClass[] srcTs = pbClass.getSrcTs();
        for (int i = 0; i < srcTs.length; i++)
        {
            for (int j = 0; j < ts.length; j++)
            {
                if (srcTs[i].asTName != null && srcTs[i].asTName.equals(ts[j].asTName))
                {
                    ts[j] = pbClass.ts[i];
                }
            }
        }
    }

    protected PBClass[] getSrcTs()
    {
        if (asClass != null)
        {
            TypeVariable<? extends Class<?>>[] typeParameters = asClass.getTypeParameters();
            PBClass[] ts1 = new PBClass[typeParameters.length];
            for (int i = 0; i < typeParameters.length; i++)
            {
                ts1[i] = new PBClass(typeParameters[i].getName());
            }
            return ts1;
        }
        else if (asPBClassNode != null)
        {
            return asPBClassNode.getTS();
        }
        return new PBClass[0];
    }

    public String getClassName()
    {
        if (asClass != null)
        {
            return asClass.getName();
        }
        else if (asPBClassNode != null)
        {
            return asPBClassNode.getClassName();
        }
        else
        {
            return null;
        }
    }

    public String getFullName()
    {
        StringBuilder tStr = null;
        if (ts != null)
        {
            for (PBClass t : ts)
            {
                if (t == null)
                {
                    continue;
                }

                if (tStr == null)
                {
                    tStr = new StringBuilder(t.getFullName());
                }
                else
                {
                    tStr.append(",").append(t.getFullName());
                }
            }
        }

        if (tStr != null)
        {
            tStr = new StringBuilder("<" + tStr + ">");
        }
        else
        {
            tStr = new StringBuilder();
        }
        String className = getClassName();
        if (className == null)
        {
            if (extendsClass != null)
            {
                return asTName + EXTENDS + extendsClass.getFullName();
            }
            else if (superClass != null)
            {
                return asTName + SUPER + superClass.getFullName();
            }
            return asTName;
        }
        else
        {
            return className + tStr;
        }
    }

    @Override
    public String toString()
    {
        return getFullName();
    }

    @Override
    public boolean isPublic()
    {
        if (asClass != null)
        {
            return Modifier.isPublic(asClass.getModifiers());
        }
        else if (asPBClassNode != null)
        {
            return asPBClassNode.isPublic();
        }
        return false;
    }

    @Override
    public boolean isProtected()
    {
        if (asClass != null)
        {
            return Modifier.isProtected(asClass.getModifiers());
        }
        else if (asPBClassNode != null)
        {
            return asPBClassNode.isProtected();
        }
        return false;
    }

    @Override
    public boolean isPrivate()
    {
        if (asClass != null)
        {
            return Modifier.isPrivate(asClass.getModifiers());
        }
        else if (asPBClassNode != null)
        {
            return asPBClassNode.isPrivate();
        }
        return false;
    }

    @Override
    public boolean isStatic()
    {
        if (asClass != null)
        {
            return Modifier.isStatic(asClass.getModifiers());
        }
        else if (asPBClassNode != null)
        {
            return asPBClassNode.isStatic();
        }
        return false;
    }
}