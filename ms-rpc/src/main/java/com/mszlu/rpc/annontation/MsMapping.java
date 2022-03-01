package com.mszlu.rpc.annontation;

import java.lang.annotation.*;

//METHOD代表放在方法上，仅用METHOD不能用于类型
//目前这个注解使用在GoodsHttpRpc中的Goods findGoods(Long id); 所以使用在方法上
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface MsMapping {
    //API路径
    String api() default "";
    //调用的主机和端口
    String url() default "";
}
