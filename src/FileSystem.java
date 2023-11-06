import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystem {

    public static String loadFile(String fileName) {
        try {
            return Files.readString(Path.of(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveFile(Path path, String data) {
        try {
            Files.writeString(path, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
