package com.travelmate.microservices.ops;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminCsvImportService {
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, List<String>> REQUIRED = Map.of(
            "flights", List.of("flightNo","airline","departureCity","arrivalCity","departureTime","arrivalTime","economyPrice","businessPrice","totalSeats","availableSeats"),
            "trains", List.of("trainNo","trainType","departureStation","arrivalStation","departureTime","arrivalTime","firstClassPrice","secondClassPrice","firstClassSeats","secondClassSeats"),
            "hotels", List.of("name","city","address","starRating","avgPrice"),
            "rooms", List.of("hotelId","roomType","bedType","price","totalRooms","availableRooms"),
            "attractions", List.of("name","city","address","adultPrice","childPrice","totalTickets","availableTickets"),
            "destinations", List.of("slug","name","tag","img","desc","intro"));
    private static final Set<String> INTEGER_FIELDS = Set.of("totalSeats","availableSeats","durationMinutes","firstClassSeats","secondClassSeats","status","starRating","hotelId","area","totalRooms","availableRooms","totalTickets","availableTickets","sortOrder");
    private static final Set<String> DECIMAL_FIELDS = Set.of("economyPrice","businessPrice","firstClassPrice","secondClassPrice","avgPrice","lat","lng","score","price","adultPrice","childPrice");
    private final OpsAggregationGateway gateway;

    public AdminCsvImportService(OpsAggregationGateway gateway) { this.gateway = gateway; }

    public Map<String,Object> importCsv(String type, MultipartFile file, boolean dryRun, String mode) {
        String normalized = type == null ? "" : type.trim().toLowerCase();
        if (!REQUIRED.containsKey(normalized)) bad("不支持的导入类型：" + type);
        if (file == null || file.isEmpty()) bad("CSV 文件不能为空");
        if (file.getSize() > MAX_SIZE) bad("CSV 文件不能超过 5MB");
        boolean upsert = "upsert".equalsIgnoreCase(mode);
        int total=0, success=0, failed=0, inserted=0, updated=0, validated=0;
        List<Map<String,Object>> failures = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true).setTrim(true).build().parse(reader)) {
            List<String> headers = new ArrayList<>(parser.getHeaderMap().keySet());
            List<String> missing = REQUIRED.get(normalized).stream()
                    .filter(required -> headers.stream().map(this::normalize).noneMatch(normalize(required)::equals)).toList();
            if (!missing.isEmpty()) bad("CSV 缺少必填表头：" + String.join(", ",missing));
            for (CSVRecord record : parser) {
                Map<String,Object> row = row(headers, record);
                if (row.values().stream().allMatch(v -> v == null || v.toString().isBlank())) continue;
                total++;
                try {
                    validateRequired(normalized,row);
                    if (dryRun) validated++;
                    else if (gateway.importResource(normalized,row,upsert)) updated++;
                    else inserted++;
                    success++;
                } catch (Exception e) {
                    failed++;
                    if (failures.size()<20) failures.add(Map.of("line",record.getRecordNumber()+1,"reason",message(e)));
                }
            }
        } catch (ResponseStatusException e) { throw e;
        } catch (Exception e) { bad("导入失败：" + message(e)); }
        if (total==0) bad("CSV 至少需要一行有效数据");
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("type",normalized); result.put("mode",upsert?"upsert":"insert"); result.put("dryRun",dryRun);
        result.put("total",total); result.put("success",success); result.put("failed",failed);
        result.put("inserted",inserted); result.put("updated",updated); result.put("validated",validated);
        result.put("failures",failures); result.put("failureLimit",20); return result;
    }

    private Map<String,Object> row(List<String> headers, CSVRecord record) {
        Map<String,Object> row = new HashMap<>();
        for(int i=0;i<headers.size();i++) {
            String key=headers.get(i).replace("\uFEFF","").trim();
            String value=i<record.size()?record.get(i).trim():"";
            Object typed=value;
            if(!value.isBlank() && INTEGER_FIELDS.contains(key)) typed=Long.valueOf(value);
            else if(!value.isBlank() && DECIMAL_FIELDS.contains(key)) typed=new java.math.BigDecimal(value);
            row.put(key,typed);
        }
        return row;
    }
    private void validateRequired(String type, Map<String,Object> row) {
        for(String field:REQUIRED.get(type)) if(row.get(field)==null || row.get(field).toString().isBlank()) bad("缺少字段 " + field);
    }
    private String normalize(String value) { return value.replace("\uFEFF","").replace("_","").replace("-","").replace(" ","").toLowerCase(); }
    private String message(Exception e) { return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(); }
    private void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
}
