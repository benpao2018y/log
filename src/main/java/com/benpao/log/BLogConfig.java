package com.benpao.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("unused")
public final class BLogConfig
{
    private static class ErrStream extends PrintStream
    {
        PBColor pbColor = PBColor.PURPLE;

        public ErrStream()
        {
            super(new ByteArrayOutputStream());
        }

        @Override
        public void print(boolean b)
        {
            System.out.print(pbColor.packBold(String.valueOf(b)));
        }

        @Override
        public void print(char c)
        {
            System.out.print(pbColor.packBold(String.valueOf(c)));
        }

        @Override
        public void print(int i)
        {
            System.out.print(pbColor.packBold(String.valueOf(i)));
        }

        @Override
        public void print(long l)
        {
            System.out.print(pbColor.packBold(String.valueOf(l)));
        }

        @Override
        public void print(float f)
        {
            System.out.print(pbColor.packBold(String.valueOf(f)));
        }

        @Override
        public void print(double d)
        {
            System.out.print(pbColor.packBold(String.valueOf(d)));
        }

        @Override
        public void print(char[] s)
        {
            System.out.print(pbColor.packBold(String.valueOf(s)));
        }

        @Override
        public void print(String s)
        {
            System.out.print(pbColor.packBold(s));
        }

        @Override
        public void print(Object obj)
        {
            System.out.print(pbColor.packBold(String.valueOf(obj)));
        }

        @Override
        public void println()
        {
            System.out.println();
        }

        @Override
        public void println(boolean x)
        {
            System.out.println(pbColor.packBold(String.valueOf(x)));
        }

        @Override
        public void println(char x)
        {
            System.out.println(pbColor.packBold(String.valueOf(x)));
        }

        @Override
        public void println(int x)
        {
            System.out.println(pbColor.packBold(String.valueOf(x)));
        }

        @Override
        public void println(long x)
        {
            System.out.println(pbColor.packBold(String.valueOf(x)));
        }

        @Override
        public void println(float x)
        {
            System.out.println(pbColor.packBold(String.valueOf(x)));
        }

        @Override
        public void println(double x)
        {
            System.out.println(pbColor.packBold(String.valueOf(x)));
        }

        @Override
        public void println(char[] x)
        {
            System.out.println(pbColor.packBold(String.valueOf(x)));
        }

        @Override
        public void println(String x)
        {
            System.out.println(pbColor.packBold(String.valueOf(x)));
        }

        @Override
        public void println(Object x)
        {
            System.out.println(pbColor.packBold(String.valueOf(x)));
        }

        @Override
        public PrintStream printf(String format, Object... args)
        {
            System.out.printf(format,args);
            return this;
        }

        @Override
        public PrintStream printf(Locale l, String format, Object... args)
        {
            System.out.printf(l,format,args);
            return this;
        }

        @Override
        public PrintStream format(String format, Object... args)
        {
            System.out.format(format, args);
            return this;
        }

        @Override
        public PrintStream format(Locale l, String format, Object... args)
        {
            System.out.format(l,format, args);
            return this;
        }

        @Override
        public PrintStream append(CharSequence csq)
        {
            System.out.append(csq);
            return this;
        }

        @Override
        public PrintStream append(CharSequence csq, int start, int end)
        {
            System.out.append(csq,start,end);
            return this;
        }

        @Override
        public PrintStream append(char c)
        {
            System.out.append(c);
            return this;
        }

        @Override
        public void flush()
        {
            System.out.flush();
        }

        @Override
        public void close()
        {
            System.out.close();
        }

        @Override
        public boolean checkError()
        {
            return System.out.checkError();
        }

        @Override
        public void write(int b)
        {
            System.out.write(b);
        }

        @Override
        public void write(byte[] buf, int off, int len)
        {
            System.out.write(buf, off, len);
        }

        @Override
        public void write(byte[] b) throws IOException
        {
            System.out.write(b);
        }
    }
    private static final PrintStream systemOldErr = System.err;
    private static final PrintStream errPrintStream = new ErrStream();
    private static volatile boolean isAdapterSystemErr;
    public static void adapterSystemErr(){
        if (!isAdapterSystemErr){
            System.setErr(errPrintStream);
            isAdapterSystemErr = true;
        }
    }

    public static void unAdapterSystemErr(){
        if (isAdapterSystemErr){
            System.setErr(systemOldErr);
            isAdapterSystemErr = false;
        }
    }

    static {
        adapterSystemErr();
    }

    private static final SimpleDateFormat DATE_FORMAT_Y_M_D_H_M_S = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static IBLogCore defaultBLog = bLogInfo ->
    {
        PBColor pbColor = bLogInfo.getType().getPbColor();

        System.out.println(pbColor.pack(bLogInfo.getMethodPath() + "("+bLogInfo.getJavaFileName()+":"+bLogInfo.getLineNum()+")") + " " + pbColor.pack(DATE_FORMAT_Y_M_D_H_M_S.format(new Date()) + " " + pbColor.packBold("[" + bLogInfo.getType().name() + "]")) + "\r\n" + bLogInfo.getMsg());
    };

    public static void setDefaultBLog(IBLogCore defaultBLog)
    {
        BLogConfig.defaultBLog = defaultBLog;
    }

    public static IBLogCore getDefaultBLog()
    {
        return defaultBLog;
    }
}
