package helpers;

public class WarningMessage<T> extends LoggingMessage<T>{

  public WarningMessage(T input, String classMethod, Exception exception) {
    super(LoggingMessage.WARNING, input, classMethod, exception);
  }

  public WarningMessage(String classMethod, Exception exception) {
    super(LoggingMessage.WARNING, classMethod, exception);
  }

  public WarningMessage(T input, String classMethod, String developerMessage) {
    super(LoggingMessage.WARNING, input, classMethod, developerMessage);
  }

  public WarningMessage(String classMethod, String developerMessage) {
    super(LoggingMessage.WARNING, classMethod, developerMessage);
  }

}
