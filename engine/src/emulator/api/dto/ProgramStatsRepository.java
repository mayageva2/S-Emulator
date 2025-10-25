package emulator.api.dto;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProgramStatsRepository {
    private static final ProgramStatsRepository instance = new ProgramStatsRepository();
    private final Map<String, AvgData> averages = new ConcurrentHashMap<>();

    public static ProgramStatsRepository getInstance() {
        return instance;
    }

    public void updateAverage(String programName, long cycles) {
        String key = programName.toUpperCase(Locale.ROOT);
        averages.compute(key, (k, old) -> {
            if (old == null) return new AvgData(1, cycles);
            long newCount = old.count + 1;
            double newAvg = ((old.avg * old.count) + cycles) / newCount;
            return new AvgData(newCount, newAvg);
        });
    }

    public double getAverage(String programName) {
        AvgData data = averages.get(programName.toUpperCase(Locale.ROOT));
        return (data == null) ? 0.0 : data.avg;
    }

    public long getCount(String programName) {
        AvgData data = averages.get(programName.toUpperCase(Locale.ROOT));
        return (data == null) ? 0 : data.count;
    }

    private static class AvgData {
        final long count;
        final double avg;
        AvgData(long count, double avg) {
            this.count = count;
            this.avg = avg;
        }
    }
}
