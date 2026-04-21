package com.back.exam.service.impl;

import com.back.exam.entity.ExamRecord;
import com.back.exam.service.ExamService;
import com.back.exam.service.KimiGradingService;
import com.back.exam.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KimiGradingServiceImpl implements KimiGradingService {

    private static final long GRADING_LOCK_TTL_SECONDS = 15 * 60;

    @Autowired
    private ExamService examService;

    @Autowired
    private RedisUtils redisUtils;

    @Override
    @Async("gradingTaskExecutor")
    public void enqueueGrading(Integer examRecordId) {
        processGrading(examRecordId, false);
    }

    @Override
    @Async("gradingTaskExecutor")
    public void retryGrading(Integer examRecordId) {
        processGrading(examRecordId, true);
    }

    private void processGrading(Integer examRecordId, boolean allowRetryFailed) {
        if (examRecordId == null) {
            return;
        }

        String lockKey = buildLockKey(examRecordId);
        Boolean locked = redisUtils.setIfAbsent(lockKey, "1", GRADING_LOCK_TTL_SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            log.info("Skip duplicate grading task for examRecordId={}", examRecordId);
            return;
        }

        try {
            ExamRecord examRecord = examService.getById(examRecordId);
            if (examRecord == null) {
                log.warn("Exam record not found when grading async, examRecordId={}", examRecordId);
                return;
            }

            String status = examRecord.getStatus();
            if (ExamRecord.STATUS_GRADED.equals(status)) {
                return;
            }
            if (ExamRecord.STATUS_GRADING.equals(status) && !allowRetryFailed) {
                return;
            }
            if (ExamRecord.STATUS_GRADE_FAILED.equals(status) && !allowRetryFailed) {
                return;
            }

            examService.performGradingTask(examRecordId);
        } catch (Exception e) {
            log.error("Async grading failed, examRecordId={}", examRecordId, e);
        } finally {
            redisUtils.delete(lockKey);
        }
    }

    private String buildLockKey(Integer examRecordId) {
        return "exam:grading:lock:" + examRecordId;
    }
}
