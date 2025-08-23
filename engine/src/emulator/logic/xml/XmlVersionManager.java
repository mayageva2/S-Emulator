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

    public void saveOrReplaceVersion(Path original, int version) {
        try {
            Files.createDirectories(versionsDir);
            Path dest = versionsDir.resolve("program-v" + version + ".xml");
            Files.copy(original, dest, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[INFO] Saved version " + version + " to " + dest);
        } catch (IOException e) {
            System.err.println("[WARN] Could not save version: " + e.getMessage());
        }
    }
}
