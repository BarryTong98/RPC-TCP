package com.mszlu.rpc.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class MsRequest implements Serializable {
    //请求的id
    private String requestId;
    //接口的名称 例如ConsumerController里面的GoodsService -> find
    private String interfaceName;
    //方法的名称
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    //版本号
    private String version;
    //如果在不同的组内，不能互相通信
    private String group;
}

