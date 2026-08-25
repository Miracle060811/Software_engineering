package com.travelmate.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class TravelPlaceServiceTests {

    @Test
    void parsesVerifiedCityAndRejectsNonCityResult() throws Exception {
        TravelPlaceService service = new TravelPlaceService();
        String cityJson = """
                [{"name":"大理市","display_name":"大理市, 云南省, 中国","lat":"25.6065","lon":"100.2676",
                  "addresstype":"city","address":{"city":"大理市","country_code":"cn"},
                  "namedetails":{"name:zh":"大理市"}}]
                """;
        TravelPlaceService.PlaceLookup city = service.parseNominatimResponse("云南大理", cityJson);
        assertEquals(TravelPlaceService.LookupStatus.FOUND, city.status());
        assertEquals("大理市", city.place().canonicalName());
        assertTrue(city.place().hasCoordinates());

        String suburbJson = """
                [{"name":"火星街道","display_name":"火星街道, 湖南省, 中国","lat":"28.2","lon":"113.1",
                  "addresstype":"suburb","address":{"suburb":"火星街道","country_code":"cn"}}]
                """;
        assertEquals(TravelPlaceService.LookupStatus.NON_CITY,
                service.parseNominatimResponse("火星", suburbJson).status());
        assertEquals(TravelPlaceService.LookupStatus.NOT_FOUND,
                service.parseNominatimResponse("酒馆蛋炒饭", "[]").status());
    }

    @Test
    void deterministicBoundaryAllowsKnownCityAndRejectsInventedPlace() {
        TravelPlaceService service = new TravelPlaceService();
        ReflectionTestUtils.setField(service, "onlineVerificationEnabled", false);

        assertEquals("杭州", service.verifyCity("杭州", "目的地").canonicalName());
        TravelPlaceService.TravelPlaceException error = assertThrows(
                TravelPlaceService.TravelPlaceException.class,
                () -> service.verifyCity("酒馆蛋炒饭", "目的地"));
        assertTrue(error.getMessage().contains("无法核验"));
    }

    @Test
    void extractsOnlyExplicitTravelPlaceCandidate() {
        TravelPlaceService service = new TravelPlaceService();
        assertEquals("酒馆蛋炒饭", service.findExplicitTravelCity("我想去酒馆蛋炒饭旅游"));
        assertEquals("酒馆蛋炒饭", service.findExplicitTravelCity("酒馆蛋炒饭怎么去？"));
        assertEquals("杭州", service.findExplicitTravelCity("杭州天气怎么样？"));
        assertEquals("北京", service.findExplicitTravelCity("请问北京有哪些景点？"));
        assertEquals("上海", service.findExplicitTravelCity("推荐上海酒店"));
        assertNull(service.findExplicitTravelCity("怎样控制旅行预算？"));
    }
}
