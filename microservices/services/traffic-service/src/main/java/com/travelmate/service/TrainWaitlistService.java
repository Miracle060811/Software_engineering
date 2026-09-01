package com.travelmate.service;

import com.travelmate.dto.TrainWaitlistCreateDTO;

public interface TrainWaitlistService {
    Long createWaitlist(Long userId, TrainWaitlistCreateDTO dto);
}
