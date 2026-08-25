package com.travelmate.service.impl;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import com.travelmate.dto.TrainBrowserTicket;
import com.travelmate.service.TrainBrowserSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TrainBrowserSyncServiceImpl implements TrainBrowserSyncService {
    private static final String INIT_URL = "https://kyfw.12306.cn/otn/leftTicket/init";
    private static final String WAIT_FOR_RESULTS_SCRIPT = """
            () => {
              const body = document.body ? document.body.innerText : '';
              return document.querySelectorAll('#queryLeftTable tr[id^="ticket_"]').length > 0
                || /操作频率过快|网络可能存在问题|没有查询到符合条件的车次|系统忙|查询失败/.test(body);
            }
            """;
    private static final String READ_ROWS_SCRIPT = """
            () => Array.from(document.querySelectorAll('#queryLeftTable tr[id^="ticket_"]'))
              .map((row) => {
                const clean = (value) => (value || '').replace(/\\s+/g, ' ').trim();
                const tds = Array.from(row.querySelectorAll('td')).map((td) => clean(td.innerText));
                const stations = Array.from(row.querySelectorAll('.cdz strong')).map((node) => clean(node.innerText || node.getAttribute('title')));
                const times = Array.from(row.querySelectorAll('.cds strong')).map((node) => clean(node.innerText));
                const durationNode = row.querySelector('.ls strong') || row.querySelectorAll('strong')[4];
                const allText = clean(row.innerText);
                const trainNo = clean(row.querySelector('.number')?.innerText) || (allText.match(/[GDCZTKY]?\\d{1,5}/)?.[0] || '');
                return {
                  trainNo,
                  departureStation: stations[0] || '',
                  arrivalStation: stations[1] || '',
                  departureTime: times[0] || '',
                  arrivalTime: times[1] || '',
                  duration: clean(durationNode?.innerText) || '',
                  businessSeat: tds[1] || '',
                  firstClassSeat: tds[3] || tds[2] || '',
                  secondClassSeat: tds[4] || '',
                  hardSeat: tds[9] || '',
                  noSeat: tds[10] || ''
                };
              })
              .filter((item) => item.trainNo && item.departureStation && item.arrivalStation);
            """;

    @Value("${train.browser-sync.headless:true}")
    private boolean headless;

    // 外部 12306 页面不可用时必须尽快回退到本地车次数据，不能占满搜索接口超时时间。
    @Value("${train.browser-sync.timeout-ms:6000}")
    private double timeoutMs;

    @Override
    public List<TrainBrowserTicket> readPublicLeftTickets(
            String depStation,
            String depCode,
            String arrStation,
            String arrCode,
            LocalDate trainDate) throws Exception {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                     .setHeadless(headless)
                     .setTimeout(timeoutMs))) {
            Page page = browser.newPage();
            page.setDefaultTimeout(timeoutMs);
            page.navigate(INIT_URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            fillQueryForm(page, depStation, depCode, arrStation, arrCode, trainDate);
            page.locator("#query_ticket").click();
            page.waitForFunction(WAIT_FOR_RESULTS_SCRIPT, null, new Page.WaitForFunctionOptions().setTimeout(timeoutMs));

            String bodyText = (String) page.evaluate("() => document.body ? document.body.innerText : ''");
            String failureReason = detectPageFailure(bodyText);
            int rowCount = ((Number) page.evaluate("() => document.querySelectorAll('#queryLeftTable tr[id^=\"ticket_\"]').length")).intValue();
            if (StringUtils.hasText(failureReason) && rowCount == 0) {
                throw new IllegalStateException(failureReason);
            }
            if (rowCount == 0) {
                throw new IllegalStateException("12306 page did not render a left-ticket table");
            }

            return readRows(page, depStation, arrStation);
        }
    }

    private void fillQueryForm(Page page, String depStation, String depCode, String arrStation, String arrCode,
                               LocalDate trainDate) {
        try {
            page.waitForFunction("() => typeof window.station_names === 'string' && window.station_names.includes('@')",
                    null, new Page.WaitForFunctionOptions().setTimeout(5000));
        } catch (Exception ignored) {
            // Some 12306 page variants defer the station list; manual text entry below is still attempted.
        }

        Locator from = page.locator("#fromStationText");
        from.click();
        from.fill(depStation);
        page.keyboard().press("Enter");

        Locator to = page.locator("#toStationText");
        to.click();
        to.fill(arrStation);
        page.keyboard().press("Enter");

        page.evaluate("""
                ({ depStation, depCode, arrStation, arrCode, trainDate }) => {
                  const resolveStationCode = (stationName) => {
                    const names = window.station_names || '';
                    const match = names.split('@')
                      .map((item) => item.split('|'))
                      .find((parts) => parts[1] === stationName);
                    return match ? match[2] : '';
                  };
                  const setValue = (selector, value) => {
                    const el = document.querySelector(selector);
                    if (!el) return;
                    el.removeAttribute('readonly');
                    el.value = value;
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                  };
                  const resolvedDepCode = depCode || resolveStationCode(depStation);
                  const resolvedArrCode = arrCode || resolveStationCode(arrStation);
                  setValue('#fromStationText', depStation);
                  if (resolvedDepCode) setValue('#fromStation', resolvedDepCode);
                  setValue('#toStationText', arrStation);
                  if (resolvedArrCode) setValue('#toStation', resolvedArrCode);
                  setValue('#train_date', trainDate);
                }
                """, Map.of(
                "depStation", depStation,
                "depCode", safe(depCode),
                "arrStation", arrStation,
                "arrCode", safe(arrCode),
                "trainDate", trainDate.toString()));
    }

    @SuppressWarnings("unchecked")
    private List<TrainBrowserTicket> readRows(Page page, String depStation, String arrStation) {
        Object raw = page.evaluate(READ_ROWS_SCRIPT);
        if (!(raw instanceof List<?> rows)) {
            return List.of();
        }
        List<TrainBrowserTicket> tickets = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> map)) {
                continue;
            }
            TrainBrowserTicket ticket = new TrainBrowserTicket();
            ticket.setTrainNo(text(map.get("trainNo")));
            ticket.setDepartureStation(text(map.get("departureStation")));
            ticket.setArrivalStation(text(map.get("arrivalStation")));
            ticket.setDepartureTime(text(map.get("departureTime")));
            ticket.setArrivalTime(text(map.get("arrivalTime")));
            ticket.setDuration(text(map.get("duration")));
            ticket.setBusinessSeat(text(map.get("businessSeat")));
            ticket.setFirstClassSeat(text(map.get("firstClassSeat")));
            ticket.setSecondClassSeat(text(map.get("secondClassSeat")));
            ticket.setHardSeat(text(map.get("hardSeat")));
            ticket.setNoSeat(text(map.get("noSeat")));
            if (depStation.equals(ticket.getDepartureStation()) && arrStation.equals(ticket.getArrivalStation())) {
                tickets.add(ticket);
            }
        }
        return tickets;
    }

    private String detectPageFailure(String bodyText) {
        if (!StringUtils.hasText(bodyText)) {
            return "12306 page returned an empty response";
        }
        if (bodyText.contains("操作频率过快")) {
            return "12306 page提示操作频率过快";
        }
        if (bodyText.contains("网络可能存在问题")) {
            return "12306 page提示网络可能存在问题";
        }
        if (bodyText.contains("没有查询到符合条件的车次")) {
            return "12306 page提示没有查询到符合条件的车次";
        }
        if (bodyText.contains("系统忙")) {
            return "12306 page提示系统忙";
        }
        if (bodyText.contains("查询失败")) {
            return "12306 page提示查询失败";
        }
        return null;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
