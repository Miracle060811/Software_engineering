package com.travelmate.service;

import com.travelmate.dto.TrainLiveSyncStatus;
import com.travelmate.entity.Train;

import java.util.List;

public interface TrainLiveSyncService {
    TrainLiveSyncStatus syncIfSupported(String depStation, String arrStation, String date);

    List<Train> getCachedLiveTrains(String depStation, String arrStation, String date);

    TrainLiveSyncStatus getLastStatus();
}
