package com.mszlu.rpc.annontation;

import java.lang.annotation.*;
//Type代表可以放在类和接口上面
//RUNTIME 运行时
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface MsHttpClient {
    //必须填 这个地方就是对应的Bean名称
    String value();
}
