package com.benpao.log;

@SuppressWarnings("unused")
public interface IBLog
{
    static void debug(Object msg)
    {
        UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException("不支持!");
        unsupportedOperationException.printStackTrace();
    }

    static void info(Object msg)
    {
        UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException("不支持!");
        unsupportedOperationException.printStackTrace();
    }

    static void error(Object msg)
    {
        UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException("不支持!");
        unsupportedOperationException.printStackTrace();
    }

    static void warn(Object msg)
    {
        UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException("不支持!");
        unsupportedOperationException.printStackTrace();
    }
    default void logDebug(Object msg)
    {
        UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException("不支持!");
        unsupportedOperationException.printStackTrace();
    }
    default void logInfo(Object msg)
    {
        UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException("不支持!");
        unsupportedOperationException.printStackTrace();
    }

    default void logError(Object msg)
    {
        UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException("不支持!");
        unsupportedOperationException.printStackTrace();
    }

    default void logWarn(Object msg)
    {
        UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException("不支持!");
        unsupportedOperationException.printStackTrace();
    }
}
