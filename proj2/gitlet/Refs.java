package gitlet;

import javax.swing.text.StringContent;
import java.io.File;

import static gitlet.Repository.*;
import static gitlet.Utils.*;

//represent the reference point of HEAD, REMOTE and so on
public class Refs {
    //current working directory
    public static final File CWD = new File(System.getProperty("user.dir"));
    //.gitlet directory.
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    //objects directory
    static final File OBJECTS_FOLDER = join(GITLET_DIR, "objects");
    static final File COMMIT_FOLDER = join(OBJECTS_FOLDER, "commits");
    static final File BLOBS_FOLDER = join(OBJECTS_FOLDER, "blobs");

    //refs directory
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    public static final File HEAD_DIR = join(REFS_DIR, "heads");

    //current .gitlet/HEAD file
    public static final File HEAD_POINT = join(REFS_DIR, "HEAD");

    //the stage directory
    public static final File ADD_STAGE_DIR = join(GITLET_DIR, "addstage");
    public static final File REMOVE_STAGE_DIR = join(GITLET_DIR, "removestage");

    /*
     * create a file: path is join(HEAD_DIR, branchName)
     * write hashName on it
     *
     * @param branchName
     * @param hashName:branch content
     */
    public static void saveBranch(String branchName, String hashName) {
        //save the file of the head of a given branch
        File branchHead = join(HEAD_DIR, branchName);
        writeContents(branchHead, hashName);
    }

    /*
     *save the point to HEAD into .gitlet/refs/HEAD folder with current branch hash
     *
     * @param branchHeadCommitHash
     */
    public static void saveHEAD(String branchName, String branchHeadCommitHash) {
        writeContents(HEAD_POINT, branchName + ":" + branchHeadCommitHash);
    }

    //getHeadBranchName
    public static String getHeadBranchName() {
        String headContent = readContentsAsString(HEAD_POINT);
        String branchName = headContent.split(":")[0];
        return branchName;
    }
}
