package emulator.api.dto;

@FunctionalInterface
public interface ProgressListener {
    void onProgress(String stage, double fraction);
}
