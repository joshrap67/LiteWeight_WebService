package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class LoggingMessage<T> {

    public static final String WARNING = "WARNING";
    public static final String ERROR = "ERROR";

    private String loggingType;
    private T input;
    private String classMethod;
    private String requestId;
    private Exception exception;
    private String developerMessage;

    public LoggingMessage(String loggingLevel, T input, String classMethod, Exception exception) {
        this.loggingType = loggingLevel;
        this.input = input;
        this.classMethod = classMethod;
        this.exception = exception;
    }

    public LoggingMessage(String loggingType, String classMethod, Exception exception) {
        this.loggingType = loggingType;
        this.input = null;
        this.classMethod = classMethod;
        this.exception = exception;
    }

    public LoggingMessage(String loggingType, T input, String classMethod,
        String developerMessage) {
        this.loggingType = loggingType;
        this.input = input;
        this.classMethod = classMethod;
        this.developerMessage = developerMessage;
    }

    public LoggingMessage(String loggingType, String classMethod,
        String developerMessage) {
        this.loggingType = loggingType;
        this.input = null;
        this.classMethod = classMethod;
        this.developerMessage = developerMessage;
    }

    public LoggingMessage withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public LoggingMessage withInput(T input) {
        this.input = input;
        return this;
    }

    public String toString() {
        String retString = "[" + this.loggingType + "] requestId: " + this.requestId;

        if (this.classMethod != null) {
            retString += "\n\tlocation: " + this.classMethod;
        }

        if (this.input != null) {
            try {
                retString +=
                    "\n\tinput: " + JsonHelper
                        .serializeMap((Map<String, Object>) this.input);
            } catch (JsonProcessingException e) {
                retString += "Error";
            }
        }

        if (this.developerMessage != null) {
            retString += "\n\tmessage: " + this.developerMessage;
        }

        if (this.exception != null) {
            // convert entire exception into string
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            this.exception.printStackTrace(pw);
            retString += "\n\texception: " + sw.toString();
        }

        return retString;
    }
}
