package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.MyUtils.getObjectFile;
import static gitlet.MyUtils.saveObjectFile;
import static gitlet.Utils.readObject;
import static gitlet.Utils.sha1;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    private Date date;//Created Date
    private String parents;//parents commits SHA1 id
    /** The snaphots of files of this commmit.
    <The keys are files in CWD  with abosultepath,
     The values are blobs in BLOB_DIR/shortCommitId */
    private HashMap<String,String> blobs;//
    private String id;//SHA1 id of this Commit

    /* TODO: fill in the rest of this class. */
    public Commit(String message, String parents) {
        initialCommmit(message, parents);
    }

    private void initialCommmit(String message, String parents) {
        this.message = message;
        this.parents = parents;
        if (parents == null) {
            this.date = new Date(0);
        } else {
            this.date = new Date();
        }
        this.blobs = new HashMap<>();
    }

    private static String dateToTimeStamp(Date date) {
        // Thu Jan 1 00:00:00 1970 +0000
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy z", Locale.US);
        return dateFormat.format(date);
    }


    public String getMessage() {
        return message;
    }

    public String getParent() {
        return parents;
    }

    public HashMap<String, String> getBlobs() {
        return blobs;
    }

    private String generateID() {
        return sha1(this.date, message, parents, blobs.toString());
    }

    /**
     * Save this Commit instance to file in objects folder.
     */
    public void save() {
        saveObjectFile(, this);
    }

    public static void main(String[] args) {
        File file = new File("");
        Utils.writeObject(file, Commit.class);
    }

    public String getId() {
        return id;
    }
}
