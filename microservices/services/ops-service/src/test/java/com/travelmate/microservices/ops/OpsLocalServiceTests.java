package com.travelmate.microservices.ops;

import com.travelmate.entity.SysSensitiveWord;
import com.travelmate.mapper.SysLogMapper;
import com.travelmate.mapper.SysSensitiveWordMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpsLocalServiceTests {
    @Test
    void updatesSensitiveWordWithoutTreatingItselfAsDuplicate() {
        SysSensitiveWordMapper mapper = mock(SysSensitiveWordMapper.class);
        SysSensitiveWord entity = new SysSensitiveWord(); entity.setId(6L); entity.setWord("旧词"); entity.setLevel(1);
        when(mapper.selectById(6L)).thenReturn(entity);
        when(mapper.selectCount(any())).thenReturn(0L);
        OpsLocalService service = new OpsLocalService(mapper, mock(SysLogMapper.class));

        service.updateSensitiveWord(6L, " 新风险词 ", 3, 1L);

        assertEquals("新风险词", entity.getWord());
        assertEquals(3, entity.getLevel());
        verify(mapper).updateById(entity);
    }

    @Test
    void rejectsDuplicateSensitiveWord() {
        SysSensitiveWordMapper mapper = mock(SysSensitiveWordMapper.class);
        SysSensitiveWord entity = new SysSensitiveWord(); entity.setId(6L); entity.setWord("旧词"); entity.setLevel(1);
        when(mapper.selectById(6L)).thenReturn(entity);
        when(mapper.selectCount(any())).thenReturn(1L);
        OpsLocalService service = new OpsLocalService(mapper, mock(SysLogMapper.class));

        assertThrows(RuntimeException.class, () -> service.updateSensitiveWord(6L, "重复词", 2, 1L));
    }
}
