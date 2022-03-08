package com.mszlu.rpc.netty.codec;

import com.mszlu.rpc.constant.MsRpcConstants;
import com.mszlu.rpc.exception.MsRpcException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 *   |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 *
 * 以魔法数开头4个bit传过来的才是我们TCP协议的传输过来的，其他的无法解析
 */
public class MsRpcDecoder extends LengthFieldBasedFrameDecoder {

    public MsRpcDecoder(){
        this(8 * 1024 * 1024,5,4,-9,0);
    }


    /**
     *
     * @param maxFrameLength 最大帧长度。它决定可以接收的数据的最大长度。如果超过，数据将被丢弃,根据实际环境定义
     * @param lengthFieldOffset 数据长度字段开始的偏移量, magic code+version=长度为5
     * @param lengthFieldLength 消息长度的大小  full length（消息长度） 长度为4
     * @param lengthAdjustment 补偿值 lengthAdjustment+数据长度取值=长度字段之后剩下包的字节数(x + 16=7 so x = -9)
     * @param initialBytesToStrip 忽略的字节长度，如果要接收所有的header+body 则为0，如果只接收body 则为header的长度 ,我们这为0
     */
    public MsRpcDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    //数据发送过来后，先进入这里面，进行解码
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {

        Object decode = super.decode(ctx, in);
        if (decode instanceof ByteBuf){
            ByteBuf frame = (ByteBuf) decode;
            int length = frame.readableBytes();
             //  magic   code        |version | full length         | messageType| codec|compress|    RequestId
            //前面这部分已经16个字节，如果我们获取的小于16个字节，那我们就不能接收
            if(length < MsRpcConstants.TOTAL_LENGTH){
                throw new MsRpcException("数据长度不符");
            }
            return decodeFrame(frame);
        }
        return decode;
    }

    //1. 4B  magic code（魔法数）
    //2. 1B version（版本）
    //3. 4B full length（消息长度）
    //4. 1B messageType（消息类型）
    //5. 1B codec（序列化类型）
    //6. 1B compress（压缩类型）
    //7. 4B  requestId（请求的Id）
    //8. body（object类型数据）
    private Object decodeFrame(ByteBuf frame) {
        //按照顺序进行读取  magic   code        |version | full length         | messageType| codec|compress|    RequestId
        //这里的readBytes就是将我们字节向后读取

        //检测魔法数
        checkMagicCode(frame);
        //检测版本
        checkVersion(frame);
        //这边使用readInt是因为Int占用4个字节，数据长度刚好就是4个字节
        //3.数据长度
        int fullLength = frame.readInt();
        //4.messageType 消息类型
        byte messageType = frame.readByte();
        //5.序列化类型
        byte codecType = frame.readByte();
        //6.压缩类型
        byte compressType = frame.readByte();
        //7. 请求id
        int requestId = frame.readInt();
        //8. 读取数据
        int dataLength = fullLength - MsRpcConstants.TOTAL_LENGTH;
        if(dataLength > 0 ){
            //有数据。读取body的数据
            byte[] bodyData =  new byte[dataLength];
            frame.readBytes(bodyData);
            //在进行编码的时候，先序列化 后压缩 -> 解压缩 -》 反序列化
            //解压缩 使用gzip
            //反序列化
        }
        return null;
    }

    private void checkVersion(ByteBuf frame) {
        byte b = frame.readByte();
        if(b != MsRpcConstants.VERSION){
            throw new MsRpcException("未知的version");
        }
    }

    private void checkMagicCode(ByteBuf frame) {
        int length = MsRpcConstants.MAGIC_NUMBER.length;
        byte[] tmp = new byte[length];
        frame.readBytes(frame);
        //这里就是为了判断前缀的魔法数是否相同，如果不相同那么就报错
        //魔法数就是一个前缀，就是标注这个消息是我们产生的
        for (int i = 0; i < length; i++) {
            if(tmp[i] != MsRpcConstants.MAGIC_NUMBER[i]){
                throw new MsRpcException("传递的魔法数有误");
            }
        }


    }

}

