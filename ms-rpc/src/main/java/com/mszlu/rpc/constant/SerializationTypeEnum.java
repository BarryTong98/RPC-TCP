package com.mszlu.rpc.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SerializationTypeEnum {
    PROTO_STUFF((byte)0x01, "protoStuff");

    private final byte code;
    private final String name;

    public static String getName(byte code){
        for(SerializationTypeEnum c : SerializationTypeEnum.values()){
            if(c.getCode() == code){
                return c.name;
            }
        }
        return null;
    }
}
