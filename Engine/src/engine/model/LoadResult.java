package engine.model;

import java.util.List;

public class LoadResult {
    private final boolean success;
    private final List<String> errors;

    public LoadResult(boolean success, List<String> errors) {
        this.success = success;
        this.errors = List.copyOf(errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getErrors() {
        return errors;
    }
}
