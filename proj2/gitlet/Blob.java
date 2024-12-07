package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Refs.*;
import static gitlet.Repository.*;
import static gitlet.Utils.*;

public class Blob implements Serializable {
    private String content;
    private File filePath;
    private String hashName;

    public Blob(String content, String hashName) {
        this.content = content;
        this.hashName = hashName;
        this.filePath = join(BLOBS_FOLDER, hashName);
    }


    public File getFilePath() {
        return filePath;
    }



    /**
     * save a Blob Object to the BLOB_FOLDER File
     */
    public void saveBlob() {
        if (!filePath.exists()) {
            writeContents(filePath, this.content);
        }

    }



    /**
     * get the content of the blob based on the blobName, where the blobName is a hash value
     *
     * @return Blob content
     */
    public static String getBlobContentFromName(String blobName) {
        String blobContent = null;
        File blobFile = join(BLOBS_FOLDER, blobName);
        if (blobFile.isFile() && blobFile.exists()) {
            blobContent = readContentsAsString(blobFile);
        }

        return blobContent;

    }

    /**
     * Overwrite the content of blob.content into the file
     */
    public static void overWriteFileWithBlob(File file, String content) {
        writeContents(file, content);
    }

}

