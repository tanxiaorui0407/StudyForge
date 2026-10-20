package com.studyforge.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.studyforge.config.StudyForgeProperties;
import com.studyforge.mapper.DocumentMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 文件哈希缓存：相同内容的文件复用历史成功结果，不重复烧 token。
 * 两级：Caffeine（内存快查）+ MySQL（跨重启持久）。
 */
@Service
public class CacheService {

    private final Cache<String, Long> fileHashCache;
    private final DocumentMapper documentMapper;
    private final StudyForgeProperties props;

    public CacheService(Cache<String, Long> fileHashCache, DocumentMapper documentMapper,
                        StudyForgeProperties props) {
        this.fileHashCache = fileHashCache;
        this.documentMapper = documentMapper;
        this.props = props;
    }

    /** 是否开启缓存复用 */
    public boolean enabled() {
        return props.getCache().isEnabled();
    }

    /** 查询某哈希是否已有成功处理过的文档 */
    public Optional<Long> findReusableDocumentId(String hash) {
        if (!enabled()) {
            return Optional.empty();
        }
        Long cached = fileHashCache.getIfPresent(hash);
        if (cached != null && cached > 0) {
            return Optional.of(cached);
        }
        Long fromDb = documentMapper.findSucceededIdByHash(hash);
        if (fromDb != null) {
            fileHashCache.put(hash, fromDb);
            return Optional.of(fromDb);
        }
        fileHashCache.put(hash, -1L);
        return Optional.empty();
    }
}
