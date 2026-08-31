package com.travelmate;

import com.travelmate.common.Result;
import com.travelmate.service.StockPreDeductResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultAndStockBoundaryTests {

    @Test
    void successResultUsesStableUnifiedPayloadShape() {
        Result<String> result = Result.success("ok");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("成功");
        assertThat(result.getData()).isEqualTo("ok");
    }

    @Test
    void errorResultKeepsMessageAndOmitsData() {
        Result<String> result = Result.error("库存不足");

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMsg()).isEqualTo("库存不足");
        assertThat(result.getData()).isNull();
    }

    @Test
    void stockPreDeductBoundaryStatesAreExplicit() {
        assertThat(StockPreDeductResult.values())
                .containsExactly(
                        StockPreDeductResult.DEDUCTED_IN_REDIS,
                        StockPreDeductResult.FALLBACK_TO_DB,
                        StockPreDeductResult.NO_STOCK);
    }
}
