package nada.exceptions;

public class HttpExceptions extends Exception {

    private final Integer statusCode;

    public HttpExceptions(String msg, Integer statusCode) {
        super(msg);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
