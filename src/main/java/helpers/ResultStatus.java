package helpers;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ResultStatus<T> {

    public static final int SUCCESS_CODE = 200;
    public static final int BAD_REQUEST = 400;
    public static final int BAD_ENTITY = 422;

    public Integer responseCode;
    public T data;
    public String resultMessage;

    public ResultStatus() {
        this.responseCode = BAD_REQUEST;
        this.data = null;
        this.resultMessage = "Error.";
    }

    public ResultStatus(int responseCode, String resultMessage) {
        this.responseCode = responseCode;
        this.resultMessage = resultMessage;
    }

//    public String toString() {
//        return "{\"success\":\"" + (this.responseCode ? "true" : "false")
//            + "\",\"resultMessage\":\"" + this.resultMessage + "\"}";
//    }

    public static <T> ResultStatus<T> successful(final String resultMessage) {
        return new ResultStatus<>(SUCCESS_CODE, resultMessage);
    }

    public static <T> ResultStatus<T> successful(final String resultMessage, final T data) {
        return new ResultStatus<>(SUCCESS_CODE, data, resultMessage);
    }

    public static <T> ResultStatus<T> failureBadRequest(final String errorMessage) {
        return new ResultStatus<>(BAD_REQUEST, "Error: " + errorMessage);
    }

    public static <T> ResultStatus<T> failureBadEntity(final String errorMessage) {
        return new ResultStatus<>(BAD_ENTITY, "Error: " + errorMessage);
    }
}
