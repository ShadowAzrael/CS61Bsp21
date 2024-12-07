package gitlet;


import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Refs.*;
import static gitlet.Repository.*;
import static gitlet.Utils.*;
import static java.lang.System.exit;

/**
 * Represents a gitlet commit object.
 *  does at a high level.
 *
 * @author Li
 */
public class Commit implements Serializable {
    /**
     * <p>
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /* The message of this Commit. */
    private String message;
    /* The parent of commit, null if it's the first commit */
    private String directParent;
    private String otherParent;
    /* the timestamp of commit*/
    private Date timestamp;
    /* the contents of commit files*/
    private HashMap<String, String> blobMap = new HashMap<>();


    public Commit(String message, Date timestamp, String directparent,
                  String blobFileName, String blobHashName) {
        this.message = message;
        this.timestamp = timestamp;
        this.directParent = directparent;
        if (blobFileName == null || blobFileName.isEmpty()) {
            this.blobMap = new HashMap<>();
        } else {
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


    /**
     * @param blobName blob hashname
     */
    public void addBlob(String fileName, String blobName) {
        this.blobMap.put(fileName, blobName);
    }

    public void removeBlob(String fileName) {
        this.blobMap.remove(fileName);
    }


    public String getHashName() {
        return sha1(this.message, dateToTimeStamp(this.timestamp), this.directParent);
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
    /* ======================== the above is getter and setter ======================*/

    /**
     * get HEAD pointer Commit Instance
     *
     * @return
     */
    public static Commit getHeadCommit() {
        /* get HEAD pointer,to the latest commit */
        String headContent = readContentsAsString(HEAD_POINT);
        String headHashName = headContent.split(":")[1];
        File commitFile = join(COMMIT_FOLDER, headHashName);

        Commit commit = readObject(commitFile, Commit.class);

        return commit;
    }

    /**
     * get branches folder branch commit object
     *
     * @return
     */
    public static Commit getBranchHeadCommit(String branchName, String errorMsg) {


        File brancheFile = join(HEAD_DIR, branchName);
        if (!brancheFile.exists()) {
            System.out.println(errorMsg);
            exit(0);
        }

        /* get The HEAD Pointer,Points To The Latest Commit */
        String headHashName = readContentsAsString(brancheFile);


        File commitFile = join(COMMIT_FOLDER, headHashName);
        /* get The Commit File */
        Commit commit = readObject(commitFile, Commit.class);

        return commit;

    }

    /**
     * use the hashname to Get the commit object
     *
     * @param hashName
     * @return
     */
    public static Commit getCommit(String hashName) {
        List<String> commitFiles = plainFilenamesIn(COMMIT_FOLDER);
        if (!commitFiles.contains(hashName)) {
            return null;
        }
        File commitFile = join(COMMIT_FOLDER, hashName);
        Commit commit = readObject(commitFile, Commit.class);
        return commit;
    }


    /**
     * Given a commitId, a corresponding commit object is returned
     *
     * @param commitId
     * @return commit or null
     */
    public static Commit getCommitFromId(String commitId) {
        Commit commit = null;

        /*  finding in commit folder */
        String resCommitId = null;
        List<String> commitFileNames = plainFilenamesIn(COMMIT_FOLDER);
        /* for prefix */
        for (String commitFileName : commitFileNames) {
            if (commitFileName.startsWith(commitId)) {
                resCommitId = commitFileName;
                break;
            }
        }

        if (resCommitId == null) {
            return null;
        } else {
            File commitFile = join(COMMIT_FOLDER, resCommitId);
            commit = readObject(commitFile, Commit.class);
        }

        return commit;
    }

    /**
     * Get the common node of both branches, search only from directParents
     *
     * @param commitA
     * @param commitB
     * @return
     */
    public static Commit getSplitCommit(Commit commitA, Commit commitB) {

        Commit p1 = commitA, p2 = commitB;

        Deque<Commit> dequecommitA = new ArrayDeque<>();
        Deque<Commit> dequecommitB = new ArrayDeque<>();
        /* for Save Visited Nodes */
        HashSet<String> visitedInCommitA = new HashSet<>();
        HashSet<String> visitedInCommitB = new HashSet<>();

        dequecommitA.add(p1);
        dequecommitB.add(p2);

        while (!dequecommitA.isEmpty() || !dequecommitB.isEmpty()) {
            if (!dequecommitA.isEmpty()) {
                /* commitA have Traversable Objects In The Queue */
                Commit currA = dequecommitA.poll();
                if (visitedInCommitB.contains(currA.getHashName())) {
                    return currA;
                }
                visitedInCommitA.add(currA.getHashName());
                addParentsToDeque(currA, dequecommitA);
            }

            if (!dequecommitB.isEmpty()) {
                Commit currB = dequecommitB.poll();
                if (visitedInCommitA.contains(currB.getHashName())) {
                    return currB;
                }
                visitedInCommitB.add(currB.getHashName());
                addParentsToDeque(currB, dequecommitB);
            }
        }

        return null;

    }

    /**
     * put the parent node of this node (or two nodes) into Queue
     *
     * @param commit
     * @param dequeCommit
     */
    private static void addParentsToDeque(Commit commit, Queue<Commit> dequeCommit) {
        if (!commit.getDirectParent().isEmpty()) {
            dequeCommit.add(getCommitFromId(commit.getDirectParent()));
        }

        if (commit.getOtherParent() != null) {
            dequeCommit.add(getCommitFromId(commit.getOtherParent()));
        }
    }

}
