package com.travelmate.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.dto.TrainBrowserTicket;
import com.travelmate.service.TrainBrowserSyncService;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainLiveQuerySupportTests {

    @Test
    void parsesOfficialLeftTicketFieldsAndStationMap() throws Exception {
        String[] fields = new String[39];
        java.util.Arrays.fill(fields, "");
        fields[3] = "G123";
        fields[6] = "NKH";
        fields[7] = "ENH";
        fields[8] = "08:10";
        fields[9] = "09:25";
        fields[10] = "01:15";
        fields[26] = "有";
        fields[29] = "--";
        fields[30] = "12";
        fields[31] = "有";
        fields[32] = "3";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("result", List.of(String.join("|", fields)));
        data.put("map", Map.of("NKH", "南京南", "ENH", "合肥南"));
        String response = new ObjectMapper().writeValueAsString(Map.of("httpstatus", 200, "data", data));

        TrainBrowserSyncServiceImpl service = new TrainBrowserSyncServiceImpl(new ObjectMapper());
        List<TrainBrowserTicket> tickets = service.parseTickets(response, "南京南", "合肥南");

        assertThat(tickets).hasSize(1);
        TrainBrowserTicket ticket = tickets.getFirst();
        assertThat(ticket.getTrainNo()).isEqualTo("G123");
        assertThat(ticket.getDepartureStation()).isEqualTo("南京南");
        assertThat(ticket.getArrivalStation()).isEqualTo("合肥南");
        assertThat(ticket.getDepartureTime()).isEqualTo("08:10");
        assertThat(ticket.getArrivalTime()).isEqualTo("09:25");
        assertThat(ticket.getDuration()).isEqualTo("01:15");
        assertThat(ticket.getBusinessSeat()).isEqualTo("3");
        assertThat(ticket.getFirstClassSeat()).isEqualTo("有");
        assertThat(ticket.getSecondClassSeat()).isEqualTo("12");
        assertThat(ticket.getHardSeat()).isEqualTo("--");
        assertThat(ticket.getNoSeat()).isEqualTo("有");
    }

    @Test
    void cityInputUsesPreferredOfficialStationsWhileExactStationStaysExact() {
        TrainBrowserSyncService liveQuery = mock(TrainBrowserSyncService.class);
        when(liveQuery.resolveStationCandidates("合肥"))
                .thenReturn(List.of("合肥南", "合肥", "合肥北城"));
        when(liveQuery.resolveStationCandidates("南京南"))
                .thenReturn(List.of("南京南"));

        TrainStationResolver resolver = new TrainStationResolver(liveQuery);

        assertThat(resolver.stationsFor("合肥"))
                .containsExactly("合肥南", "合肥", "合肥北城");
        assertThat(resolver.stationsFor("南京南"))
                .containsExactly("南京南");
    }

    @Test
    void routeCandidatesStartWithPreferredHighSpeedStations() {
        TrainBrowserSyncService liveQuery = mock(TrainBrowserSyncService.class);
        when(liveQuery.resolveStationCandidates("广州"))
                .thenReturn(List.of("广州南", "广州东", "广州", "广州白云", "广州北"));
        when(liveQuery.resolveStationCandidates("深圳"))
                .thenReturn(List.of("深圳北", "深圳", "福田", "深圳东", "深圳坪山"));

        TrainStationResolver resolver = new TrainStationResolver(liveQuery);

        assertThat(resolver.routeCandidates("广州", "深圳").getFirst())
                .isEqualTo(new TrainStationResolver.RouteCandidate("广州南", "深圳北"));
    }
}
