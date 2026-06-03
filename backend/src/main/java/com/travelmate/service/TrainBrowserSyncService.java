package com.travelmate.service;

import com.travelmate.dto.TrainBrowserTicket;

import java.time.LocalDate;
import java.util.List;

public interface TrainBrowserSyncService {
    List<TrainBrowserTicket> readPublicLeftTickets(
            String depStation,
            String depCode,
            String arrStation,
            String arrCode,
            LocalDate trainDate) throws Exception;
}
