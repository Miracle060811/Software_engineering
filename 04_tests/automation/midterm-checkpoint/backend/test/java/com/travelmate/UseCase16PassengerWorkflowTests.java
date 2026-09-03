package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.travelmate.entity.Passenger;
import com.travelmate.mapper.PassengerMapper;
import com.travelmate.service.impl.PassengerServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UseCase16PassengerWorkflowTests {

    @Test
    void intTc116CreatesNormalizedPassengerForCurrentUser() {
        PassengerMapper mapper = mock(PassengerMapper.class);
        PassengerServiceImpl service = service(mapper);
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.insert(any(Passenger.class))).thenReturn(1);
        Passenger passenger = passenger("  张三  ", "e12345678", "13800138000");

        assertThat(service.addPassenger(passenger)).isTrue();
        assertThat(passenger.getName()).isEqualTo("张三");
        assertThat(passenger.getIdCard()).isEqualTo("E12345678");
        assertThat(passenger.getType()).isZero();
        assertThat(passenger.getCreateTime()).isNotNull();
    }

    @Test
    void unitTc116RejectsInvalidAndDuplicateCertificates() {
        PassengerMapper mapper = mock(PassengerMapper.class);
        PassengerServiceImpl service = service(mapper);

        assertThatThrownBy(() -> service.addPassenger(passenger("张三", "123", "13800138000")))
                .hasMessage("证件号格式无效");
        assertThatThrownBy(() -> service.addPassenger(passenger("张三", "E12345678", "123")))
                .hasMessage("手机号格式无效");

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertThatThrownBy(() -> service.addPassenger(passenger("张三", "E12345678", "13800138000")))
                .hasMessage("该证件旅客已存在");
        verify(mapper, never()).insert(any(Passenger.class));
    }

    @Test
    void unitTc116DeletesOnlyPassengerOwnedByUser() {
        PassengerMapper mapper = mock(PassengerMapper.class);
        PassengerServiceImpl service = service(mapper);
        when(mapper.delete(any(Wrapper.class))).thenReturn(1);

        assertThat(service.deletePassenger(88L, 7L)).isTrue();
        verify(mapper).delete(any(Wrapper.class));
    }

    private PassengerServiceImpl service(PassengerMapper mapper) {
        PassengerServiceImpl service = new PassengerServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        return service;
    }

    private Passenger passenger(String name, String idCard, String phone) {
        Passenger passenger = new Passenger();
        passenger.setUserId(7L);
        passenger.setName(name);
        passenger.setIdCard(idCard);
        passenger.setPhone(phone);
        return passenger;
    }
}
