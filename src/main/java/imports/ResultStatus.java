package imports;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ResultStatus<T> {

    public static final int SUCCESS_CODE = 200;
    public static final int BAD_REQUEST = 400;

    public Integer responseCode;
    public T data;
    public String resultMessage;
    public boolean success;

    public ResultStatus() {
        this.responseCode = BAD_REQUEST;
        this.data = null;
        this.success = false;
        this.resultMessage = "Error.";
    }

    public ResultStatus(int responseCode, String resultMessage) {
        this.responseCode = responseCode;
        this.resultMessage = resultMessage;
        this.success = responseCode == SUCCESS_CODE;
    }

    public static <T> ResultStatus<T> successful(final String resultMessage) {
        return new ResultStatus<>(SUCCESS_CODE, resultMessage);
    }

    public static <T> ResultStatus<T> successful(final String resultMessage, final T data) {
        return new ResultStatus<>(SUCCESS_CODE, data, resultMessage, true);
    }

    public static <T> ResultStatus<T> failureBadRequest(final String errorMessage) {
        return new ResultStatus<>(BAD_REQUEST, "Error: " + errorMessage);
    }
}
