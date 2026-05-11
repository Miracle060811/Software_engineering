package com.travelmate.service;

public interface SensitiveWordService {

    boolean containsSensitiveWord(String text);

    String filter(String text);
}
