package emulator.logic.debug;

import emulator.api.EmulatorEngine;
import emulator.api.debug.DebugSession;
import emulator.api.debug.DebugSnapshot;
import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;
import emulator.api.dto.RunResult;

import java.lang.reflect.Method;
import java.util.*;

public class EngineDebugAdapter implements DebugSession {
    private final EmulatorEngine engine;
    private final String programName;
    private String selectedProgram;
    private boolean directAvailable = false;
    private boolean directHasProgramParam = false;
    private Method mDebugStart, mDebugStartWithProgram, mStepOver, mResume, mStop, mIsFinished, mCurrentPC, mCycles, mVarsSnapshot, mCurrentRunResult;
    private boolean alive = false;
    private List<DebugSnapshot> timeline = List.of();
    private int idx = -1;
    private Map<String,String> lastKnownVars = new LinkedHashMap<>();
    private List<String> orderedVarNames = List.of();

    private int degreeAtStart = 0;
    @SuppressWarnings("unused")
    private Long[] inputsAtStart = new Long[0];

    public EngineDebugAdapter(EmulatorEngine engine, String programName) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.programName = programName;
        tryBindDirectMode();
    }

    public EngineDebugAdapter(EmulatorEngine engine) {
        this(engine, null);
        tryBindDirectMode();
    }

    public DebugSnapshot start(Long[] inputs, int degree, String programName) throws Exception {
        String mainName;
        try { mainName = engine.programView(0).programName(); }
        catch (Exception ex) { mainName = null; }

        String target = (programName != null && !programName.isBlank())
                ? programName
                : mainName;

        this.selectedProgram = target;
        this.degreeAtStart = degree;
        this.inputsAtStart = inputs != null ? inputs.clone() : new Long[0];
        ProgramView pvAtStart = null;
        try {
            pvAtStart = (selectedProgram != null && !selectedProgram.isBlank())
                    ? engine.programView(selectedProgram, this.degreeAtStart)
                    : engine.programView(this.degreeAtStart);
        } catch (Exception ignore) {}
        this.orderedVarNames = discoverAllVarNames(pvAtStart, this.inputsAtStart);
        this.lastKnownVars = new LinkedHashMap<>();
        for (String n : orderedVarNames) lastKnownVars.put(n, "-");

        boolean canUseDirectForThisTarget =
                directAvailable && (directHasProgramParam
                                || (mainName != null && mainName.equalsIgnoreCase(selectedProgram)));
        if (canUseDirectForThisTarget) {
            // DIRECT
            if (directHasProgramParam) {
                invoke(mDebugStartWithProgram, selectedProgram, inputs, degree);
            } else {
                invoke(mDebugStart, inputs, degree);
            }
            alive = true;
            DebugSnapshot snap = directSnapshot();
            return buildSnapshotWithState(snap.currentInstructionIndex(), snap.vars(), snap.cycles(), snap.finished());
        } else {
            // REPLAY
            RunResult rr = (selectedProgram != null && !selectedProgram.isBlank())
                    ? engine.run(selectedProgram, degree, inputs)
                    : engine.run(degree, inputs);
            this.timeline = buildTimelineFromHistoryOrFallback(rr);
            this.idx = (timeline.isEmpty() ? -1 : 0);
            this.alive = !timeline.isEmpty();
            DebugSnapshot cur = current();
            return buildSnapshotWithState(cur.currentInstructionIndex(), cur.vars(), cur.cycles(), cur.finished());
        }
    }

    @Override
    public DebugSnapshot start(Long[] inputs, int degree) throws Exception {
        return start(inputs, degree, this.programName);
    }

    @Override
    public DebugSnapshot stepOver() throws Exception {
        if (directAvailable) {
            invoke(mStepOver);
            DebugSnapshot s = directSnapshot();
            return buildSnapshotWithState(s.currentInstructionIndex(), s.vars(), s.cycles(), s.finished());
        } else {
            if (!alive || timeline.isEmpty()) return buildSnapshotWithState(current().currentInstructionIndex(), current().vars(), current().cycles(), current().finished());
            idx = Math.min(idx + 1, timeline.size() - 1);
            if (idx == timeline.size() - 1) alive = false;
            DebugSnapshot s = current();
            return buildSnapshotWithState(s.currentInstructionIndex(), s.vars(), s.cycles(), s.finished());
        }
    }

    @Override
    public DebugSnapshot resume() throws Exception {
        if (directAvailable) {
            invoke(mResume);
            DebugSnapshot s = directSnapshot();
            return buildSnapshotWithState(s.currentInstructionIndex(), s.vars(), s.cycles(), s.finished());
        } else {
            if (!alive || timeline.isEmpty()) return buildSnapshotWithState(current().currentInstructionIndex(), current().vars(), current().cycles(), current().finished());
            idx = timeline.size() - 1;
            alive = false;
            DebugSnapshot s = current();
            return buildSnapshotWithState(s.currentInstructionIndex(), s.vars(), s.cycles(), s.finished());
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
            try {
                mDebugStartWithProgram = cls.getMethod("debugStart", String.class, Long[].class, int.class);
                directHasProgramParam = true;
            } catch (NoSuchMethodException ignore) {
                mDebugStart = cls.getMethod("debugStart", Long[].class, int.class);
                directHasProgramParam = false;
            }

            mStepOver = cls.getMethod("debugStepOver");
            mResume = cls.getMethod("debugResume");
            mStop = cls.getMethod("debugStop");
            mIsFinished = cls.getMethod("debugIsFinished");
            mCurrentPC = cls.getMethod("debugCurrentPC");
            mCycles = cls.getMethod("debugCycles");
            mVarsSnapshot = tryGetMethod(cls,
                    "debugVarsSnapshot", "getDebugVarsSnapshot",
                    "varsSnapshot", "getVarsSnapshot",
                    "debugVariables", "getDebugVariables",
                    "variablesSnapshot", "getVariablesSnapshot"
            );
            mCurrentRunResult = tryGetMethod(cls,
                    "debugCurrentRunResult", "getDebugCurrentRunResult",
                    "currentRunResult", "getCurrentRunResult",
                    "debugResult", "getDebugResult",
                    "debugCurrentResult", "getDebugCurrentResult"
            );
            directAvailable = true;
        } catch (NoSuchMethodException e) {
            directAvailable = false;
        }
    }

    private static Method tryGetMethod(Class<?> cls, String... names) {
        for (String n : names) {
            try { return cls.getMethod(n); }
            catch (NoSuchMethodException ignore) {}
        }
        return null;
    }

    private DebugSnapshot directSnapshot() throws Exception {
        int pc = safeInt(invoke(mCurrentPC));
        int cycles = safeInt(invoke(mCycles));
        boolean fin = (boolean) invoke(mIsFinished);
        Map<String, String> vars = null;

        if (mVarsSnapshot != null) {
            @SuppressWarnings("unchecked")
            Map<String, String> raw = (Map<String, String>) invoke(mVarsSnapshot);
            if (raw != null && !raw.isEmpty()) {
                vars = raw;
            }
        }

        if (vars == null || vars.isEmpty()) {
            if (mCurrentRunResult != null) {
                RunResult rr = (RunResult) invoke(mCurrentRunResult);
                if (rr != null) {
                    vars = toVarsMap(rr, this.inputsAtStart);
                }
            }
        }

        if (vars == null || vars.isEmpty()) {
            Object history = engine.history();
            if (history instanceof Iterable<?> it) {
                Object last = null;
                for (Object rec : it) last = rec;
                if (last != null) {
                    DebugSnapshot hs = snapFromHistoryRecord(last);
                    if (hs != null) {
                        if ((vars == null || vars.isEmpty()) && hs.vars() != null && !hs.vars().isEmpty()) {
                            vars = hs.vars();
                        }
                        if (pc == 0 && hs.currentInstructionIndex() != 0) {
                            pc = hs.currentInstructionIndex();
                        }
                        if (cycles == 0 && hs.cycles() != 0) {
                            cycles = hs.cycles();
                        }
                    }
                }
            }
        }

        if (vars == null) vars = Map.of();
        return new DebugSnapshot(pc, vars, cycles, fin);
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

    @SuppressWarnings("unchecked")
    private List<DebugSnapshot> buildTimelineFromHistoryOrFallback(RunResult rr) {
        try {
            var m = engine.getClass().getMethod("debugTrace");
            Object v = m.invoke(engine);
            if (v instanceof List<?> list && !list.isEmpty()) {
                List<DebugSnapshot> out = new ArrayList<>(list.size());
                for (Object rec : list) {
                    int pcAfter = (int)  rec.getClass().getMethod("pcAfter").invoke(rec);
                    int cycles  = (int)  rec.getClass().getMethod("cycles").invoke(rec);
                    @SuppressWarnings("unchecked")
                    Map<String,String> vars = (Map<String,String>) rec.getClass().getMethod("vars").invoke(rec);
                    boolean finished = (boolean) rec.getClass().getMethod("finished").invoke(rec);
                    out.add(new DebugSnapshot(pcAfter, vars == null ? Map.of() : vars, cycles, finished));
                }
                return out;
            }
        } catch (NoSuchMethodException ignore) {
        } catch (Exception ignore) {
        }

        var synth = synthesizeTimelineFromProgramView(rr);
        if (!synth.isEmpty()) return synth;
        return List.of(new DebugSnapshot(0, Map.of(), 0, true));
    }

    private List<DebugSnapshot> synthesizeTimelineFromProgramView(RunResult rr) {
        try {
            ProgramView pv = (selectedProgram != null && !selectedProgram.isBlank())
                    ? engine.programView(selectedProgram, degreeAtStart)
                    : engine.programView(degreeAtStart);
            if (pv == null || pv.instructions() == null || pv.instructions().isEmpty()) return List.of();

            List<DebugSnapshot> out = new ArrayList<>();
            int cycles = 0;
            int pc = 0;

            for (InstructionView iv : pv.instructions()) {
                out.add(new DebugSnapshot(pc, Map.of(), cycles, false));
                cycles += Math.max(0, iv.cycles());
                pc++;
            }
            if (!out.isEmpty()) {
                Map<String,String> lastVars = (rr != null) ? toVarsMap(rr, this.inputsAtStart) : Map.of();
                DebugSnapshot last = out.get(out.size() - 1);
                out.set(out.size() - 1, new DebugSnapshot(last.currentInstructionIndex(), lastVars, cycles, true));
            }
            return out;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private boolean timelineHasProgress(List<DebugSnapshot> snaps) {
        if (snaps == null || snaps.size() < 2) return false;
        int prevPc = snaps.get(0).currentInstructionIndex();
        int prevCycles = snaps.get(0).cycles();
        for (int i = 1; i < snaps.size(); i++) {
            var s = snaps.get(i);
            if (s.currentInstructionIndex() != prevPc || s.cycles() != prevCycles) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private DebugSnapshot snapFromHistoryRecord(Object rec) {
        try {
            int pc = tryInvokeInt(rec,
                    "getInstructionIndex", "instructionIndex",
                    "getPcAfter", "pcAfter",
                    "getPc", "pc"
            );
            int cycles = tryInvokeInt(rec, "getCycles", "cycles");
            Map<String,String> vars = (Map<String,String>) tryInvoke(rec, Map.class,
                    "getVars", "vars", "getVariables", "variablesSnapshot"
            );
            if (vars == null || vars.isEmpty()) {
                try {
                    RunResult rr = (RunResult) tryInvoke(rec, RunResult.class,
                            "getRunResult", "result", "getResult"
                    );
                    if (rr != null) vars = toVarsMap(rr, this.inputsAtStart);
                } catch (Exception ignore) {}
            }
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

    private static Map<String, String> toVarsMap(emulator.api.dto.RunResult rr, Long[] inputs) {
        Map<Integer, Long> x = new java.util.TreeMap<>();
        Map<Integer, Long> z = new java.util.TreeMap<>();
        emulator.api.dto.VariableView yVar = null;

        java.util.List<emulator.api.dto.VariableView> vars =
                (rr == null || rr.vars() == null) ? java.util.List.of() : rr.vars();

        for (var v : vars) {
            if (v == null) continue;
            String name = v.name() == null ? "" : v.name().toLowerCase(java.util.Locale.ROOT);
            if (v.type() == emulator.api.dto.VarType.RESULT || "y".equals(name)) {
                yVar = v;
            } else if (v.type() == emulator.api.dto.VarType.INPUT || name.startsWith("x")) {
                x.put(v.number(), v.value());
            } else if (v.type() == emulator.api.dto.VarType.WORK || name.startsWith("z")) {
                z.put(v.number(), v.value());
            }
        }

        if (inputs != null) {
            for (int i = 0; i < inputs.length; i++) {
                x.putIfAbsent(i + 1, inputs[i]);
            }
        }

        var out = new java.util.LinkedHashMap<String,String>();
        long yVal = (yVar != null) ? yVar.value() : (rr != null ? rr.y() : 0L);
        out.put("y", String.valueOf(yVal));
        x.forEach((i,v) -> out.put("x"+i, String.valueOf(v)));
        z.forEach((i,v) -> out.put("z"+i, String.valueOf(v)));
        return out;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static int numSuffix(String s) {
        if (s == null) return Integer.MAX_VALUE;
        String t = s.trim().toLowerCase(Locale.ROOT);
        int i = 0; while (i < t.length() && !Character.isDigit(t.charAt(i))) i++;
        if (i >= t.length()) return Integer.MAX_VALUE;
        try { return Integer.parseInt(t.substring(i)); } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    private static Comparator<String> varOrder() {
        return (a,b) -> {
            String A = nz(a).toLowerCase(Locale.ROOT), B = nz(b).toLowerCase(Locale.ROOT);
            int ra = rank(A), rb = rank(B);
            if (ra != rb) return Integer.compare(ra, rb);
            if (ra == 1 || ra == 2) return Integer.compare(numSuffix(A), numSuffix(B));
            return A.compareTo(B);
        };
    }

    private static int rank(String s) {
        if ("y".equals(s)) return 0;
        if (s.startsWith("x")) return 1;
        if (s.startsWith("z")) return 2;
        return 3;
    }

    private List<String> discoverAllVarNames(ProgramView pv, Long[] inputs) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add("y");

        int xCount = (inputs != null) ? inputs.length : 0;
        for (int i = 1; i <= xCount; i++) names.add("x"+i);

        // scan z tokens from args
        if (pv != null && pv.instructions() != null) {
            var Z = java.util.regex.Pattern.compile("\\bz(?:[1-9]\\d*)?\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
            for (var iv : pv.instructions()) {
                if (iv.args() == null) continue;
                for (String a : iv.args()) {
                    if (a == null) continue;
                    var m = Z.matcher(a);
                    while (m.find()) names.add(m.group().toLowerCase(Locale.ROOT));
                }
            }
        }

        return names.stream().sorted(varOrder()).toList();
    }

    private Map<String,String> buildMaskedVars() {
        LinkedHashMap<String,String> masked = new LinkedHashMap<>();
        for (String name : orderedVarNames) {
            if (isInputVar(name)) {
                int idx = xIndex(name);
                String v = "-";
                if (idx != Integer.MAX_VALUE && inputsAtStart != null && idx >= 1 && idx <= inputsAtStart.length) {
                    Long val = inputsAtStart[idx - 1];
                    v = (val == null ? "-" : String.valueOf(val));
                }
                masked.put(name, v);
            } else {
                masked.put(name, "-");
            }
        }
        return masked;
    }

    private DebugSnapshot buildSnapshotWithState(int pc, Map<String,String> vars, int cycles, boolean fin) {
        Map<String,String> masked = buildMaskedVars();
        if (vars != null) {
            for (var e : vars.entrySet()) {
                String k = e.getKey(), v = e.getValue();
                if (k != null && v != null) masked.put(k, v);
            }
        }
        this.lastKnownVars.clear();
        this.lastKnownVars.putAll(masked);
        return new DebugSnapshot(pc, masked, cycles, fin);
    }

    private static boolean isInputVar(String name) {
        if (name == null) return false;
        String n = name.trim().toLowerCase(Locale.ROOT);
        if (!n.startsWith("x")) return false;
        try {
            int k = Integer.parseInt(n.substring(1));
            return k > 0;
        } catch (Exception ignore) {
            return false;
        }
    }

    private static int xIndex(String name) {
        if (!isInputVar(name)) return Integer.MAX_VALUE;
        return Integer.parseInt(name.substring(1));
    }

    private int inferLastPC(RunResult rr) { return 0; }
    private int inferCycles(RunResult rr) { return 0; }
    private Map<String,String> snapshotVarsFromRunResult(RunResult rr) { return Map.of(); }
}
