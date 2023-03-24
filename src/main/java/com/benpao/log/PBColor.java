package com.benpao.log;

import java.util.Objects;

@SuppressWarnings("DuplicatedCode")
public enum PBColor
{
    RED(31), GREEN(32), YELLOW(33), BLUE(34), PURPLE(35), LIGHT_BLUE(36);

    private final int value;
    private static final String end = "\u001b[0m";

    PBColor(int value)
    {
        this.value = value;
    }

    public String pack(String str)
    {
        if (currentOS() == OS.WINDOWS && isRunByJar())
        {
            return str;
        }
        else
        {
            return "\u001b[" + value + "m" + str + end;
        }
    }

    //<editor-fold desc="系统">
    private enum OS
    {
        WINDOWS("Windows OS"), LINUX("Linux OS"), MAC("Mac OS"), UN_KNOW("UnKnow Os");
        private final String osString;

        OS(String osString)
        {
            this.osString = osString;
        }

        @Override
        public String toString()
        {
            return osString;
        }
    }

    private static OS currentOS;

    private static OS currentOS()
    {
        if (currentOS != null)
        {
            return currentOS;
        }

        //        int lineNumber = 69;

        String osName = System.getProperty("os.name");
        //        String osArch = System.getProperty("os.arch");
        if (osName.contains("Windows"))
        {
            currentOS = OS.WINDOWS;
        }
        else if (osName.contains("Mac") || osName.contains("Darwin"))
        {
            currentOS = OS.MAC;
        }
        else if (osName.contains("Linux"))
        {
            currentOS = OS.LINUX;
        }
        else
        {
            currentOS = OS.UN_KNOW;
        }
        return currentOS;
    }

    private static boolean isRunByJar;
    private static boolean isRunByJarGet;

    /**
     * @return 是否是Jar形式运行
     */
    public static boolean isRunByJar()
    {
        if (isRunByJarGet)
        {
            return isRunByJar;
        }

        String property = System.getProperty("sun.java.command");

        try
        {
            Class<?> aClass = Class.forName(property);
            String protocol = Objects.requireNonNull(aClass.getResource("")).getProtocol();
            if (protocol.equals("jar"))
            {
                isRunByJar = true;
            }
        }
        catch (ClassNotFoundException e)
        {
            //如果对应的类不存在，说明是jar启动
            isRunByJar = true;
        }
        isRunByJarGet = true;

        return isRunByJar;
    }

    //</editor-fold>

    public String packBold(String str)
    {
        if (currentOS() == OS.WINDOWS && isRunByJar())
        {
            return str;
        }
        else
        {
            return "\u001b[1;" + value + "m" + str + end;
        }
    }
}
