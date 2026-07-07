package com.hufng.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonResultTest {

    @Test
    void success_withDataOnly_usesSuccessCodeAndDefaultMessage() {
        CommonResult<String> result = CommonResult.success("payload");

        assertThat(result.getCode()).isEqualTo(ResultCode.SUCCESS.getCode());
        assertThat(result.getMessage()).isEqualTo(ResultCode.SUCCESS.getMessage());
        assertThat(result.getData()).isEqualTo("payload");
    }

    @Test
    void success_withCustomMessage_overridesDefaultMessage() {
        CommonResult<String> result = CommonResult.success("payload", "custom message");

        assertThat(result.getCode()).isEqualTo(ResultCode.SUCCESS.getCode());
        assertThat(result.getMessage()).isEqualTo("custom message");
        assertThat(result.getData()).isEqualTo("payload");
    }

    @Test
    void failed_withErrorCode_usesErrorCodeAndMessageAndNullData() {
        CommonResult<Object> result = CommonResult.failed(ResultCode.FORBIDDEN);

        assertThat(result.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode());
        assertThat(result.getMessage()).isEqualTo(ResultCode.FORBIDDEN.getMessage());
        assertThat(result.getData()).isNull();
    }

    @Test
    void failed_withErrorCodeAndCustomMessage_overridesMessage() {
        CommonResult<Object> result = CommonResult.failed(ResultCode.FORBIDDEN, "custom message");

        assertThat(result.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode());
        assertThat(result.getMessage()).isEqualTo("custom message");
        assertThat(result.getData()).isNull();
    }

    @Test
    void failed_withMessageOnly_usesFailedCode() {
        CommonResult<Object> result = CommonResult.failed("something went wrong");

        assertThat(result.getCode()).isEqualTo(ResultCode.FAILED.getCode());
        assertThat(result.getMessage()).isEqualTo("something went wrong");
        assertThat(result.getData()).isNull();
    }

    @Test
    void failed_withNoArgs_usesFailedCodeAndDefaultMessage() {
        CommonResult<Object> result = CommonResult.failed();

        assertThat(result.getCode()).isEqualTo(ResultCode.FAILED.getCode());
        assertThat(result.getMessage()).isEqualTo(ResultCode.FAILED.getMessage());
        assertThat(result.getData()).isNull();
    }

    @Test
    void validateFailed_withNoArgs_usesValidateFailedCodeAndDefaultMessage() {
        CommonResult<Object> result = CommonResult.validateFailed();

        assertThat(result.getCode()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
        assertThat(result.getMessage()).isEqualTo(ResultCode.VALIDATE_FAILED.getMessage());
        assertThat(result.getData()).isNull();
    }

    @Test
    void validateFailed_withCustomMessage_overridesMessage() {
        CommonResult<Object> result = CommonResult.validateFailed("field X is required");

        assertThat(result.getCode()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
        assertThat(result.getMessage()).isEqualTo("field X is required");
        assertThat(result.getData()).isNull();
    }

    @Test
    void unauthorized_usesUnauthorizedCodeAndMessage() {
        CommonResult<String> result = CommonResult.unauthorized("session-data");

        assertThat(result.getCode()).isEqualTo(ResultCode.UNAUTHORIZED.getCode());
        assertThat(result.getMessage()).isEqualTo(ResultCode.UNAUTHORIZED.getMessage());
        assertThat(result.getData()).isEqualTo("session-data");
    }

    @Test
    void forbidden_usesForbiddenCodeAndMessage() {
        CommonResult<String> result = CommonResult.forbidden("some-data");

        assertThat(result.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode());
        assertThat(result.getMessage()).isEqualTo(ResultCode.FORBIDDEN.getMessage());
        assertThat(result.getData()).isEqualTo("some-data");
    }

    @Test
    void setters_updateFieldsReturnedByGetters() {
        CommonResult<String> result = CommonResult.success("initial");

        result.setCode(999L);
        result.setMessage("updated message");
        result.setData("updated data");

        assertThat(result.getCode()).isEqualTo(999L);
        assertThat(result.getMessage()).isEqualTo("updated message");
        assertThat(result.getData()).isEqualTo("updated data");
    }

    @Test
    void toString_serializesFieldsAsJson() {
        CommonResult<String> result = CommonResult.success("payload");

        assertThat(result.toString())
                .contains("\"code\":" + ResultCode.SUCCESS.getCode())
                .contains("\"message\":\"" + ResultCode.SUCCESS.getMessage() + "\"")
                .contains("\"data\":\"payload\"");
    }

    @Test
    void toString_omitsNullFields() {
        CommonResult<Object> result = CommonResult.failed();

        assertThat(result.toString()).doesNotContain("\"data\"");
    }
}
