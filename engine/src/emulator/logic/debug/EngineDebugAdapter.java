package emulator.logic.debug;

import emulator.api.EmulatorEngine;
import emulator.api.debug.DebugSession;
import emulator.api.debug.DebugSnapshot;
import emulator.api.dto.RunResult;

import java.lang.reflect.Method;
import java.util.*;

public class EngineDebugAdapter implements DebugSession {
    private final EmulatorEngine engine;
    private boolean directAvailable = false;
    private Method mDebugStart, mStepOver, mResume, mStop, mIsFinished, mCurrentPC, mCycles, mVarsSnapshot;
    private boolean alive = false;
    private List<DebugSnapshot> timeline = List.of();
    private int idx = -1;

    public EngineDebugAdapter(EmulatorEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
        tryBindDirectMode();
    }

    @Override
    public DebugSnapshot start(Long[] inputs, int degree) throws Exception {
        if (directAvailable) {
            // DIRECT
            invoke(mDebugStart, inputs, degree);
            alive = true;
            return directSnapshot();
        } else {
            // REPLAY
            RunResult rr = engine.run(degree, inputs);
            this.timeline = buildTimelineFromHistoryOrFallback(rr);
            this.idx = (timeline.isEmpty() ? -1 : 0);
            this.alive = !timeline.isEmpty();
            return current();
        }
    }

    @Override
    public DebugSnapshot stepOver() throws Exception {
        if (directAvailable) {
            invoke(mStepOver);
            return directSnapshot();
        } else {
            if (!alive || timeline.isEmpty()) return current();
            idx = Math.min(idx + 1, timeline.size() - 1);
            if (idx == timeline.size() - 1) alive = false;
            return current();
        }
    }

    @Override
    public DebugSnapshot resume() throws Exception {
        if (directAvailable) {
            invoke(mResume);
            return directSnapshot();
        } else {
            if (!alive || timeline.isEmpty()) return current();
            idx = timeline.size() - 1;
            alive = false;
            return current();
        }
    }

    @Override
    public void stop() throws Exception {
        if (directAvailable) {
            invoke(mStop);
        }
        alive = false;
    }

    @Override
    public boolean isAlive() {
        if (directAvailable) {
            try { return !(boolean) invoke(mIsFinished); }
            catch (Exception ignore) { return false; }
        }
        return alive;
    }

    private void tryBindDirectMode() {
        try {
            Class<?> cls = engine.getClass();
            mDebugStart = cls.getMethod("debugStart", Long[].class, int.class);
            mStepOver = cls.getMethod("debugStepOver");
            mResume = cls.getMethod("debugResume");
            mStop = cls.getMethod("debugStop");
            mIsFinished = cls.getMethod("debugIsFinished");
            mCurrentPC = cls.getMethod("debugCurrentPC");
            mCycles = cls.getMethod("debugCycles");
            mVarsSnapshot = cls.getMethod("debugVarsSnapshot");
            directAvailable = true;
        } catch (NoSuchMethodException e) {
            directAvailable = false;
        }
    }

    private DebugSnapshot directSnapshot() throws Exception {
        int pc     = safeInt(invoke(mCurrentPC));
        int cycles = safeInt(invoke(mCycles));
        @SuppressWarnings("unchecked")
        Map<String, String> vars = (Map<String, String>) invoke(mVarsSnapshot);
        boolean fin = (boolean) invoke(mIsFinished);
        return new DebugSnapshot(pc, vars == null ? Map.of() : vars, cycles, fin);
    }

    private Object invoke(Method m, Object... args) throws Exception {
        if (m == null) throw new IllegalStateException("Direct-mode method not bound");
        return m.invoke(engine, args);
    }

    private static int safeInt(Object o) {
        return (o instanceof Number n) ? n.intValue() : 0;
    }

    private DebugSnapshot current() {
        return (timeline.isEmpty() || idx < 0) ?
                new DebugSnapshot(0, Map.of(), 0, true) :
                timeline.get(idx);
    }

    private List<DebugSnapshot> buildTimelineFromHistoryOrFallback(RunResult rr) {
        Object history = engine.history();
        List<DebugSnapshot> out = new ArrayList<>();
        if (history instanceof Iterable<?> it) {
            for (Object rec : it) out.add(snapFromHistoryRecord(rec));
        }
        if (!out.isEmpty()) return out;

        Map<String,String> finalVars = snapshotVarsFromRunResult(rr);
        int finalPc     =  Math.max(0, inferLastPC(rr));
        int finalCycles =  inferCycles(rr);
        return List.of(new DebugSnapshot(finalPc, finalVars, finalCycles, true));
    }

    @SuppressWarnings("unchecked")
    private DebugSnapshot snapFromHistoryRecord(Object rec) {
        try {
            int pc = tryInvokeInt(rec, "getInstructionIndex", "pc", "getPc", "instructionIndex");
            int cycles = tryInvokeInt(rec, "getCycles", "cycles");
            Map<String,String> vars = (Map<String,String>) tryInvoke(rec, Map.class, "getVars", "vars", "getVariables", "variablesSnapshot");
            boolean fin = tryInvokeBool(rec, "isFinished", "finished");
            return new DebugSnapshot(pc, vars == null ? Map.of() : vars, cycles, fin);
        } catch (Exception e) {
            return new DebugSnapshot(0, Map.of(), 0, false);
        }
    }

    private Object tryInvoke(Object target, Class<?> expect, String... names) throws Exception {
        for (String n : names) {
            try {
                Method m = target.getClass().getMethod(n);
                Object v = m.invoke(target);
                if (v == null || expect.isInstance(v)) return v;
            } catch (NoSuchMethodException ignore) {}
        }
        throw new NoSuchMethodException("none of " + Arrays.toString(names));
    }

    private int tryInvokeInt(Object target, String... names) {
        for (String n : names) {
            try {
                Method m = target.getClass().getMethod(n);
                Object v = m.invoke(target);
                if (v instanceof Number num) return num.intValue();
            } catch (Exception ignore) {}
        }
        return 0;
    }

    private boolean tryInvokeBool(Object target, String... names) {
        for (String n : names) {
            try {
                Method m = target.getClass().getMethod(n);
                Object v = m.invoke(target);
                if (v instanceof Boolean b) return b;
            } catch (Exception ignore) {}
        }
        return false;
    }

    private int inferLastPC(RunResult rr) { return 0; }
    private int inferCycles(RunResult rr) { return 0; }
    private Map<String,String> snapshotVarsFromRunResult(RunResult rr) { return Map.of(); }
}
