package com.travelmate.microservices.local;

import com.travelmate.entity.Destination;
import com.travelmate.mapper.DestinationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InternalAdminDestinationControllerTests {
    private DestinationMapper destinations;
    private MockMvc mvc;

    @BeforeEach void setUp() {
        destinations = mock(DestinationMapper.class);
        mvc = MockMvcBuilders.standaloneSetup(new InternalAdminDestinationController(destinations, "token")).build();
    }

    @Test void listsSynchronizesAndDisablesDestinations() throws Exception {
        Destination row = new Destination(); row.setId(7L); row.setSlug("beijing"); row.setStatus(1);
        when(destinations.selectList(any())).thenReturn(List.of(row));
        when(destinations.selectOne(any())).thenReturn(null, row);
        when(destinations.selectById(7L)).thenReturn(row);
        when(destinations.updateById(any(Destination.class))).thenReturn(1);

        mvc.perform(get("/internal/local/admin/destinations").header("X-Internal-Token", "token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].slug").value("beijing"));
        String body="["
                + "{\"slug\":\"BEIJING\",\"name\":\"北京\",\"tag\":\"古都\",\"img\":\"a.jpg\",\"desc\":\"首都\",\"intro\":\"北京介绍\"},"
                + "{\"slug\":\"shanghai\",\"name\":\"上海\",\"tag\":\"都市\",\"img\":\"b.jpg\",\"desc\":\"魔都\",\"intro\":\"上海介绍\"}]";
        mvc.perform(post("/internal/local/admin/destinations/sync-home").header("X-Internal-Token", "token")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.inserted").value(1)).andExpect(jsonPath("$.updated").value(1));
        mvc.perform(delete("/internal/local/admin/destinations/7").header("X-Internal-Token", "token"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(0, row.getStatus());
        verify(destinations).insert(any(Destination.class));
    }

    @Test void rejectsInvalidTokenEmptyInputAndDuplicateSlugs() throws Exception {
        mvc.perform(get("/internal/local/admin/destinations").header("X-Internal-Token", "wrong"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/internal/local/admin/destinations/sync-home").header("X-Internal-Token", "token")
                        .contentType("application/json").content("[]"))
                .andExpect(status().isBadRequest());
        String duplicate="["
                + "{\"slug\":\"x\",\"name\":\"甲\",\"tag\":\"标签\",\"img\":\"a\",\"desc\":\"描述\",\"intro\":\"介绍\"},"
                + "{\"slug\":\"X\",\"name\":\"乙\",\"tag\":\"标签\",\"img\":\"b\",\"desc\":\"描述\",\"intro\":\"介绍\"}]";
        mvc.perform(post("/internal/local/admin/destinations/sync-home").header("X-Internal-Token", "token")
                        .contentType("application/json").content(duplicate))
                .andExpect(status().isBadRequest());
    }
}
