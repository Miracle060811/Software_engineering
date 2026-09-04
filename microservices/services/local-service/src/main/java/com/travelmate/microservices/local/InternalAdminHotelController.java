package com.travelmate.microservices.local;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.Attraction;
import com.travelmate.entity.Hotel;
import com.travelmate.entity.HotelOrder;
import com.travelmate.entity.HotelRoom;
import com.travelmate.entity.Review;
import com.travelmate.mapper.AttractionMapper;
import com.travelmate.mapper.HotelMapper;
import com.travelmate.mapper.HotelOrderMapper;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.mapper.ReviewMapper;
import com.travelmate.service.HotelRoomStockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/internal/local/admin")
public class InternalAdminHotelController {
    private final HotelMapper hotels;
    private final HotelRoomMapper rooms;
    private final HotelOrderMapper orders;
    private final HotelRoomStockService stockService;
    private final AttractionMapper attractions;
    private final ReviewMapper reviews;
    private final String token;

    public InternalAdminHotelController(HotelMapper hotels, HotelRoomMapper rooms, HotelOrderMapper orders,
                                        HotelRoomStockService stockService, AttractionMapper attractions, ReviewMapper reviews,
                                        @Value("${app.internal-service-token}") String token) {
        this.hotels = hotels; this.rooms = rooms; this.orders = orders; this.stockService = stockService;
        this.attractions = attractions; this.reviews = reviews; this.token = token;
    }

    @GetMapping("/hotels")
    public List<Hotel> hotels(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); return hotels.selectList(new LambdaQueryWrapper<Hotel>().orderByDesc(Hotel::getId));
    }

    @PostMapping("/hotels")
    public Hotel addHotel(@RequestBody Hotel hotel, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); validateHotel(hotel); hotel.setId(null); hotels.insert(hotel); return hotel;
    }

    @PutMapping("/hotels/{id}")
    public void updateHotel(@PathVariable Long id, @RequestBody Hotel hotel, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); validateHotel(hotel); hotel.setId(id);
        if (hotels.updateById(hotel) == 0) notFound("酒店不存在");
    }

    @DeleteMapping("/hotels/{id}")
    public void deleteHotel(@PathVariable Long id, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        if (orders.selectCount(new LambdaQueryWrapper<HotelOrder>().eq(HotelOrder::getHotelId, id)) > 0) conflict("该酒店已有订单，不能删除");
        if (hotels.deleteById(id) == 0) notFound("酒店不存在");
    }

    @GetMapping("/hotels/{hotelId}/rooms")
    public List<HotelRoom> rooms(@PathVariable Long hotelId, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); return rooms.selectList(new LambdaQueryWrapper<HotelRoom>().eq(HotelRoom::getHotelId, hotelId).orderByAsc(HotelRoom::getId));
    }

    @PostMapping("/hotels/{hotelId}/rooms")
    public HotelRoom addRoom(@PathVariable Long hotelId, @RequestBody HotelRoom room, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); if (hotels.selectById(hotelId) == null) notFound("酒店不存在");
        validateRoom(room); room.setId(null); room.setHotelId(hotelId); rooms.insert(room); stockService.syncWithDatabase(room.getId()); return room;
    }

    @PutMapping("/hotel-rooms/{id}")
    public void updateRoom(@PathVariable Long id, @RequestBody HotelRoom room, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); validateRoom(room); room.setId(id);
        if (rooms.updateById(room) == 0) notFound("房型不存在"); stockService.syncWithDatabase(id);
    }

    @DeleteMapping("/hotel-rooms/{id}")
    public void deleteRoom(@PathVariable Long id, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        if (orders.selectCount(new LambdaQueryWrapper<HotelOrder>().eq(HotelOrder::getRoomId, id)) > 0) conflict("该房型已有订单，不能删除");
        if (rooms.deleteById(id) == 0) notFound("房型不存在"); stockService.syncWithDatabase(id);
    }

    @GetMapping("/attractions")
    public List<Attraction> attractions(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); return attractions.selectList(new LambdaQueryWrapper<Attraction>().orderByAsc(Attraction::getCity).orderByDesc(Attraction::getId));
    }

    @PostMapping("/attractions")
    public Attraction addAttraction(@RequestBody Attraction attraction, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); validateAttraction(attraction); attraction.setId(null); attractions.insert(attraction); return attraction;
    }

    @PutMapping("/attractions/{id}")
    public void updateAttraction(@PathVariable Long id, @RequestBody Attraction attraction, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); validateAttraction(attraction); attraction.setId(id);
        if (attractions.updateById(attraction) == 0) notFound("景点不存在");
    }

    @DeleteMapping("/attractions/{id}")
    public void deleteAttraction(@PathVariable Long id, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        long reviewCount = reviews.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getTargetType, 1).eq(Review::getTargetId, id).eq(Review::getDeleted, 0));
        if (reviewCount > 0) {
            Attraction attraction = attractions.selectById(id);
            if (attraction == null) notFound("景点不存在");
            attraction.setStatus(0);
            if (attractions.updateById(attraction) == 0) notFound("景点不存在");
        } else if (attractions.deleteById(id) == 0) notFound("景点不存在");
    }

    private void validateHotel(Hotel hotel) {
        text(hotel.getName(), "酒店名称"); text(hotel.getCity(), "城市"); text(hotel.getAddress(), "地址"); nonNegative(hotel.getAvgPrice(), "均价");
        if (hotel.getStarRating() == null || hotel.getStarRating() < 1 || hotel.getStarRating() > 5) bad("星级必须在 1-5 之间");
        if (hotel.getScore() != null && (hotel.getScore().compareTo(BigDecimal.ZERO) < 0 || hotel.getScore().compareTo(BigDecimal.valueOf(5)) > 0)) bad("评分必须在 0-5 之间");
        if (hotel.getStatus() == null) hotel.setStatus(1);
    }

    private void validateRoom(HotelRoom room) {
        text(room.getRoomType(), "房型"); text(room.getBedType(), "床型"); nonNegative(room.getPrice(), "房型价格"); nonNegative(room.getTotalRooms(), "总房量"); nonNegative(room.getAvailableRooms(), "可售房量");
        if (room.getAvailableRooms() > room.getTotalRooms()) bad("可售房量不能大于总房量");
        if (room.getStatus() == null) room.setStatus(1);
    }

    private void validateAttraction(Attraction attraction) {
        text(attraction.getName(), "景点名称"); text(attraction.getCity(), "城市"); text(attraction.getAddress(), "地址");
        nonNegative(attraction.getAdultPrice(), "成人票价"); nonNegative(attraction.getChildPrice(), "儿童票价");
        nonNegative(attraction.getTotalTickets(), "总票数"); nonNegative(attraction.getAvailableTickets(), "可售票数");
        if (attraction.getAvailableTickets() > attraction.getTotalTickets()) bad("可售票数不能大于总票数");
        if (attraction.getStatus() == null) attraction.setStatus(1);
    }

    private void verify(String supplied) { if (!token.equals(supplied)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效"); }
    private void text(String value, String name) { if (value == null || value.isBlank()) bad(name + "不能为空"); }
    private void nonNegative(Number value, String name) { if (value == null || value.doubleValue() < 0) bad(name + "不能为负数"); }
    private void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private void notFound(String message) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
