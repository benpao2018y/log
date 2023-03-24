package com.benpao.log;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;

public class PBLogPlugin implements Plugin
{
    @Override
    public String getName()
    {
        return "PBLogPlugin";
    }

    @Override
    public void init(JavacTask task, String... args)
    {
        task.addTaskListener(new PBLogCompiler(task));
    }
}
