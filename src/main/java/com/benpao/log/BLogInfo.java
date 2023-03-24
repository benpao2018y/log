package com.benpao.log;

public class BLogInfo
{
    @SuppressWarnings("unused")
    public enum Type
    {
        Debug(PBColor.BLUE), Info(PBColor.GREEN), Error(PBColor.RED), Warn(PBColor.YELLOW);

        private final PBColor pbColor;
        Type(PBColor pbColor){
            this.pbColor = pbColor;
        }

        public PBColor getPbColor()
        {
            return pbColor;
        }
    }

    private final Type type;
    private final Object msg;
    private final String methodPath;
    private final String javaFileName;
    private final int lineNum;


    public BLogInfo(Type type,Object msg,String methodPath,String javaFileName,int lineNum){
        this.type = type;
        this.msg = msg;
        this.methodPath = methodPath;
        this.javaFileName = javaFileName;
        this.lineNum = lineNum;
    }

    public Type getType()
    {
        return type;
    }

    public Object getMsg()
    {
        return msg;
    }

    public String getMethodPath()
    {
        return methodPath;
    }

    public String getJavaFileName()
    {
        return javaFileName;
    }

    public int getLineNum()
    {
        return lineNum;
    }
}
