package com.benpao.compiler;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.ProcessingEnvironment;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public abstract class PABJavacTask extends TreeTranslator implements TaskListener
{
    //<editor-fold desc="封装一个简单的文件日志输出">
    private static File logFile;
    private static final Object logFileLock = new Object();
    private static class FileLog
    {
        static ExecutorService logExecutorService = Executors.newSingleThreadExecutor();
        static AtomicBoolean isNew = new AtomicBoolean(true);
        public static void logFile(Object o){
            synchronized (logFileLock){
                if (logFile == null){
                    return;
                }
            }
            logExecutorService.execute(() -> writeLog(o, !isNew.getAndSet(false)));
        }

        static SimpleDateFormat DATE_FORMAT_Y_M_D_H_M_S = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        static void writeLog(Object o,boolean append){
            try
            {
                FileWriter fileWriter = new FileWriter(logFile,append);
                if (!append)
                {
                    fileWriter.write("[日志 " + DATE_FORMAT_Y_M_D_H_M_S.format(new Date()) + "]");
                }
                fileWriter.write("\r\n\r\n");
                fileWriter.write(o.toString());
                fileWriter.flush();
                fileWriter.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    //</editor-fold>
    protected JavacTask task;
    private final Log log;
    protected final AtomicInteger logIndex = new AtomicInteger(1);
    protected TreeMaker treeMaker;
    protected Names names;

    protected final ProcessingEnvironment processingEnv;
    protected Map<String, String> options;
    public PABJavacTask(JavacTask task)
    {
        if (task instanceof BasicJavacTask)
        {
            Context context = ((BasicJavacTask) task).getContext();
            log = Log.instance(context);
            //<editor-fold desc="创建语句的关键对象">
            treeMaker = TreeMaker.instance(context);
            //</editor-fold>

            names = Names.instance(context);

            names.fromString("");

            processingEnv = JavacProcessingEnvironment.instance(context);
            options = processingEnv.getOptions();

            String optionValue = getOptionValue(PBFields.LOG_FILE_SRC);
            if (optionValue != null){
                synchronized (logFileLock){
                    if (logFile == null){
                        logFile = new File(optionValue);
                    }
                }
            }
        }
        else
        {
            log = null;
            treeMaker = null;
            names = null;
            processingEnv = null;
        }
    }

    protected String throwableToString(Throwable e)
    {
        StringBuilder str = new StringBuilder(e.toString());
        for (StackTraceElement stackTraceElement : e.getStackTrace())
        {
            str.append("\r\n")
                    .append("\tat ")
                    .append(stackTraceElement.toString());
        }
        return str.toString();
    }

    protected String getOptionValue(String optionKey){
        if (options.containsKey(optionKey)){
            return options.get(optionKey);
        }
        return null;
    }

    protected boolean getOptionBoolValue(String optionKey){
        String optionValue = getOptionValue(optionKey);
        if (optionValue != null){
            return optionValue.equalsIgnoreCase("true") || optionValue.equals("1");
        }

        return false;
    }

    protected void log(Object s)
    {
        if (log != null)
        {
            //<editor-fold desc="Log.WriterKind.NOTICE 和别的类型的输出表现完全一样">
            log.printRawLines(Log.WriterKind.NOTICE, "[PBLogPlugin:" + logIndex.getAndAdd(1) + "] => " + s);
            //</editor-fold>
        }

        FileLog.logFile(s);
    }
}
