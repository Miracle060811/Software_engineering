package com.travelmate.service.impl;

import com.travelmate.entity.SysSensitiveWord;
import com.travelmate.mapper.SysSensitiveWordMapper;
import com.travelmate.service.SensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    @Autowired
    private SysSensitiveWordMapper sensitiveWordMapper;

    @Override
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        List<SysSensitiveWord> words = sensitiveWordMapper.selectList(null);
        for (SysSensitiveWord sw : words) {
            if (sw.getWord() != null && text.contains(sw.getWord())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        List<String> words = sensitiveWordMapper.selectList(null).stream()
                .map(SysSensitiveWord::getWord)
                .filter(w -> w != null && !w.isEmpty())
                .collect(Collectors.toList());
        String result = text;
        for (String word : words) {
            result = result.replace(word, "***");
        }
        return result;
    }
}
