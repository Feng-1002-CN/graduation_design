package com.graduation.competition.common;

import java.util.List;

/**
 * 分页响应，页码从 1 开始。
 */
public record PageResult<T>(List<T> records, long total, int page, int size) {
}

