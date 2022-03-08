package com.mszlu.rpc.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CompressTypeEnum {
    //读取协议这的压缩类型，来此枚举进行匹配
    //在这边 如果我们传递0x01 -> 我们就使用gzip压缩
    GZIP((byte) 0x01, "gzip");
    //还可以定义其他的类型

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        for (CompressTypeEnum c : CompressTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}

