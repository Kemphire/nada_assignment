package nada.exceptions;

public class SameUsersException extends HttpExceptions {
    public SameUsersException(String msg) {
        super(msg, 400);
    }
}
