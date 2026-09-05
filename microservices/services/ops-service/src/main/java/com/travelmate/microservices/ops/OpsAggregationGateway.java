package com.travelmate.microservices.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpsAggregationGateway {
    private final RestClient identity;
    private final RestClient traffic;
    private final RestClient local;
    private final RestClient community;
    private final String token;
    private final ObjectMapper objectMapper;

    public OpsAggregationGateway(RestClient.Builder builder,
                                 ObjectMapper objectMapper,
                                 @Value("${app.services.identity-url}") String identityUrl,
                                 @Value("${app.services.traffic-url}") String trafficUrl,
                                 @Value("${app.services.local-url}") String localUrl,
                                 @Value("${app.services.community-url}") String communityUrl,
                                 @Value("${app.internal-service-token}") String token,
                                 @Value("${app.services.connect-timeout-ms:1000}") int connectTimeoutMs,
                                 @Value("${app.services.read-timeout-ms:2000}") int readTimeoutMs) {
        this.identity = buildClient(builder, identityUrl, connectTimeoutMs, readTimeoutMs);
        this.traffic = buildClient(builder, trafficUrl, connectTimeoutMs, readTimeoutMs);
        this.local = buildClient(builder, localUrl, connectTimeoutMs, readTimeoutMs);
        this.community = buildClient(builder, communityUrl, connectTimeoutMs, readTimeoutMs);
        this.token = token;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> users() { return list(identity, "/internal/identity/admin/users"); }
    public void disableUser(Long id, Long adminId) { postText(identity, "/internal/identity/admin/users/" + id + "/disable?adminId=" + adminId, Map.of()); }
    public void enableUser(Long id) { postText(identity, "/internal/identity/admin/users/" + id + "/enable", Map.of()); }
    public List<Map<String, Object>> orders() { return list(traffic, "/internal/traffic/admin/orders"); }
    public List<Map<String, Object>> localOrders() { return list(local, "/internal/local/admin/dashboard-orders"); }
    public List<Map<String, Object>> flights() { return list(traffic, "/internal/traffic/admin/flights"); }
    public Map<String, Object> addFlight(Map<String, Object> body) { return post(traffic, "/internal/traffic/admin/flights", body); }
    public void updateFlight(Long id, Map<String, Object> body) { put(traffic, "/internal/traffic/admin/flights/" + id, body); }
    public void deleteFlight(Long id) { delete(traffic, "/internal/traffic/admin/flights/" + id); }
    public List<Map<String, Object>> trains() { return list(traffic, "/internal/traffic/admin/trains"); }
    public Map<String, Object> addTrain(Map<String, Object> body) { return post(traffic, "/internal/traffic/admin/trains", body); }
    public void updateTrain(Long id, Map<String, Object> body) { put(traffic, "/internal/traffic/admin/trains/" + id, body); }
    public void deleteTrain(Long id) { delete(traffic, "/internal/traffic/admin/trains/" + id); }
    public List<Map<String, Object>> hotels() { return list(local, "/internal/local/admin/hotels"); }
    public Map<String, Object> addHotel(Map<String, Object> body) { return post(local, "/internal/local/admin/hotels", body); }
    public void updateHotel(Long id, Map<String, Object> body) { put(local, "/internal/local/admin/hotels/" + id, body); }
    public void deleteHotel(Long id) { delete(local, "/internal/local/admin/hotels/" + id); }
    public List<Map<String, Object>> hotelRooms(Long hotelId) { return list(local, "/internal/local/admin/hotels/" + hotelId + "/rooms"); }
    public Map<String, Object> addHotelRoom(Long hotelId, Map<String, Object> body) { return post(local, "/internal/local/admin/hotels/" + hotelId + "/rooms", body); }
    public void updateHotelRoom(Long id, Map<String, Object> body) { put(local, "/internal/local/admin/hotel-rooms/" + id, body); }
    public void deleteHotelRoom(Long id) { delete(local, "/internal/local/admin/hotel-rooms/" + id); }
    public List<Map<String, Object>> attractions() { return list(local, "/internal/local/admin/attractions"); }
    public Map<String, Object> addAttraction(Map<String, Object> body) { return post(local, "/internal/local/admin/attractions", body); }
    public void updateAttraction(Long id, Map<String, Object> body) { put(local, "/internal/local/admin/attractions/" + id, body); }
    public void deleteAttraction(Long id) { delete(local, "/internal/local/admin/attractions/" + id); }
    public List<Map<String, Object>> destinations() { return list(local, "/internal/local/admin/destinations"); }
    public Map<String, Object> syncHomeDestinations(List<Map<String, Object>> body) { return post(local, "/internal/local/admin/destinations/sync-home", body); }
    public void deleteDestination(Long id) { delete(local, "/internal/local/admin/destinations/" + id); }
    public List<Map<String, Object>> coupons() { return list(local, "/internal/local/admin/coupons"); }
    public Map<String, Object> addCoupon(Map<String, Object> body) { return post(local, "/internal/local/admin/coupons", body); }
    public void updateCoupon(Long id, Map<String, Object> body) { put(local, "/internal/local/admin/coupons/" + id, body); }
    public void deleteCoupon(Long id) { delete(local, "/internal/local/admin/coupons/" + id); }
    public List<Map<String, Object>> couponClaims(Long id) {
        Map<Object, Map<String, Object>> usersById = new LinkedHashMap<>();
        for (Map<String, Object> user : users()) usersById.put(user.get("id"), user);
        return list(local, "/internal/local/admin/coupons/" + id + "/claims").stream().map(claim -> {
            Map<String, Object> result = new LinkedHashMap<>(claim);
            Map<String, Object> user = usersById.get(claim.get("userId"));
            result.put("username", user == null ? "未知用户" : user.get("username"));
            result.put("nickname", user == null ? null : user.get("nickname"));
            return result;
        }).toList();
    }
    public String approveOrderRefund(String orderNo) {
        return postText(orderNo.startsWith("HT") ? local : traffic,
                (orderNo.startsWith("HT") ? "/internal/local" : "/internal/traffic")
                        + "/admin/orders/" + orderNo + "/refund/approve", Map.of());
    }
    public String rejectOrderRefund(String orderNo) {
        return postText(orderNo.startsWith("HT") ? local : traffic,
                (orderNo.startsWith("HT") ? "/internal/local" : "/internal/traffic")
                        + "/admin/orders/" + orderNo + "/refund/reject", Map.of());
    }
    public String completeOrderTicket(String orderNo) {
        return postText(traffic, "/internal/traffic/admin/orders/" + orderNo + "/ticket/complete", Map.of());
    }
    public boolean importResource(String type, Map<String,Object> row, boolean upsert) {
        return switch (type) {
            case "flights" -> save(row, upsert, flights(), List.of("flightNo"), this::addFlight, this::updateFlight);
            case "trains" -> save(row, upsert, trains(), List.of("trainNo"), this::addTrain, this::updateTrain);
            case "hotels" -> save(row, upsert, hotels(), List.of("name","city","address"), this::addHotel, this::updateHotel);
            case "rooms" -> {
                Long hotelId=number(row.get("hotelId"));
                yield save(row,upsert,hotelRooms(hotelId),List.of("roomType"),
                        body->addHotelRoom(hotelId,body),this::updateHotelRoom);
            }
            case "attractions" -> save(row,upsert,attractions(),List.of("name","city"),this::addAttraction,this::updateAttraction);
            case "destinations" -> {
                Map<String,Object> result=syncHomeDestinations(List.of(row));
                yield ((Number)result.getOrDefault("updated",0)).intValue()>0;
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"不支持的导入类型："+type);
        };
    }

    private boolean save(Map<String,Object> row, boolean upsert, List<Map<String,Object>> existing,
                         List<String> keys, java.util.function.Function<Map<String,Object>,Map<String,Object>> add,
                         java.util.function.BiConsumer<Long,Map<String,Object>> update) {
        if(upsert) {
            Map<String,Object> match=existing.stream().filter(item->keys.stream()
                    .allMatch(key->java.util.Objects.equals(text(item.get(key)),text(row.get(key))))).findFirst().orElse(null);
            if(match!=null) { update.accept(number(match.get("id")),row); return true; }
        }
        add.apply(row); return false;
    }
    private Long number(Object value) { return value instanceof Number n?n.longValue():Long.valueOf(value.toString()); }
    private String text(Object value) { return value==null?null:value.toString(); }
    public List<Map<String, Object>> posts(Integer status) {
        return list(community, status == null ? "/internal/community/admin/posts"
                : "/internal/community/admin/posts?status=" + status);
    }
    public long pendingPostCount() { return scalar(community, "/internal/community/admin/pending-post-count"); }
    public List<Map<String, Object>> reviewReports(Integer status) {
        String path = status == null ? "/internal/local/admin/review-reports"
                : "/internal/local/admin/review-reports?status=" + status;
        return list(local, path);
    }

    public Map<String, Object> approvePost(Long id) { return post(community, "/internal/community/admin/posts/" + id + "/approve", Map.of()); }
    public Map<String, Object> rejectPost(Long id, Map<String, Object> body) { return post(community, "/internal/community/admin/posts/" + id + "/reject", body); }
    public Map<String, Object> updatePostMetrics(Long id, Map<String, Object> body) { return post(community, "/internal/community/admin/posts/" + id + "/metrics", body); }
    public Map<String, Object> resolveReport(Long id, Map<String, Object> body) { return post(local, "/internal/local/admin/review-reports/" + id + "/resolve", body); }
    public Map<String, Object> rejectReport(Long id, Map<String, Object> body) { return post(local, "/internal/local/admin/review-reports/" + id + "/reject", body); }
    public Map<String, Object> deleteReportedReview(Long id, Map<String, Object> body) { return post(local, "/internal/local/admin/review-reports/" + id + "/delete-review", body); }
    public List<Map<String, Object>> reviewReplies(Long reviewId) { return list(local, "/internal/local/admin/reviews/" + reviewId + "/replies"); }
    public Map<String, Object> addReviewReply(Long reviewId, Map<String, Object> body) { return post(local, "/internal/local/admin/reviews/" + reviewId + "/replies", body); }
    public void deleteReply(Long id) { delete(local, "/internal/local/admin/replies/" + id); }

    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", scalar(identity, "/internal/identity/admin/count"));
        result.put("totalOrders", scalar(traffic, "/internal/traffic/admin/order-count"));
        result.put("pendingPosts", scalar(community, "/internal/community/admin/pending-post-count"));
        result.put("pendingReports", scalar(local, "/internal/local/admin/pending-report-count"));
        return result;
    }

    private List<Map<String, Object>> list(RestClient client, String path) {
        try {
            List<Map<String, Object>> body = client.get().uri(path).header("X-Internal-Token", token)
                    .retrieve().body(new ParameterizedTypeReference<>() {});
            return body == null ? List.of() : body;
        } catch (RestClientException e) {
            throw translateFailure(e);
        }
    }

    private Map<String, Object> post(RestClient client, String path, Object body) {
        try {
            Map<String, Object> result = client.post().uri(path).header("X-Internal-Token", token).body(body)
                    .retrieve().body(new ParameterizedTypeReference<>() {});
            return result == null ? Map.of() : result;
        } catch (RestClientException e) {
            throw translateFailure(e);
        }
    }

    private String postText(RestClient client, String path, Object body) {
        try {
            String result = client.post().uri(path).header("X-Internal-Token", token).body(body)
                    .retrieve().body(String.class);
            return result == null ? "" : result;
        } catch (RestClientException e) {
            throw translateFailure(e);
        }
    }

    private void put(RestClient client, String path, Object body) {
        try {
            client.put().uri(path).header("X-Internal-Token", token).body(body).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw translateFailure(e);
        }
    }

    private void delete(RestClient client, String path) {
        try {
            client.delete().uri(path).header("X-Internal-Token", token).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw translateFailure(e);
        }
    }

    private long scalar(RestClient client, String path) {
        try {
            Number result = client.get().uri(path).header("X-Internal-Token", token).retrieve().body(Number.class);
            return result == null ? 0 : result.longValue();
        } catch (RestClientException e) {
            throw translateFailure(e);
        }
    }

    ResponseStatusException translateFailure(RestClientException cause) {
        if (cause instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            if (status >= 400 && status < 500 && status != 401 && status != 403) {
                return new ResponseStatusException(response.getStatusCode(), responseMessage(response), cause);
            }
        }
        return unavailable(cause);
    }

    private String responseMessage(RestClientResponseException response) {
        try {
            JsonNode body = objectMapper.readTree(response.getResponseBodyAsByteArray());
            String message = body == null ? null : body.path("msg").asText(null);
            if (message != null && !message.isBlank()) return message;
        } catch (Exception ignored) {
            // 下游返回非标准响应时，仍保留对应的 4xx 状态并使用通用提示。
        }
        return "提交内容不符合要求，请检查后重试";
    }

    private ResponseStatusException unavailable(RestClientException cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "业务服务暂不可用，请稍后重试", cause);
    }

    private RestClient buildClient(RestClient.Builder builder, String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalArgumentException("内部服务超时时间必须大于 0");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return builder.clone().requestFactory(requestFactory).baseUrl(baseUrl).build();
    }
}
