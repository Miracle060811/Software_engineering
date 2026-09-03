package com.travelmate.service.impl;

import com.travelmate.dto.TrainLiveSyncStatus;
import com.travelmate.entity.Train;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.service.TrainLiveSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainServiceLivePriorityTests {

    @Test
    void attemptsPreferredLiveRouteBeforeReturningExistingLocalData() {
        TrainMapper mapper = mock(TrainMapper.class);
        TrainLiveSyncService liveSyncService = mock(TrainLiveSyncService.class);
        TrainStationResolver stationResolver = mock(TrainStationResolver.class);
        TrainServiceImpl service = new TrainServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "trainLiveSyncService", liveSyncService);
        ReflectionTestUtils.setField(service, "trainStationResolver", stationResolver);

        Train local = new Train();
        local.setTrainNo("LOCAL-G1");
        when(stationResolver.routeCandidates("北京南", "上海虹桥"))
                .thenReturn(List.of(new TrainStationResolver.RouteCandidate("北京南", "上海虹桥")));
        when(stationResolver.stationsFor("北京南")).thenReturn(List.of("北京南"));
        when(stationResolver.stationsFor("上海虹桥")).thenReturn(List.of("上海虹桥"));
        when(liveSyncService.syncIfSupported("北京南", "上海虹桥", "2026-09-03"))
                .thenReturn(new TrainLiveSyncStatus(true, true, false, "fallback", "北京南->上海虹桥",
                        "2026-09-03", "LOCAL_DEMO_CACHE", 0, null));
        when(mapper.selectList(any())).thenReturn(List.of(local));

        List<Train> result = service.searchTrains("北京南", "上海虹桥", "2026-09-03", 0, 10);

        verify(liveSyncService).syncIfSupported("北京南", "上海虹桥", "2026-09-03");
        assertThat(result).containsExactly(local);
    }
}
