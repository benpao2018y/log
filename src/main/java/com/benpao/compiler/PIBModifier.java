package com.benpao.compiler;

@SuppressWarnings("unused")
public interface PIBModifier
{
    boolean isPublic();
    boolean isProtected();
    boolean isPrivate();
    default boolean isPackagePrivate(){
        return !isPublic() && !isProtected() && !isPrivate();
    }
    boolean isStatic();
}
