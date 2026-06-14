package com.xuetupt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 * <p>
 * 所有 Controller 接口统一返回此格式，
 * 包含成功状态、错误信息、数据和总数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    /** 是否成功 */
    private Boolean success;
    /** 错误信息 */
    private String errorMsg;
    /** 响应数据 */
    private Object data;
    /** 数据总数（分页时使用） */
    private Long total;

    /** 返回成功响应（无数据） */
    public static Result ok() {
        return new Result(true, null, null, null);
    }

    /** 返回成功响应（带数据） */
    public static Result ok(Object data) {
        return new Result(true, null, data, null);
    }

    /** 返回成功响应（带数据和总数，用于分页） */
    public static Result ok(Object data, Long total) {
        return new Result(true, null, data, total);
    }

    /** 返回失败响应 */
    public static Result fail(String errorMsg) {
        return new Result(false, errorMsg, null, null);
    }
}
