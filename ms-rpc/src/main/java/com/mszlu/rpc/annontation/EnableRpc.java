package com.mszlu.rpc.annontation;

import com.mszlu.rpc.spring.MsRpcSpringBeanPostProcessor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(MsRpcSpringBeanPostProcessor.class)
//EnableHttpClient是为了http调用
//EnableRpc是为了Rpc TCP调用
//这个地方目前在我们的应用里只引用里一次，因为import里一次
//如果我们要多个地方使用，我们在Import那边要导入多个
//导入多个会有对应的风险，spring默认都是单例的管理
public @interface EnableRpc {
    //这里是为了将nacos部署集群
    //nacos主机名
    String nacosHost() default "locahost";

    //nacos端口号
    int nacosPort() default 8848;

    //nacos组，同一个组内 互通，并且组成集群
    String nacosGroup() default "ms-rpc-group";

    //server服务端口
    int serverPort() default 13567;


}
