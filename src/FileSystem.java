import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystem {

    public static String loadFile(String filepath) {
        try {
            return Files.readString(Path.of(filepath));
        } catch (IOException e) {
            error(String.valueOf(e));
        }
        return null;
    }

    public static void saveFile(Path path, String data) {
        try {
            if (Files.notExists(path.getParent())) Files.createDirectories(path.getParent());
            Files.writeString(path, data);
        } catch (IOException e) {
            error(String.valueOf(e));
        }
    }

    private static void error(String message) {
        System.out.printf("[FILESYSTEM_ERROR!]: %s%n", message);
        System.exit(1);
    }
}
