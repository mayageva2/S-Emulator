package emulator.logic.xml;

public class XmlResult<T> {
    private final boolean success;
    private final T value;
    private final String error;

    private XmlResult(boolean success, T value, String error) {
        this.success = success;
        this.value = value;
        this.error = error;
    }

    public static <T> XmlResult<T> ok(T value) {
        return new XmlResult<>(true, value, null);
    }

    public static <T> XmlResult<T> error(String message) {
        return new XmlResult<>(false, null, message);
    }

    public boolean isSuccess() { return success; }
    public T getValue() { return value; }
    public String getError() { return error; }
}
