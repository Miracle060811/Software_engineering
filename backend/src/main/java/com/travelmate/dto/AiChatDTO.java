package com.travelmate.dto;

import lombok.Data;

@Data
public class AiChatDTO {
    private String sessionId;
    private String message;
    private String clientDate;
    private String clientTimeZone;
}
