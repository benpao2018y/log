package com.benpao.log;

@SuppressWarnings("unused")
public interface IBLog
{
    static void debug(Object msg)
    {
        throw new UnsupportedOperationException();
    }

    static void info(Object msg)
    {
        throw new UnsupportedOperationException();
    }

    static void error(Object msg)
    {
        throw new UnsupportedOperationException();
    }

    static void warn(Object msg)
    {
        throw new UnsupportedOperationException();
    }
    default void logDebug(Object msg)
    {
        throw new UnsupportedOperationException();
    }
    default void logInfo(Object msg)
    {
        throw new UnsupportedOperationException();
    }

    default void logError(Object msg)
    {
        throw new UnsupportedOperationException();
    }

    default void logWarn(Object msg)
    {
        throw new UnsupportedOperationException();
    }
}
