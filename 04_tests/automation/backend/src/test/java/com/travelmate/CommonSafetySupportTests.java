package com.travelmate;

import com.travelmate.common.LogSanitizer;
import com.travelmate.common.PaginationSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CommonSafetySupportTests {

    @Test
    void paginationNormalizesInvalidAndOversizedInputs() {
        PaginationSupport.Page firstPage = PaginationSupport.normalize(-3, 0, 100);
        PaginationSupport.Page cappedPage = PaginationSupport.normalize(2, 500, 100);

        assertThat(firstPage).isEqualTo(new PaginationSupport.Page(1, 1, 0));
        assertThat(cappedPage).isEqualTo(new PaginationSupport.Page(2, 100, 100));
    }

    @Test
    void paginationCannotOverflowAndWindowsStayInsideResults() {
        PaginationSupport.Page hugePage = PaginationSupport.normalize(Integer.MAX_VALUE, 100, 100);
        PaginationSupport.Window emptyWindow = PaginationSupport.window(Integer.MAX_VALUE, 100, 100, 25);

        assertThat(hugePage.offset()).isEqualTo(Integer.MAX_VALUE);
        assertThat(emptyWindow.start()).isEqualTo(25);
        assertThat(emptyWindow.end()).isEqualTo(25);
    }

    @Test
    void paginationRequiresAPositiveMaximumSize() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PaginationSupport.normalize(1, 10, 0));
    }

    @Test
    void logValuesAreCollapsedToOneLine() {
        assertThat(LogSanitizer.singleLine("route\r\nforged"))
                .isEqualTo("route__forged");
        assertThat(LogSanitizer.singleLine(null)).isEmpty();
    }
}
