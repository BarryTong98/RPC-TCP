package com.mszlu.rpc.message;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MsResponse<T> implements Serializable {

    private String requestId;
    /**
     * response code
     */
    private Integer code;
    /**
     * response message
     */
    private String message;
    /**
     * response body
     */
    private T data;


    public static <T> MsResponse<T> success(T data, String requestId) {
        MsResponse<T> response = new MsResponse<>();
        //定义200代表成功
        response.setCode(200);
        response.setMessage("success");
        //这里的返回相同的requestId可以将请求和返回映射起来
        response.setRequestId(requestId);
        if (null != data) {
            response.setData(data);
        }
        return response;
    }

    public static <T> MsResponse<T> fail(String message) {
        MsResponse<T> response = new MsResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

}

