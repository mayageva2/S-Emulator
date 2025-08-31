package console;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class ConsoleIO {
    private final Scanner in;
    private final PrintStream out;

    public ConsoleIO(InputStream in, PrintStream out) {
        this.in = new Scanner(in);
        this.out = out;
    }

    //This func returns the line of text
    public String ask(String prompt) {
        out.print(prompt);
        return in.nextLine();
    }

    //These funcs print
    public void println(String s) {
        out.println(s);
    }
    public void print(String s) {
        out.print(s);
    }
}
