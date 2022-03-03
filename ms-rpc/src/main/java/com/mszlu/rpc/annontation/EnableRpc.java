package com.mszlu.rpc.annontation;

import com.mszlu.rpc.beans.MsBeanDefinitionRegistry;
import com.mszlu.rpc.sping.MsRpcSpringBeanPostProcessor;
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
}
