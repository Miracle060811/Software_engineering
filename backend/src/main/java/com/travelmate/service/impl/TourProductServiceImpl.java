package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.dto.TourBookingCreateDTO;
import com.travelmate.entity.TourOrder;
import com.travelmate.entity.TourProduct;
import com.travelmate.entity.TourProductStep;
import com.travelmate.entity.TourSchedule;
import com.travelmate.mapper.TourOrderMapper;
import com.travelmate.mapper.TourProductMapper;
import com.travelmate.mapper.TourProductStepMapper;
import com.travelmate.mapper.TourScheduleMapper;
import com.travelmate.service.TourProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class TourProductServiceImpl extends ServiceImpl<TourProductMapper, TourProduct>
        implements TourProductService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{8,64}$");

    @Autowired
    private TourProductStepMapper tourProductStepMapper;

    @Autowired
    private TourScheduleMapper tourScheduleMapper;

    @Autowired
    private TourOrderMapper tourOrderMapper;

    @Override
    public List<TourProduct> listByType(Integer tourType) {
        List<TourProduct> products = list(new LambdaQueryWrapper<TourProduct>()
                .eq(TourProduct::getTourType, tourType)
                .eq(TourProduct::getStatus, 1)
                .orderByDesc(TourProduct::getCreateTime));
        products.forEach(product -> {
            product.setSteps(tourProductStepMapper.selectList(new LambdaQueryWrapper<TourProductStep>()
                    .eq(TourProductStep::getProductId, product.getId())
                    .orderByAsc(TourProductStep::getDayNo)
                    .orderByAsc(TourProductStep::getSequenceNo)));
            product.setSchedules(tourScheduleMapper.selectList(new LambdaQueryWrapper<TourSchedule>()
                    .eq(TourSchedule::getProductId, product.getId())
                    .eq(TourSchedule::getStatus, 1)
                    .ge(TourSchedule::getTravelDate, LocalDate.now())
                    .orderByAsc(TourSchedule::getTravelDate)));
        });
        return products;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TourOrder createBooking(Long userId, TourBookingCreateDTO request) {
        NormalizedBooking normalized = validateAndNormalize(userId, request);
        TourOrder existing = selectByIdempotencyKey(userId, normalized.idempotencyKey());
        if (existing != null) {
            return requireMatchingRetry(existing, normalized);
        }

        TourProduct product = getById(normalized.productId());
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new RuntimeException("旅游产品不存在或已下架");
        }
        if (product.getTourType() == null || (product.getTourType() != 0 && product.getTourType() != 1)) {
            throw new RuntimeException("旅游产品类型无效");
        }

        TourSchedule schedule = tourScheduleMapper.selectById(normalized.scheduleId());
        if (schedule == null || !Objects.equals(schedule.getProductId(), product.getId())) {
            throw new RuntimeException("所选班期不属于该旅游产品");
        }
        if (!Integer.valueOf(1).equals(schedule.getStatus())
                || schedule.getTravelDate() == null
                || schedule.getTravelDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("所选日期不可售");
        }
        if (schedule.getUnitPrice() == null || schedule.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("班期价格无效");
        }
        if (schedule.getUnitPrice().compareTo(normalized.expectedUnitPrice()) != 0) {
            throw new RuntimeException("产品价格已变化，请刷新后重试");
        }

        int updated = tourScheduleMapper.deductStock(
                schedule.getId(), product.getId(), normalized.participantCount());
        if (updated != 1) {
            throw new RuntimeException("班期库存不足，预订失败");
        }

        TourOrder order = new TourOrder();
        order.setOrderNo("TO" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setScheduleId(schedule.getId());
        order.setProductName(product.getName());
        order.setTourType(product.getTourType());
        order.setTravelDate(schedule.getTravelDate());
        order.setParticipantCount(normalized.participantCount());
        order.setContactName(normalized.contactName());
        order.setContactPhone(normalized.contactPhone());
        order.setUnitPrice(schedule.getUnitPrice());
        order.setAmount(schedule.getUnitPrice().multiply(BigDecimal.valueOf(normalized.participantCount())));
        order.setIdempotencyKey(normalized.idempotencyKey());
        order.setStatus(1);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setDeleted(0);
        if (tourOrderMapper.insert(order) != 1) {
            throw new RuntimeException("旅游产品订单创建失败");
        }
        return order;
    }

    @Override
    public TourOrder findIdempotentBooking(Long userId, TourBookingCreateDTO request) {
        NormalizedBooking normalized = validateAndNormalize(userId, request);
        TourOrder existing = selectByIdempotencyKey(userId, normalized.idempotencyKey());
        if (existing == null) {
            throw new RuntimeException("重复请求处理失败，请查询订单后重试");
        }
        return requireMatchingRetry(existing, normalized);
    }

    @Override
    public List<TourOrder> listUserOrders(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户未登录或Token无效");
        }
        return tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getUserId, userId)
                .eq(TourOrder::getDeleted, 0)
                .orderByDesc(TourOrder::getCreateTime));
    }

    @Override
    public TourOrder getUserOrder(Long userId, String orderNo) {
        if (userId == null || !StringUtils.hasText(orderNo)) {
            return null;
        }
        return tourOrderMapper.selectOne(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getUserId, userId)
                .eq(TourOrder::getOrderNo, orderNo.trim())
                .eq(TourOrder::getDeleted, 0));
    }

    private TourOrder selectByIdempotencyKey(Long userId, String idempotencyKey) {
        return tourOrderMapper.selectOne(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getUserId, userId)
                .eq(TourOrder::getIdempotencyKey, idempotencyKey)
                .eq(TourOrder::getDeleted, 0));
    }

    private TourOrder requireMatchingRetry(TourOrder existing, NormalizedBooking request) {
        boolean matches = Objects.equals(existing.getProductId(), request.productId())
                && Objects.equals(existing.getScheduleId(), request.scheduleId())
                && Objects.equals(existing.getParticipantCount(), request.participantCount())
                && Objects.equals(existing.getContactName(), request.contactName())
                && Objects.equals(existing.getContactPhone(), request.contactPhone())
                && existing.getUnitPrice() != null
                && existing.getUnitPrice().compareTo(request.expectedUnitPrice()) == 0;
        if (!matches) {
            throw new RuntimeException("幂等键已用于其他预订请求");
        }
        return existing;
    }

    private NormalizedBooking validateAndNormalize(Long userId, TourBookingCreateDTO request) {
        if (userId == null) {
            throw new RuntimeException("用户未登录或Token无效");
        }
        if (request == null || request.getProductId() == null || request.getScheduleId() == null) {
            throw new RuntimeException("旅游产品和班期不能为空");
        }
        int count = request.getParticipantCount() == null ? 0 : request.getParticipantCount();
        if (count < 1 || count > 10) {
            throw new RuntimeException("出行人数必须为1至10人");
        }
        String contactName = request.getContactName() == null ? "" : request.getContactName().trim();
        if (contactName.length() < 2 || contactName.length() > 50) {
            throw new RuntimeException("联系人姓名长度必须为2至50个字符");
        }
        String contactPhone = request.getContactPhone() == null ? "" : request.getContactPhone().trim();
        if (!PHONE_PATTERN.matcher(contactPhone).matches()) {
            throw new RuntimeException("联系人手机号格式不正确");
        }
        BigDecimal expectedUnitPrice = request.getExpectedUnitPrice();
        if (expectedUnitPrice == null || expectedUnitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("预期价格必须大于0");
        }
        String idempotencyKey = request.getIdempotencyKey() == null
                ? ""
                : request.getIdempotencyKey().trim();
        if (!IDEMPOTENCY_KEY_PATTERN.matcher(idempotencyKey).matches()) {
            throw new RuntimeException("幂等键格式不正确");
        }
        return new NormalizedBooking(
                request.getProductId(), request.getScheduleId(), count,
                contactName, contactPhone, expectedUnitPrice, idempotencyKey);
    }

    private record NormalizedBooking(
            Long productId,
            Long scheduleId,
            Integer participantCount,
            String contactName,
            String contactPhone,
            BigDecimal expectedUnitPrice,
            String idempotencyKey) {
    }
}
