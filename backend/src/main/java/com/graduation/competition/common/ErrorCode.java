package com.graduation.competition.common;

import lombok.Getter;

/**
 * 业务错误码。参数错误位于 400xx，资源错误位于 404xx，服务错误位于 500xx。
 */
@Getter
public enum ErrorCode {
    BAD_REQUEST(40001, "请求参数错误"),
    VALIDATION_FAILED(40002, "参数校验失败"),
    INVALID_FILE(40003, "文件类型或大小不符合要求"),
    RESOURCE_NOT_FOUND(40401, "资源不存在"),
    STORAGE_UNAVAILABLE(50001, "文件存储服务不可用"),
    VECTOR_STORE_UNAVAILABLE(50002, "向量库服务不可用"),
    MODEL_UNAVAILABLE(50003, "AI 模型服务不可用"),
    SERVICE_UNAVAILABLE(50004, "外部服务不可用"),
    INTERNAL_ERROR(50000, "服务内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

