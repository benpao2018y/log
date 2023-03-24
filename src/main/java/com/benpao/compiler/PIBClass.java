package com.benpao.compiler;

import java.util.List;

@SuppressWarnings({
        "rawtypes",
        "unused"
})
public interface PIBClass<C extends PIBClass,P extends PIBProperty,M extends PIBMethod> extends PIBModifier
{
    String getPackage();

    default boolean isSamePackage(PIBClass aClass){
        if (aClass == null){
            return false;
        }
        String aPackage = getPackage();
        if (aPackage != null){
            return aPackage.equals(aClass.getPackage());
        }
        return false;
    }

    default boolean isDeclaredMethod(String name, Class<?>... parameterTypes){
        return getDeclaredMethod(name,parameterTypes) != null;
    }

    default boolean isClassMethodExit(String name, Class<?>... parameterTypes){
        return getClassMethod(name,parameterTypes) != null;
    }

    default boolean isDeclaredProperty(String propertyName){
        return getDeclaredProperty(propertyName) != null;
    }

    //<editor-fold desc="获取所有自身方法和父类方法，但不包括接口">
    PIBMethod getClassMethod(String name, Class<?>... parameterTypes);
    //</editor-fold>
    M getDeclaredMethod(String name, Class<?>... parameterTypes);
    P getDeclaredProperty(String name);
    boolean isSonOf(C c);
    boolean isSonOf(Class<?> aClass);
    List<PBClass> getImplementsList();
    List<PBClass> getExtendsList();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean isInterfaceDefaultMethodExit(String name, Class<?>... parameterTypes){
        return getClassMethod(name,parameterTypes) != null;
    }
    PIBMethod getInterfaceDefaultMethod(String name, Class<?>... parameterTypes);
}
