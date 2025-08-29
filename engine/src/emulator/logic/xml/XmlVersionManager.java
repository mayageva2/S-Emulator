package emulator.logic.xml;

import java.io.IOException;
import java.nio.file.*;

public class XmlVersionManager {
    private final Path versionsDir;

    public XmlVersionManager() {
        this(Paths.get("versions"));
    }

    public XmlVersionManager(Path versionsDir) {
        this.versionsDir = versionsDir;
    }

    //This func saves a given XML program file into the versionsDir
    public Path saveOrReplaceVersion(Path original, int version) throws IOException {
        Files.createDirectories(versionsDir);
        Path dest = versionsDir.resolve("program-v" + version + ".xml");
        Files.copy(original, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }
}
