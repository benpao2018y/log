package com.benpao.compiler;

public interface PIBProperty extends PIBModifier
{
    String getPropertyName();

    PBClass getPropertyType();
}
