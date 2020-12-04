package utils;

public class ErrorMessage<T> extends LoggingMessage<T> {

    public ErrorMessage(T input, String classMethod, Exception exception) {
        super(LoggingMessage.ERROR, input, classMethod, exception);
    }

    public ErrorMessage(String classMethod, Exception exception) {
        super(LoggingMessage.ERROR, classMethod, exception);
    }

    public ErrorMessage(T input, String classMethod, String developerMessage) {
        super(LoggingMessage.ERROR, input, classMethod, developerMessage);
    }

    public ErrorMessage(String classMethod, String developerMessage) {
        super(LoggingMessage.ERROR, classMethod, developerMessage);
    }
}
