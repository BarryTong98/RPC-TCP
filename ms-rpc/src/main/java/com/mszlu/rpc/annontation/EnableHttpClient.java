package com.mszlu.rpc.annontation;

import com.mszlu.rpc.beans.MsBeanDefinitionRegistry;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(MsBeanDefinitionRegistry.class)
//这个是因为当consumer调用的过程中，不能使用@ComponentScan注解扫描到ms-rpc下的包
//这个主要功能就是扫包，让client 可以调用到provicer里面的function
/*
*  Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(EnableHttpClient.class.getCanonicalName());
   找到Enable注解，获取其中的basePackage属性，此属性标明了@MsHttpClient所在的包
   Object basePackage = annotationAttributes.get("basePackage");
* */
public @interface EnableHttpClient {
    //扫包路径
    String basePackage();

}
