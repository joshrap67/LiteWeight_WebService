package utils;

public class Parser {
    public static Integer convertObjectToInteger(Object object) {
        if (object != null) {
            return Integer.parseInt(object.toString());
        } else {
            return null;
        }
    }

    public static Double convertObjectToDouble(Object object) {
        if (object != null) {
            return Double.parseDouble(object.toString());
        } else {
            return null;
        }
    }
}
