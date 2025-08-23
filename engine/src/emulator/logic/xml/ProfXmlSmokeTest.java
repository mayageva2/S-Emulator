package emulator.logic.xml;

import emulator.logic.instruction.Instruction;
import emulator.logic.program.Program;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ProfXmlSmokeTest {

    public static void main(String[] args) {
        try {
            // 1) resolve XML path: CLI arg, else classpath resource "minus.xml"
            Path xmlPath;
            if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
                xmlPath = Paths.get(args[0].trim());
                System.out.println("Using file: " + xmlPath.toAbsolutePath());
            } else {
                URL u = ProfXmlSmokeTest.class.getClassLoader().getResource("minus.xml");
                if (u == null) {
                    System.err.println("[ERROR] Provide a path argument or put 'minus.xml' on the classpath.");
                    return;
                }
                xmlPath = Path.of(u.toURI());
                System.out.println("Using resource: " + xmlPath);
            }

            // 2) read & sanitize via XmlResult
            XmlProgramReader reader = new XmlProgramReader();
            XmlResult<ProgramXml> result = reader.tryRead(xmlPath);

            if (!result.isSuccess()) {
                System.err.println("[ERROR] " + result.getError());
                return;
            }

            ProgramXml programXml = result.getValue();
            // 3) validate (only throws if invalid)
            XmlProgramValidator validator = new XmlProgramValidator();
            // if you have validateAll(...) use that; otherwise keep the two calls:
            // validator.validateAll(programXml);
            try {
                validator.validate(programXml);
            } catch (XmlReadException ve) {
                System.err.println("[ERROR] Validation failed: " + ve.getMessage());
                return;
            }

            // 4) only now (read OK + validations pass) -> save version 1
            XmlVersionManager versionManager = new XmlVersionManager();
            versionManager.saveVersion(xmlPath, 1);

            // 5) map to engine objects
            Program program = XmlToObjects.toProgram(programXml);

            // 6) summary
            List<Instruction> instructions = program.getInstructions();
            System.out.println("\n[OK] XML mapped to engine objects successfully.");
            System.out.println("Program name : " + programXml.getName());
            System.out.println("Instr count  : " + instructions.size());

            int show = Math.min(10, instructions.size());
            System.out.println("\nFirst " + show + " instructions (class names):");
            for (int i = 0; i < show; i++) {
                Instruction ins = instructions.get(i);
                System.out.printf("  [%02d] %s%n", i, ins.getClass().getSimpleName());
            }

        } catch (XmlReadException e) {
            System.err.println("[ERROR] " + e.getMessage()); // engine/business validation error
        } catch (Exception e) {
            System.err.println("[ERROR] Unexpected: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
