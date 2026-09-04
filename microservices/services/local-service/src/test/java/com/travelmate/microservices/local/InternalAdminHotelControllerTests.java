package com.travelmate.microservices.local;

import com.travelmate.entity.Hotel;
import com.travelmate.entity.HotelOrder;
import com.travelmate.entity.HotelRoom;
import com.travelmate.mapper.HotelMapper;
import com.travelmate.mapper.HotelOrderMapper;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.mapper.AttractionMapper;
import com.travelmate.mapper.ReviewMapper;
import com.travelmate.service.HotelRoomStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InternalAdminHotelControllerTests {
    private HotelMapper hotels;
    private HotelRoomMapper rooms;
    private HotelOrderMapper orders;
    private HotelRoomStockService stock;
    private AttractionMapper attractions;
    private ReviewMapper reviews;
    private MockMvc mvc;

    @BeforeEach void setUp() {
        hotels = mock(HotelMapper.class); rooms = mock(HotelRoomMapper.class);
        orders = mock(HotelOrderMapper.class); stock = mock(HotelRoomStockService.class);
        attractions = mock(AttractionMapper.class); reviews = mock(ReviewMapper.class);
        mvc = MockMvcBuilders.standaloneSetup(new InternalAdminHotelController(hotels, rooms, orders, stock,
                attractions, reviews, "token")).build();
    }

    @Test void hotelAndRoomCrudWorks() throws Exception {
        Hotel hotel = new Hotel(); hotel.setId(1L);
        HotelRoom room = new HotelRoom(); room.setId(2L);
        when(hotels.selectList(any())).thenReturn(List.of(hotel));
        when(hotels.selectById(1L)).thenReturn(hotel);
        when(rooms.selectList(any())).thenReturn(List.of(room));
        when(hotels.updateById(any(Hotel.class))).thenReturn(1); when(hotels.deleteById(1L)).thenReturn(1);
        when(rooms.updateById(any(HotelRoom.class))).thenReturn(1); when(rooms.deleteById(2L)).thenReturn(1);
        when(orders.selectCount(any())).thenReturn(0L);
        String h="{\"name\":\"酒店\",\"city\":\"上海\",\"address\":\"示例路1号\",\"starRating\":4,\"avgPrice\":500}";
        String r="{\"roomType\":\"大床房\",\"bedType\":\"大床\",\"price\":300,\"totalRooms\":10,\"availableRooms\":8}";
        mvc.perform(get("/internal/local/admin/hotels").header("X-Internal-Token","token")).andExpect(status().isOk());
        mvc.perform(post("/internal/local/admin/hotels").header("X-Internal-Token","token").contentType("application/json").content(h)).andExpect(status().isOk());
        mvc.perform(put("/internal/local/admin/hotels/1").header("X-Internal-Token","token").contentType("application/json").content(h)).andExpect(status().isOk());
        mvc.perform(delete("/internal/local/admin/hotels/1").header("X-Internal-Token","token")).andExpect(status().isOk());
        mvc.perform(get("/internal/local/admin/hotels/1/rooms").header("X-Internal-Token","token")).andExpect(status().isOk());
        mvc.perform(post("/internal/local/admin/hotels/1/rooms").header("X-Internal-Token","token").contentType("application/json").content(r)).andExpect(status().isOk());
        mvc.perform(put("/internal/local/admin/hotel-rooms/2").header("X-Internal-Token","token").contentType("application/json").content(r)).andExpect(status().isOk());
        mvc.perform(delete("/internal/local/admin/hotel-rooms/2").header("X-Internal-Token","token")).andExpect(status().isOk());
        verify(stock, atLeastOnce()).syncWithDatabase(any());
    }

    @Test void rejectsInvalidTokenAndInvalidStock() throws Exception {
        when(hotels.selectById(1L)).thenReturn(new Hotel());
        String invalid="{\"roomType\":\"大床房\",\"bedType\":\"大床\",\"price\":300,\"totalRooms\":2,\"availableRooms\":3}";
        mvc.perform(get("/internal/local/admin/hotels").header("X-Internal-Token","wrong")).andExpect(status().isForbidden());
        mvc.perform(post("/internal/local/admin/hotels/1/rooms").header("X-Internal-Token","token").contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
    }

    @Test void refusesDeletionWhenOrdersExist() throws Exception {
        when(orders.selectCount(any())).thenReturn(1L);
        mvc.perform(delete("/internal/local/admin/hotels/1").header("X-Internal-Token","token")).andExpect(status().isConflict());
        mvc.perform(delete("/internal/local/admin/hotel-rooms/2").header("X-Internal-Token","token")).andExpect(status().isConflict());
    }

    @Test void attractionCrudWorksAndPreservesReviewedAttractions() throws Exception {
        com.travelmate.entity.Attraction reviewed = new com.travelmate.entity.Attraction();
        reviewed.setId(4L);
        when(attractions.selectList(any())).thenReturn(List.of());
        when(attractions.selectById(4L)).thenReturn(reviewed);
        when(attractions.updateById(any(com.travelmate.entity.Attraction.class))).thenReturn(1);
        when(attractions.deleteById(3L)).thenReturn(1);
        String body="{\"name\":\"西湖\",\"city\":\"杭州\",\"address\":\"西湖区\",\"adultPrice\":80,\"childPrice\":40,\"totalTickets\":100,\"availableTickets\":80}";
        mvc.perform(get("/internal/local/admin/attractions").header("X-Internal-Token","token")).andExpect(status().isOk());
        mvc.perform(post("/internal/local/admin/attractions").header("X-Internal-Token","token").contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(put("/internal/local/admin/attractions/3").header("X-Internal-Token","token").contentType("application/json").content(body)).andExpect(status().isOk());
        when(reviews.selectCount(any())).thenReturn(0L);
        mvc.perform(delete("/internal/local/admin/attractions/3").header("X-Internal-Token","token")).andExpect(status().isOk());
        when(reviews.selectCount(any())).thenReturn(1L);
        mvc.perform(delete("/internal/local/admin/attractions/4").header("X-Internal-Token","token")).andExpect(status().isOk());
        verify(attractions).deleteById(3L);
        verify(attractions).updateById(reviewed);
        org.junit.jupiter.api.Assertions.assertEquals(0, reviewed.getStatus());
    }
}
