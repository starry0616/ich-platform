package com.yuxi.ich.common.exception;

import com.yuxi.ich.common.response.ResultCode;

/**
 * 业务异常。
 *
 * <p>Service 层校验不通过时抛出，由全局异常处理器统一转换为 Result，
 * 避免在每个 Controller 里手写 if-else 返回错误结构。
 *
 * <pre>
 * if (activity.getQuota() &lt;= activity.getSigned()) {
 *     throw new BizException(ResultCode.CONFLICT, "活动名额已满");
 * }
 * </pre>
 */
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
