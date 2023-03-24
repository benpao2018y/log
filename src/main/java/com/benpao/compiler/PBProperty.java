package com.benpao.compiler;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class PBProperty implements PIBProperty
{
    public final Field asField;
    public final PBPropertyNode asPBPropertyNode;

    PBProperty(Field field){
        asField = field;
        asPBPropertyNode = null;
    }

    PBProperty(PBPropertyNode pbPropertyNode){
        asPBPropertyNode = pbPropertyNode;
        asField = null;
    }

    @Override
    public boolean isPublic()
    {
        if (asField != null){
            return Modifier.isPublic(asField.getModifiers());
        }
        else if (asPBPropertyNode != null){
            return asPBPropertyNode.isPublic();
        }
        return false;
    }

    @Override
    public boolean isProtected()
    {
        if (asField != null){
            return Modifier.isProtected(asField.getModifiers());
        }
        else if (asPBPropertyNode != null){
            return asPBPropertyNode.isProtected();
        }
        return false;
    }

    @Override
    public boolean isPrivate()
    {
        if (asField != null){
            return Modifier.isPrivate(asField.getModifiers());
        }
        else if (asPBPropertyNode != null){
            return asPBPropertyNode.isPrivate();
        }
        return false;
    }

    @Override
    public boolean isStatic()
    {
        if (asField != null){
            return Modifier.isStatic(asField.getModifiers());
        }
        else if (asPBPropertyNode != null){
            return asPBPropertyNode.isStatic();
        }
        return false;
    }

    @Override
    public String getPropertyName()
    {
        if (asField != null){
            return asField.getName();
        }
        else if (asPBPropertyNode != null){
            return asPBPropertyNode.getPropertyName();
        }
        return null;
    }

    @Override
    public PBClass getPropertyType()
    {
        if (asField != null){
            return new PBClass(asField.getType());
        }
        else if (asPBPropertyNode != null){
            return asPBPropertyNode.getPropertyType();
        }
        return null;
    }
}
