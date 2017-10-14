package bot.bittrex;

public class ReconnectionAttemptsExceededException extends RuntimeException{

    public ReconnectionAttemptsExceededException() { super(); }
    public ReconnectionAttemptsExceededException(String message) { super(message); }
    public ReconnectionAttemptsExceededException(String message, Throwable cause) { super(message, cause); }
    public ReconnectionAttemptsExceededException(Throwable cause) { super(cause); }

}
