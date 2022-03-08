package com.mszlu.rpc.compress;

import com.mszlu.rpc.constant.CompressTypeEnum;
import com.mszlu.rpc.exception.MsRpcException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipCompress implements Compress{
    @Override
    public String name() {
        return CompressTypeEnum.GZIP.getName();
    }

    @Override
    public byte[] compress(byte[] bytes) {
        //进行对应的压缩
        if (bytes == null){
            throw new NullPointerException("传入的压缩数据为null");
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            //通过GZIP输出溜
            GZIPOutputStream gzip = new GZIPOutputStream(os);
            //读进去
            gzip.write(bytes);
            //进行释放
            gzip.flush();
            gzip.finish();
            //生成字节数组
            return os.toByteArray();
        } catch (IOException e) {
            throw new MsRpcException("压缩数据出错",e);
        }
    }

    @Override
    public byte[] decompress(byte[] bytes) {
        if (bytes == null){
            throw new NullPointerException("传入的解压缩数据为null");
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            //将字节数组读进去
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
            byte[] buffer = new byte[1024 * 4];
            int n;
            //这边大于-1代表有值
            while ((n = gzipInputStream.read(buffer)) > -1){
                os.write(buffer,0,n);
            }
            return os.toByteArray();
        } catch (IOException e) {
            throw new MsRpcException("解压缩数据出错",e);
        }
    }
}
