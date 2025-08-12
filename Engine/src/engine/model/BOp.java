package engine.model;

public enum BOp {
    NEUTRAL,        // v <- v
    INCREASE,       // v <- v + 1
    DECREASE,       // v <- v - 1
    JUMP_NOT_ZERO   // IF v != 0 GO TO Lk
}
