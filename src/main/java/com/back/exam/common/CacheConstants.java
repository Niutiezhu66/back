package com.back.exam.common;

public class CacheConstants {

    public static final String QUESTION_CACHE = "question";

    public static final String PAPER_CACHE = "paper";

    public static final String EXAM_RECORD_CACHE = "exam_record";

    public static final String QUESTION_DETAIL_KEY = "question:detail:";

    public static final String QUESTION_CATEGORY_KEY = "question:category:";

    public static final String PAPER_DETAIL_KEY = "paper:detail:";

    public static final String EXAM_RECORD_DETAIL_KEY = "exam_record:detail:";

    public static final String POPULAR_QUESTIONS_KEY = "question:popular";

    public static final String QUESTION_VIEW_COUNT_KEY = "question:view_count";

    public static final int POPULAR_QUESTIONS_COUNT = 10;

    public static final long DEFAULT_EXPIRE_SECONDS = 1800; // 30分钟

    public static final long HOT_DATA_EXPIRE_SECONDS = 3600; // 1小时

    public static final long WEEKLY_STATS_EXPIRE_SECONDS = 604800; // 7天
}