/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-26 21:34 创建
 */
package org.antframework.cache.serialize.hessian;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import lombok.AllArgsConstructor;
import org.antframework.cache.common.Exceptions;
import org.antframework.cache.serialize.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Hessian序列化器
 */
@AllArgsConstructor
public class HessianSerializer implements Serializer {
    // 名称
    private final String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof byte[]) {
            return Arrays.copyOf((byte[]) obj, ((byte[]) obj).length);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Hessian2Output hessian2Output = new Hessian2Output(out);
        try {
            hessian2Output.writeObject(obj);
            hessian2Output.close();
        } catch (IOException e) {
            return Exceptions.rethrow(e);
        }

        return out.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) {
        if (bytes == null) {
            return null;
        }
        if (type == byte[].class) {
            return (T) Arrays.copyOf(bytes, bytes.length);
        }

        Object value;

        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        Hessian2Input hessian2Input = new Hessian2Input(in);
        try {
            value = hessian2Input.readObject();
            hessian2Input.close();
        } catch (IOException e) {
            return Exceptions.rethrow(e);
        }

        return type.cast(value);
    }
}
