package com.travelmate.dto;

import lombok.Data;

@Data
public class PrivateMessageSendDTO {
    private Long receiverId;
    private String content;
}
