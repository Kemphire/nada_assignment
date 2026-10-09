package nada.exceptions;

public class UserNotFoundException extends HttpExceptions {
    public UserNotFoundException(String msg) {
        super(msg, 404);
    }
}
