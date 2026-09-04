package com.travelmate.microservices.ops;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminCsvImportServiceTests {
    @Test void validatesWithoutWritingAndImportsThroughGateway() {
        OpsAggregationGateway gateway=mock(OpsAggregationGateway.class);
        AdminCsvImportService service=new AdminCsvImportService(gateway);
        String csv="flightNo,airline,departureCity,arrivalCity,departureTime,arrivalTime,economyPrice,businessPrice,totalSeats,availableSeats\n"
                +"CA1,国航,北京,上海,2026-09-05T08:00:00,2026-09-05T10:00:00,500,1000,100,80\n";
        MockMultipartFile file=new MockMultipartFile("file","flights.csv","text/csv",csv.getBytes(StandardCharsets.UTF_8));
        Map<String,Object> checked=service.importCsv("flights",file,true,"insert");
        assertEquals(1,checked.get("validated")); verifyNoInteractions(gateway);
        when(gateway.importResource(eq("flights"),anyMap(),eq(false))).thenReturn(false);
        Map<String,Object> imported=service.importCsv("flights",file,false,"insert");
        assertEquals(1,imported.get("inserted")); verify(gateway).importResource(eq("flights"),anyMap(),eq(false));
    }

    @Test void reportsBadRowsWithoutWritingThem() {
        OpsAggregationGateway gateway=mock(OpsAggregationGateway.class);
        AdminCsvImportService service=new AdminCsvImportService(gateway);
        String csv="name,city,address,starRating,avgPrice\n酒店,上海,,4,500\n";
        MockMultipartFile file=new MockMultipartFile("file","hotels.csv","text/csv",csv.getBytes(StandardCharsets.UTF_8));
        Map<String,Object> result=service.importCsv("hotels",file,false,"insert");
        assertEquals(1,result.get("failed")); verifyNoInteractions(gateway);
    }
}
