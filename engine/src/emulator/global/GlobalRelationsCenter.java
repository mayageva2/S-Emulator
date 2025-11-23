package emulator.global;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalRelationsCenter {

    private static final Map<String, Set<String>> programToFunctions =
            new ConcurrentHashMap<>();

    private static final Map<String, Set<String>> functionToPrograms =
            new ConcurrentHashMap<>();

    private static final Map<String, Set<String>> relatedFunctions =
            new ConcurrentHashMap<>();

    private GlobalRelationsCenter() {}

    // Add function under program
    public static void addFunctionToProgram(String program, String function) {
        programToFunctions
                .computeIfAbsent(program, k -> ConcurrentHashMap.newKeySet())
                .add(function);

        functionToPrograms
                .computeIfAbsent(function, k -> ConcurrentHashMap.newKeySet())
                .add(program);
    }

    // Add “related functions” (functions from same program)
    public static void addRelatedFunction(String baseFunc, String relatedFunc) {
        if (baseFunc.equalsIgnoreCase(relatedFunc)) return;

        relatedFunctions
                .computeIfAbsent(baseFunc, k -> ConcurrentHashMap.newKeySet())
                .add(relatedFunc);

        relatedFunctions
                .computeIfAbsent(relatedFunc, k -> ConcurrentHashMap.newKeySet())
                .add(baseFunc);
    }

    // Get functions belonging to a program
    public static Set<String> getFunctionsByProgram(String program) {
        return programToFunctions.getOrDefault(program, Set.of());
    }

    // Get programs that use a function
    public static Set<String> getProgramsUsingFunction(String func) {
        return functionToPrograms.getOrDefault(func, Set.of());
    }

    // Get functions related to a function
    public static Set<String> getRelatedFunctions(String func) {
        return relatedFunctions.getOrDefault(func, Set.of());
    }

    public static void clear() {
        programToFunctions.clear();
        functionToPrograms.clear();
        relatedFunctions.clear();
    }
}
