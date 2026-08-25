package com.travelmate.service.impl;

import com.travelmate.service.TrainBrowserSyncService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class TrainStationResolver {
    private static final int MAX_ROUTE_CANDIDATES = 20;
    private static final Map<String, List<String>> CITY_STATIONS = new LinkedHashMap<>();

    private final TrainBrowserSyncService trainBrowserSyncService;

    static {
        city("北京", "北京南", "北京西", "北京", "北京丰台", "北京朝阳", "清河", "北京北");
        city("上海", "上海虹桥", "上海", "上海南", "上海西");
        city("广州", "广州南", "广州", "广州东", "广州白云", "广州北");
        city("深圳", "深圳北", "深圳", "福田", "深圳东", "深圳坪山");
        city("杭州", "杭州东", "杭州西", "杭州", "杭州南");
        city("南京", "南京南", "南京");
        city("苏州", "苏州北", "苏州", "苏州园区", "苏州新区");
        city("成都", "成都东", "成都西", "成都南", "成都");
        city("重庆", "重庆北", "重庆西", "重庆沙坪坝", "重庆");
        city("南昌", "南昌西", "南昌");
        city("泉州", "泉州", "泉州东", "泉州南");
        city("厦门", "厦门北", "厦门");
        city("福州", "福州南", "福州");
        city("长沙", "长沙南", "长沙");
        city("武汉", "武汉", "汉口", "武昌");
        city("西安", "西安北", "西安");
        city("郑州", "郑州东", "郑州");
        city("济南", "济南西", "济南", "济南东");
        city("青岛", "青岛北", "青岛", "青岛西");
        city("天津", "天津西", "天津", "天津南");
        city("昆明", "昆明南", "昆明");
        city("合肥", "合肥南", "合肥");
        city("石家庄", "石家庄", "石家庄东", "石家庄北");
        city("太原", "太原南", "太原");
        city("沈阳", "沈阳北", "沈阳南", "沈阳");
        city("长春", "长春西", "长春", "长春南");
        city("哈尔滨", "哈尔滨西", "哈尔滨", "哈尔滨北");
        city("呼和浩特", "呼和浩特东", "呼和浩特");
        city("兰州", "兰州西", "兰州");
        city("贵阳", "贵阳北", "贵阳东", "贵阳");
        city("南宁", "南宁东", "南宁");
        city("海口", "海口东", "海口");
        city("宁波", "宁波");
        city("温州", "温州南", "温州北", "温州");
        city("徐州", "徐州东", "徐州");
        city("无锡", "无锡东", "无锡", "无锡新区");
        city("常州", "常州北", "常州");
    }

    public TrainStationResolver(TrainBrowserSyncService trainBrowserSyncService) {
        this.trainBrowserSyncService = trainBrowserSyncService;
    }

    public List<RouteCandidate> routeCandidates(String depInput, String arrInput) {
        List<String> deps = stationsFor(depInput);
        List<String> arrs = stationsFor(arrInput);
        List<RouteCandidate> routes = new ArrayList<>();
        for (String dep : deps) {
            for (String arr : arrs) {
                if (dep.equals(arr)) {
                    continue;
                }
                routes.add(new RouteCandidate(dep, arr));
                if (routes.size() >= MAX_ROUTE_CANDIDATES) {
                    return routes;
                }
            }
        }
        return routes;
    }

    public List<String> stationsFor(String input) {
        String normalized = normalize(input);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        List<String> official = trainBrowserSyncService.resolveStationCandidates(normalized);
        List<String> mapped = CITY_STATIONS.get(normalized);
        if (mapped == null && official.size() <= 1) {
            return official.isEmpty() ? List.of(normalized) : official;
        }

        List<String> result = new ArrayList<>();
        if (mapped != null) {
            boolean catalogExpanded = official.size() > 1;
            for (String station : mapped) {
                if ((!catalogExpanded || official.contains(station)) && !result.contains(station)) {
                    result.add(station);
                }
            }
        }
        for (String station : official) {
            if (!result.contains(station)) {
                result.add(station);
            }
        }
        if (result.isEmpty()) {
            result.add(normalized);
        }
        return result;
    }

    public boolean isCityInput(String input) {
        return CITY_STATIONS.containsKey(normalize(input));
    }

    private static void city(String city, String... stations) {
        CITY_STATIONS.put(city, List.of(stations));
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().replace("　", "").toUpperCase(Locale.ROOT) : "";
    }

    public record RouteCandidate(String depStation, String arrStation) {
    }
}
