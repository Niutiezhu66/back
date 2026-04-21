package com.back.exam.service;

public interface KimiGradingService {

    void enqueueGrading(Integer examRecordId);

    void retryGrading(Integer examRecordId);
}
