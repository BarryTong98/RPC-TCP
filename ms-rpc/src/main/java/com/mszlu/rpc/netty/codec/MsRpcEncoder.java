package com.mszlu.rpc.netty.codec;

import com.mszlu.rpc.compress.Compress;
import com.mszlu.rpc.constant.CompressTypeEnum;
import com.mszlu.rpc.constant.MsRpcConstants;
import com.mszlu.rpc.constant.SerializationTypeEnum;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

//编码就是不管在客户端发送还是在服务端发送，会先接收编码器编码
public class MsRpcEncoder extends MessageToByteEncoder<MsMessage> {
    //定义一个原子的Integer，然后进行一个自增的操作
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    //1. 4B  magic code（魔法数）
    //2. 1B version（版本）
    //3. 4B full length（消息长度）
    //4. 1B messageType（消息类型）
    //5. 1B codec（序列化类型）
    //6. 1B compress（压缩类型）
    //7. 4B  requestId（请求的Id）
    //8. body（object类型数据）
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext,
                          MsMessage msMessage,
                          ByteBuf out) throws Exception {
        //拿到message 要进行编码处理
        //要根据上面的格式
        //1. 4B  magic code（魔法数）
        out.writeBytes(MsRpcConstants.MAGIC_NUMBER);
        //2. 1B version（版本）
        out.writeByte(MsRpcConstants.VERSION);
        //3. 4B full length（消息长度）
        //这时候消息长度还是没有，我们需要预留数据长度位置
        out.writerIndex(out.writerIndex() + 4);
        //4. 1B messageType（消息类型）
        out.writeByte(msMessage.getMessageType());
        //5. 1B codec（序列化类型）
        //序列化 先进行序列化 再进行压缩
        out.writeByte(msMessage.getCodec());
        //6. 1B compress（压缩类型）
        out.writeByte(msMessage.getCompress());
        //这里的Request id
        out.writeByte(ATOMIC_INTEGER.getAndIncrement());

        Object data = msMessage.getData();
        //header 长度为16
        int fullLength = MsRpcConstants.HEAD_LENGTH;

        //序列化
        Serializer serializer = loadSerializer(msMessage.getCodec());
        byte[] bodyBytes = serializer.serialize(data);
        //压缩
        Compress compress = loadCompress(msMessage.getCompress());
        //压缩后需要的值
        bodyBytes = compress.compress(bodyBytes);
        //加上数据的长度
        fullLength += bodyBytes.length;
        out.writeBytes(bodyBytes);
        //将FullLength写到之前预留的位置
        int writeIndex = out.writerIndex();
        out.writerIndex(writeIndex - fullLength + MsRpcConstants.MAGIC_NUMBER.length + 1);
        out.writeInt(fullLength);
        out.writerIndex(writeIndex);
    }

    private Serializer loadSerializer(byte codec) {
        //通过对应的type拿到name
        String name = SerializationTypeEnum.getName(codec);
        //在这里name会有很多种压缩方式，如果我们使用if else进行多次判断，那么整个代码就会十分的繁杂
        //加载我们的压缩，在META-INF/services新建com.mszlu.rpc.compress.Compress 文件，其中的内容为 com.mszlu.rpc.compress.GzipCompress
        //他会对这个路径进行匹配，这个load其实就是一个列表，去列表中进行查询name
        ServiceLoader<Serializer> load = ServiceLoader.load(Serializer.class);
        for (Serializer serializer : load) {
            //name.equals(compress.name())不能这样子去使用，因为name可能等于null
            if (serializer.name().equals(name)) {
                return serializer;
            }
        }
        throw new MsRpcException("无对应的序列化类型");
    }

    private Compress loadCompress(byte compressType) {
        //通过对应的type拿到name
        String name = CompressTypeEnum.getName(compressType);
        //在这里name会有很多种压缩方式，如果我们使用if else进行多次判断，那么整个代码就会十分的繁杂
        //加载我们的压缩，在META-INF/services新建com.mszlu.rpc.compress.Compress 文件，其中的内容为 com.mszlu.rpc.compress.GzipCompress
        //他会对这个路径进行匹配，这个load其实就是一个列表，去列表中进行查询name
        ServiceLoader<Compress> load = ServiceLoader.load(Compress.class);
        for (Compress compress : load) {
            //name.equals(compress.name())不能这样子去使用，因为name可能等于null
            if (compress.name().equals(name)) {
                return compress;
            }
        }
        throw new MsRpcException("无对应的压缩类型");
    }
}
