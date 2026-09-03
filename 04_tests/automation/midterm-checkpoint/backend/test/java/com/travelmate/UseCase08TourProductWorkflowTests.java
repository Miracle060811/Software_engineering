package com.travelmate;

import com.travelmate.common.Result;
import com.travelmate.controller.TourProductController;
import com.travelmate.entity.TourProduct;
import com.travelmate.service.TourProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCase08TourProductWorkflowTests {

    private TourProductController controller;
    private TourProductService service;

    @BeforeEach
    void setUp() {
        controller = new TourProductController();
        service = mock(TourProductService.class);
        ReflectionTestUtils.setField(controller, "tourProductService", service);
    }

    @Test
    void intTc108ReturnsDayTourContractForRequestedCategory() {
        TourProduct product = new TourProduct();
        product.setId(11L);
        product.setName("西湖环湖一日游");
        product.setTourType(0);
        product.setDepartureCity("杭州");
        product.setDestination("西湖");
        product.setDuration("1天");
        product.setPrice(new BigDecimal("118.00"));
        product.setStatus(1);
        when(service.listByType(0)).thenReturn(List.of(product));

        Result<List<TourProduct>> result = controller.list(0);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).singleElement().satisfies(item -> {
            assertThat(item.getName()).isEqualTo("西湖环湖一日游");
            assertThat(item.getTourType()).isZero();
            assertThat(item.getPrice()).isEqualByComparingTo("118.00");
        });
        verify(service).listByType(0);
    }

    @Test
    void unitTc108ReturnsEmptyListWithoutInventingProducts() {
        when(service.listByType(1)).thenReturn(List.of());

        Result<List<TourProduct>> result = controller.list(1);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void unitTc108RejectsUnsupportedCategory() {
        Result<List<TourProduct>> result = controller.list(9);

        assertThat(result.getCode()).isNotEqualTo(200);
        assertThat(result.getMsg()).contains("必须为0或1");
    }
}
