/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-07 15:50 创建
 */
package org.antframework.cache.boot.annotation;

import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheableOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 强制@Cacheable(sync=true)，迫使Spring通过org.springframework.cache.Cache#get(java.lang.Object, java.util.concurrent.Callable)方法获取缓存
 */
public class ForcedSyncAnnotationCacheOperationSource extends AnnotationCacheOperationSource {
    @Override
    protected Collection<CacheOperation> determineCacheOperations(CacheOperationProvider provider) {
        Collection<CacheOperation> operations = super.determineCacheOperations(provider);
        if (operations == null) {
            return null;
        }
        List<CacheOperation> forcedOperations = new ArrayList<>(operations.size());
        for (CacheOperation operation : operations) {
            if (operation instanceof CacheableOperation) {
                forcedOperations.add(convertForcedSync((CacheableOperation) operation));
            } else {
                forcedOperations.add(operation);
            }
        }
        return forcedOperations;
    }

    // 将CacheableOperation的sync标记强制转换为true
    private CacheableOperation convertForcedSync(CacheableOperation operation) {
        CacheableOperation.Builder builder = new CacheableOperation.Builder();

        builder.setName(operation.getName());
        builder.setCacheNames(operation.getCacheNames().toArray(new String[0]));
        builder.setCondition(operation.getCondition());
        builder.setUnless(operation.getUnless());
        builder.setKey(operation.getKey());
        builder.setKeyGenerator(operation.getKeyGenerator());
        builder.setCacheManager(operation.getCacheManager());
        builder.setCacheResolver(operation.getCacheResolver());
        // 强制sync为true
        builder.setSync(true);

        return builder.build();
    }
}
