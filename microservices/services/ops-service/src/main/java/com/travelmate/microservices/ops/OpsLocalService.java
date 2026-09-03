package com.travelmate.microservices.ops;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.SysLog;
import com.travelmate.entity.SysSensitiveWord;
import com.travelmate.mapper.SysLogMapper;
import com.travelmate.mapper.SysSensitiveWordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    public Map<String, Object> dashboardMetrics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recentStart = now.minusMinutes(15).withSecond(0).withNano(0);
        List<SysLog> recentLogs = logMapper.selectList(new LambdaQueryWrapper<SysLog>()
                .ge(SysLog::getCreateTime, recentStart));
        List<SysLog> recentErrors = logMapper.selectList(new LambdaQueryWrapper<SysLog>()
                .eq(SysLog::getStatus, 0)
                .orderByDesc(SysLog::getCreateTime)
                .last("LIMIT 10"));
        long errorLogsToday = logMapper.selectCount(new LambdaQueryWrapper<SysLog>()
                .eq(SysLog::getStatus, 0)
                .ge(SysLog::getCreateTime, LocalDate.now().atStartOfDay()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("onlineUsers", recentLogs.stream()
                .filter(log -> log.getCreateTime() != null && !log.getCreateTime().isBefore(now.minusMinutes(15)))
                .map(SysLog::getUserId).filter(Objects::nonNull).distinct().count());
        result.put("qpsTrend", buildLogTrend(recentLogs, now, false));
        result.put("latencyTrend", buildLogTrend(recentLogs, now, true));
        result.put("recentErrors", recentErrors.stream().map(this::errorRow).toList());
        result.put("errorLogsToday", errorLogsToday);
        return result;
    }

    private List<Map<String, Object>> buildLogTrend(List<SysLog> logs, LocalDateTime now, boolean latency) {
        LocalDateTime start = now.minusMinutes(11).withSecond(0).withNano(0);
        Map<LocalDateTime, List<SysLog>> grouped = logs.stream()
                .filter(log -> log.getCreateTime() != null && !log.getCreateTime().isBefore(start))
                .collect(Collectors.groupingBy(log -> log.getCreateTime().withSecond(0).withNano(0)));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int offset = 0; offset < 12; offset++) {
            LocalDateTime minute = start.plusMinutes(offset);
            List<SysLog> bucket = grouped.getOrDefault(minute, List.of());
            long value = latency
                    ? Math.round(bucket.stream().map(SysLog::getTimeMs).filter(Objects::nonNull)
                            .mapToLong(Long::longValue).average().orElse(0D))
                    : bucket.size();
            trend.add(Map.of("time", minute.format(DateTimeFormatter.ofPattern("HH:mm")), "value", value));
        }
        return trend;
    }

    private Map<String, Object> errorRow(SysLog log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("method", log.getMethod());
        row.put("errorMsg", log.getErrorMsg());
        row.put("timeMs", log.getTimeMs());
        row.put("createTime", log.getCreateTime());
        return row;
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
