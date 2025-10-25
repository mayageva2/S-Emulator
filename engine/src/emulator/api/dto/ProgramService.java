package emulator.api.dto;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProgramService {
    private static final Map<String, ProgramStats> programs = new ConcurrentHashMap<>();
    private static final ProgramService instance = new ProgramService();

    private ProgramService() {}

    public static ProgramService getInstance() {return instance;}

    public void addProgram(String programName, String username,
                           int instructionCount, int maxDegree) {
        programs.put(programName, new ProgramStats(
                programName,
                username,
                instructionCount,
                maxDegree,
                0,
                0.0
        ));
    }

    public void recordRun(String programName, double creditCost) {
        ProgramStats old = programs.get(programName);
        if (old == null) return;
        int newRunCount = old.getRunCount() + 1;
        double newAvgCost = (old.getAvgCreditCost() * old.getRunCount() + creditCost) / newRunCount;

        programs.put(programName, new ProgramStats(
                old.getProgramName(),
                old.getUsername(),
                old.getInstructionCount(),
                old.getMaxDegree(),
                newRunCount,
                newAvgCost
        ));
    }

    public List<ProgramStats> getAllPrograms() {
        List<ProgramStats> result = new ArrayList<>();
        for (ProgramStats p : programs.values()) {
            double avg = ProgramStatsRepository.getInstance().getAverage(p.getProgramName());
            result.add(new ProgramStats(
                    p.getProgramName(),
                    p.getUsername(),
                    p.getInstructionCount(),
                    p.getMaxDegree(),
                    p.getRunCount(),
                    avg
            ));
        }
        return result;
    }

    public boolean programExists(String programName) {
        return programs.containsKey(programName);
    }

    public void clear() {programs.clear();}
}
