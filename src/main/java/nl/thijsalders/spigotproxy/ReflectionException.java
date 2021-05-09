package nl.thijsalders.spigotproxy;

public class ReflectionException extends RuntimeException {

    public ReflectionException(Throwable throwable) {
        super("An exception occurred during the reflection process", throwable);
    }


    public ReflectionException(String message) {
        super(message);
    }


    public ReflectionException(String message, Throwable throwable) {
        super(message, throwable);
    }


    public ReflectionException() {
        super();
    }

}