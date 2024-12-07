package gitlet;

import java.io.File;

import static gitlet.Repository.*;
import static gitlet.Utils.*;

/**
 * Represent the reference point of HEAD, REMOTE and so on;
 */
public class Refs {
    /* The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /* The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /* the objects directory */
    static final File OBJECTS_FOLDER = join(GITLET_DIR, "objects");
    static final File COMMIT_FOLDER = join(OBJECTS_FOLDER, "commits");
    static final File BLOBS_FOLDER = join(OBJECTS_FOLDER, "blobs");

    /* The refs directory. */
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    public static final File HEAD_DIR = join(REFS_DIR, "heads");

    /* the current .gitlet/HEAD file */
    public static final File HEAD_POINT = join(REFS_DIR, "HEAD");

    /* the stage directory */
    public static final File ADD_STAGE_DIR = join(GITLET_DIR, "addstage");
    public static final File REMOVE_STAGE_DIR = join(GITLET_DIR, "removestage");


    /**
     * Create a file: the path is join(HEAD_DIR, branchName)
     * write in the file with hashName
     *
     * @param branchName
     * @param hashName:   write the content of the branch
     */
    public static void saveBranch(String branchName, String hashName) {
        // save the file of the head of a given branch
        File branchHead = join(HEAD_DIR, branchName);
        writeContents(branchHead, hashName);

    }



    /**
     * Write a hash value of the current branch in the HEAD file,
     * Save the point to HEAD into .gitlet/refs/HEAD folder
     *
     * @param branchHeadCommitHash HashName of the commit, which is the content written to HEAD
     */
    public static void saveHEAD(String branchName, String branchHeadCommitHash) {
        writeContents(HEAD_POINT, branchName + ":" + branchHeadCommitHash);
    }

    /**
     * get the name of the current branch directly from the HEAD file
     *
     * @return
     */
    public static String getHeadBranchName() {
        String headContent = readContentsAsString(HEAD_POINT);
        String[] splitContent = headContent.split(":");
        String branchName = splitContent[0];
        return branchName;
    }


}
