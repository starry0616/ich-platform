package com.yuxi.ich.ai.service;

import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AI 回答长度保护纯单元测试（不启动 Spring、不连数据库）。
 *
 * <p>验证 {@link AiService#assertAnswerLength(String)}：正常长度放行，
 * 超过 {@code MAX_ANSWER_LENGTH} 抛 400 业务异常，且不做静默截断。
 */
@DisplayName("AI 回答长度保护")
class AiAnswerLengthUnitTests {

    @Test
    @DisplayName("空回答不校验、直接放行")
    void nullAnswerPasses() {
        assertThatCode(() -> AiService.assertAnswerLength(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("正常长度回答放行")
    void normalAnswerPasses() {
        assertThatCode(() -> AiService.assertAnswerLength("这是一段正常长度的非遗问答回答。"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("正好达到上限 8000 字放行")
    void exactlyMaxPasses() {
        String answer = "答".repeat(AiService.MAX_ANSWER_LENGTH);
        assertThatCode(() -> AiService.assertAnswerLength(answer)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("超过上限 8001 字抛 400 业务异常")
    void overMaxThrows() {
        String answer = "答".repeat(AiService.MAX_ANSWER_LENGTH + 1);
        assertThatThrownBy(() -> AiService.assertAnswerLength(answer))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException be = (BizException) ex;
                    assertThat(be.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                });
    }
}
