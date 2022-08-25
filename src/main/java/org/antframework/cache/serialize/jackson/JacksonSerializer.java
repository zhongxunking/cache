/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-25 12:47 创建
 */
package org.antframework.cache.serialize.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import org.antframework.cache.common.Exceptions;
import org.antframework.cache.serialize.Serializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Jackson序列化器
 */
@AllArgsConstructor
public class JacksonSerializer implements Serializer {
    /**
     * Jackson
     */
    public static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.setTimeZone(TimeZone.getDefault());
        OBJECT_MAPPER.setLocale(Locale.getDefault());
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
    }

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

        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            return Exceptions.rethrow(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) {
        if (bytes == null) {
            return null;
        }
        if (type == byte[].class) {
            return (T) Arrays.copyOf(bytes, bytes.length);
        }

        try {
            return OBJECT_MAPPER.readValue(bytes, type);
        } catch (IOException e) {
            return Exceptions.rethrow(e);
        }
    }
}
