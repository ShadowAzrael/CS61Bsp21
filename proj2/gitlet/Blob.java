package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Utils.*;
import static gitlet.MyUtils.*;

public class Blob implements Serializable {

    /**
     * The content of the file blob points to.
     */
    private final String content;

    /**
     * The SHA1 id generated from the blob object.
     */
    private final String id;


    public Blob(File sourceFile) {
        this.content = readContentsAsString(sourceFile);
        this.id = generateId(sourceFile);
    }

    /**
     * Generate SH1 id.
     *
     * @param sourceFile File instance
     * @return SHA1 id
     */
    public static String generateId(File sourceFile) {
        return sha1(sourceFile.getName(), readContentsAsString(sourceFile));
    }


    /**
     * Get the SHA1 id generated from the source file content.
     *
     * @return SHA1 id
     */
    public String getId() {
        return id;
    }

    /**
     * Get the Blob file.
     *
     * @return File instance
     */
    public String getConetent() {
        return content;
    }

    public String makeBlob() {
        return "0";
    }
}