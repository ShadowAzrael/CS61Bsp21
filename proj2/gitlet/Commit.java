package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Refs.*;
import static gitlet.Repository.*;
import static gitlet.Utils.*;
import static java.lang.System.exit;

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
    private String message;
    private String directParent;
    private String otherParent;
    private Date timestamp;
    private HashMap<String, String> blobMap = new HashMap<>();
    /** The message of this Commit. */

    public Commit(String message, Date timestamp, String directparent,
                  String blobFileName, String blobHashName) {
        this.message = message;
        this.directParent = directParent;
        this.otherParent = otherParent;
        this.timestamp = timestamp;
        if (blobFileName == null || blobHashName.isEmpty()) {
            this.blobMap = new HashMap<>();
        }
        else {
            this.blobMap.put(blobFileName, blobHashName);
        }
    }

    public Commit(Commit directparent) {
        this.message = directparent.message;
        this.timestamp = directparent.timestamp;
        this.directParent = directparent.directParent;
        this.blobMap = directparent.blobMap;
    }

    /**
     * To save commit into files in COMMIT_FOLDER, persists the status of object.
     */
    public void saveCommit() {
        // get the uid of this
        String hashname = this.getHashName();

        // write obj to files
        File commitFile = new File(COMMIT_FOLDER, hashname);
        writeObject(commitFile, this);
    }

    //commit hashName
    public String getHashName() {
        return sha1(this.message, dateToTimeStamp(this.timestamp), this.directParent);
    }

    public void addBlob(String blobFileName, String blobHashName) {
        this.blobMap.put(blobFileName, blobHashName);
    }

    public void removeBlob(String blobFileName) {
        this.blobMap.remove(blobFileName);
    }
    public void setDirectParent(String directParent) {
        this.directParent = directParent;
    }

    public String getDirectParent() {
        return directParent;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }


    public HashMap<String, String> getBlobMap() {
        return blobMap;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public String getOtherParent() {
        return otherParent;
    }

    public void setOtherParent(String otherParent) {
        this.otherParent = otherParent;
    }

    /* ======================== functions ======================*/
    public static Commit getHeadCommit() {
        String headContent = readContentsAsString(HEAD_POINT);
        String headHashName = headContent.split(":")[1];
        File commitFile = join(COMMIT_FOLDER, headHashName);
        Commit headCommit = readObject(commitFile, Commit.class);
        return headCommit;
    }

}
