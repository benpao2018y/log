package com.benpao.compiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@SuppressWarnings("unused")
public class PBMethod implements PIBMethod
{
    public final Method asMethod;
    public final PBMethodNode asPBMethodNode;

    PBMethod(Method method){
        asMethod = method;
        asPBMethodNode = null;
    }

    PBMethod(PBMethodNode pbMethodNode){
        asPBMethodNode = pbMethodNode;
        asMethod = null;
    }

    @Override
    public boolean isPublic()
    {
        if (asMethod != null){
            return Modifier.isPublic(asMethod.getModifiers());
        }
        else if (asPBMethodNode != null){
            return asPBMethodNode.isPublic();
        }
        return false;
    }

    @Override
    public boolean isProtected()
    {
        if (asMethod != null){
            return Modifier.isProtected(asMethod.getModifiers());
        }
        else if (asPBMethodNode != null){
            return asPBMethodNode.isProtected();
        }
        return false;
    }

    @Override
    public boolean isPrivate()
    {
        if (asMethod != null){
            return Modifier.isPrivate(asMethod.getModifiers());
        }
        else if (asPBMethodNode != null){
            return asPBMethodNode.isPrivate();
        }
        return false;
    }

    @Override
    public boolean isStatic()
    {
        if (asMethod != null){
            return Modifier.isStatic(asMethod.getModifiers());
        }
        else if (asPBMethodNode != null){
            return asPBMethodNode.isStatic();
        }
        return false;
    }

    @Override
    public String getMethodName()
    {
        if (asMethod != null){
            return asMethod.getName();
        }
        else if (asPBMethodNode != null){
            return asPBMethodNode.getMethodName();
        }
        return null;
    }

    @Override
    public boolean isDefault()
    {
        if (asMethod != null){
            return asMethod.isDefault();
        }
        else if (asPBMethodNode != null){
            return asPBMethodNode.isDefault();
        }
        return false;
    }
}
