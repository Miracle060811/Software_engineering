package com.travelmate.service;

import com.travelmate.dto.TrainWaitlistCreateDTO;
import com.travelmate.entity.TrainWaitlist;

import java.util.List;

public interface TrainWaitlistService {
    Long createWaitlist(Long userId, TrainWaitlistCreateDTO dto);

    List<TrainWaitlist> listWaitlists(Long userId);

    void cancelWaitlist(Long userId, Long waitlistId);
}
