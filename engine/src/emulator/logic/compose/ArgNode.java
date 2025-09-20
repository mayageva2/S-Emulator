package emulator.logic.compose;

import java.util.List;

public interface ArgNode {
    final class Var implements ArgNode {
        public final String name;
        public Var(String name) { this.name = name; }
        public String name() { return name; }
    }
    final class Const implements ArgNode {
        public final long value;
        public Const(long value) { this.value = value; }
        public long value() { return value; }
    }
    final class Call implements ArgNode {
        public final String function;
        public final List<ArgNode> args;
        public Call(String function, List<ArgNode> args) {
            this.function = function;
            this.args = args;
        }
        public String function() { return function; }
        public java.util.List<ArgNode> args() { return args; }
    }
}
