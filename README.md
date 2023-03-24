# 依赖:
```xml
<dependency>
    <groupId>com.sun</groupId>
    <artifactId>tools</artifactId>
    <version>1.8</version>
    <scope>system</scope>
    <systemPath>${java.home}/../lib/tools.jar</systemPath>
</dependency>
```

# 插件参数配置
- ALogFileSrc：编译时的日志输出路径,可以不设置,不设置就不输出
- ASrcRootDir：项目源文件跟目录,最好设置,不设置可能会在某些极端情况下造成编译时信息丢失
- AReadAllSrcRootDir: 编译时是否预读取全部源文件，默认注销，当一个Java文件种有两个独立的类的时候低概率会出现信息丢失，如果没有这样的写法，完全可以注销这个参数，A类里面嵌套了B类这种不算两个独立的类.
- ACloseDebug：关闭Debug类输出
- ACloseError：关闭Error类输出
- ACloseInfo：关闭Info类输出
- ACloseWarn：关闭Warn类输出
- AShowCompiledResult：编译时的日志中是否输出每个被编译的类的编译后的源码

###AReadAllSrcRootDir，ACloseDebug，ACloseError，ACloseInfo，ACloseWarn,AShowCompiledResult 这几个参数属于布尔型，明确赋值为1或者true才是真，别的任何形式，包括注销都是否
``` xml
<project>

<build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <compilerArgs>
                        <arg>-Xplugin:PBLogPlugin</arg>
                        <arg>-ALogFileSrc=/XXXXXXXX/test/log.txt
                        </arg>
                        <arg>-ASrcRootDir=/XXXXXXXX/test/src/main/java
                        </arg>
                        <!--<arg>-AReadAllSrcRootDir=1</arg>-->
                        <!--<arg>-ACloseDebug=1</arg>-->
                        <!--<arg>-ACloseError=1</arg>-->
                        <!--<arg>-ACloseInfo=1</arg>-->
                        <!--<arg>-ACloseWarn=1</arg>-->
                        <arg>-AShowCompiledResult=1</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```
- 使用例子：静态调用
```java
import com.benpao.log.IBLog;

import static com.benpao.log.IBLog.*;

class Main
{
    public static void main(String[] args)
    {
        //<editor-fold desc="引入静态方法使用">
        debug("debug信息");
        info("Info");
        warn("warn");
        error("error");
        //</editor-fold>

        //<editor-fold desc="静态调用">
        IBLog.debug("warn");
        IBLog.info("info");
        IBLog.warn("warn");
        IBLog.error("error");
        //</editor-fold>
    }
}
```
- 使用例子：实现接口调用

```java
import com.benpao.log.IBLog;

class Test implements IBLog{
    //只要类实现一下com.benpao.log.IBLog接口就可以使用它的成员日志方法,不需要额外实现任何东西
    void testMethod(){
        logDebug("logDebug");
        logInfo("logInfo");
        logWarn("logWarn");
        logError("logError");
    }
}
```
- 使用例子: 继承实现了接口的父类

```java
import com.benpao.log.IBLog;

class TestA implements IBLog{
    
}

class TestB extends TestA{
    void testMethod(){
        logDebug("logDebug");
        logInfo("logInfo");
        logWarn("logWarn");
        logError("logError");
    }
}
```
# 注意事项:
任何类或者它的父类重写了对应的几个方法，那么将不再编译处理