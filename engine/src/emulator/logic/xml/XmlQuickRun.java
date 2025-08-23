package emulator.logic.xml;

import java.net.URISyntaxException;
import java.nio.file.Path;

public class XmlQuickRun {
    public static void main(String[] args) {
        try {
            var url = XmlQuickRun.class.getClassLoader().getResource("tst.txt");
            if (url == null) {
                System.out.println("Error: sample-program.xml not found in resources!");
                return;
            }
            Path file = Path.of(url.toURI());

            XmlProgramReader reader = new XmlProgramReader();
            ProgramXml program = reader.readAndSanitize(file);

            XmlProgramValidator validator = new XmlProgramValidator();
            validator.validateBasic(program);
            validator.validateLabelsExist(program);

            System.out.println("[OK] Program loaded successfully: " + program.getName());

        } catch (XmlReadException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
