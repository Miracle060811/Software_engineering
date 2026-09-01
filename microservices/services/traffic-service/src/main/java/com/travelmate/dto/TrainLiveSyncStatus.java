package com.travelmate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainLiveSyncStatus {
    private boolean enabled;
    private boolean supportedRoute;
    private boolean synced;
    private String message;
    private String route;
    private String date;
    private String dataSource;
    private int trainCount;
    private LocalDateTime syncedAt;
}
