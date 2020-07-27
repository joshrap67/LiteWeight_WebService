package helpers;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ResultStatus<T> {

  public boolean success;
  public T data;
  public String resultMessage;

  public ResultStatus() {
    this.success = false;
    this.data = null;
    this.resultMessage = "Error.";
  }

  public ResultStatus(boolean success, String resultMessage) {
    this.success = success;
    this.resultMessage = resultMessage;
  }

  public String toString() {
    return "{\"success\":\"" + (this.success ? "true" : "false")
        + "\",\"resultMessage\":\"" + this.resultMessage + "\"}";
  }

  public static <T> ResultStatus<T> successful(final String resultMessage) {
    return new ResultStatus<>(true, resultMessage);
  }

  public static <T> ResultStatus<T> successful(final String resultMessage, final T data) {
    return new ResultStatus<>(true, data, resultMessage);
  }

  public static <T> ResultStatus<T> failure(final String errorMessage) {
    return new ResultStatus<>(false, "Error: " + errorMessage);
  }
}
