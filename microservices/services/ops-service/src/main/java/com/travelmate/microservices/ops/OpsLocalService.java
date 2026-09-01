package com.travelmate.microservices.ops;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.SysLog;
import com.travelmate.entity.SysSensitiveWord;
import com.travelmate.mapper.SysLogMapper;
import com.travelmate.mapper.SysSensitiveWordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpsLocalService {
    private static final int MAX_PAGE_SIZE = 100;
    private final SysSensitiveWordMapper sensitiveWordMapper;
    private final SysLogMapper logMapper;

    public OpsLocalService(SysSensitiveWordMapper sensitiveWordMapper, SysLogMapper logMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
        this.logMapper = logMapper;
    }

    public List<SysSensitiveWord> listSensitiveWords() {
        return sensitiveWordMapper.selectList(new LambdaQueryWrapper<SysSensitiveWord>()
                .orderByDesc(SysSensitiveWord::getCreateTime));
    }

    public SysSensitiveWord addSensitiveWord(String rawWord, Integer level, Long adminId) {
        String word = rawWord == null ? "" : rawWord.trim();
        if (word.isEmpty()) throw new RuntimeException("敏感词不能为空");
        if (word.length() > 100) throw new RuntimeException("敏感词不能超过100个字符");
        if (sensitiveWordMapper.selectCount(new LambdaQueryWrapper<SysSensitiveWord>()
                .eq(SysSensitiveWord::getWord, word)) > 0) throw new RuntimeException("敏感词已存在");
        SysSensitiveWord entity = new SysSensitiveWord();
        entity.setWord(word);
        entity.setLevel(level == null ? 1 : Math.max(1, Math.min(level, 3)));
        entity.setCreateTime(LocalDateTime.now());
        sensitiveWordMapper.insert(entity);
        log(adminId, "新增敏感词: " + word, 1, null);
        return entity;
    }

    public void deleteSensitiveWord(Long id, Long adminId) {
        if (sensitiveWordMapper.deleteById(id) == 0) throw new RuntimeException("敏感词不存在");
        log(adminId, "删除敏感词: " + id, 1, null);
    }

    public boolean containsSensitiveWord(String content) {
        if (content == null || content.isBlank()) return false;
        return sensitiveWordMapper.selectList(null).stream()
                .map(SysSensitiveWord::getWord).filter(word -> word != null && !word.isBlank())
                .anyMatch(content::contains);
    }

    public Map<String, Object> logs(int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        List<SysLog> all = logMapper.selectList(new LambdaQueryWrapper<SysLog>().orderByDesc(SysLog::getCreateTime));
        int from = Math.min((normalizedPage - 1) * normalizedSize, all.size());
        int to = Math.min(from + normalizedSize, all.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", all.subList(from, to));
        result.put("total", all.size());
        result.put("page", normalizedPage);
        result.put("size", normalizedSize);
        return result;
    }

    public void log(Long userId, String operation, int status, String error) {
        SysLog log = new SysLog();
        log.setUserId(userId);
        log.setOperation(operation);
        log.setStatus(status);
        log.setErrorMsg(error);
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }
}
