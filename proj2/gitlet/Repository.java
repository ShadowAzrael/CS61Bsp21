package gitlet;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


import static gitlet.Commit.*;
import static gitlet.Refs.*;
import static gitlet.Utils.*;
import static gitlet.Blob.*;
import static java.lang.System.exit;


/** Represents a gitlet repository.
 *
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     *
     * .gitlet (folder)
     *  |--objects (folder) // storge commit objects file
     *      |--commits
     *      |--blobs
     *  |--refs (folder)
     *      |--heads (flolder) //point to the current branch
     *          |-- master (file)
     *          |-- other file //other branch path
     *      |--HEAD (file) //save Head point hash
     *  |-- addstage (folder) //staged area folder
     *  |-- removestage (folder)
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * To get Date obj a format to transform the object to String.
     *
     * @param date a Date obj
     * @return timestamp in standrad format
     */
    public static String dateToTimeStamp(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }

    /**
     *Check if the args of java gitlet.Main is empty.
     *
     * @param args
     */
    public static void checkArgsEmpty(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            exit(0);
        }
    }

    /**
     * initialize the folders
     */
    public static void setupPersistence() {
        GITLET_DIR.mkdirs();
        COMMIT_FOLDER.mkdirs();
        BLOBS_FOLDER.mkdirs();
        REFS_DIR.mkdirs();
        HEAD_DIR.mkdirs();
        ADD_STAGE_DIR.mkdirs();
        REMOVE_STAGE_DIR.mkdirs();
    }

    /* ---------------------- implementations --------------------- */
    /**
     * java gitlet.Main init
     */
    public static void initPersistence() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control "
                    + "system already exists in the current directory.");
            exit(0);
        }
        //create the folders in need
        setupPersistence();

        Date timestampInit = new Date(0);
        Commit initialCommmit = new Commit("initial commmit", timestampInit, "",null, null);
        initialCommmit.saveCommit();

        //save the hashName to heads dir
        String commitHashName = initialCommmit.getHashName();
        String branchName = "master";
        saveBranch(branchName, commitHashName);

        //HEAD point to commit heads dir
        saveHEAD("master", commitHashName);
    }

    /**
     * java gitlet.Main add [file name]
     */
    public static void addStage(String addFileName) {
        if (addFileName == null || addFileName.isEmpty()) {
            System.out.println("Please enter a file name.");
            exit(0);
        }

        File fileAdded = join(CWD, addFileName);

        if (!fileAdded.exists()) {
            System.out.println("File does not exist.");
            exit(0);
        }

        String fileAddedcontent = readContentsAsString(fileAdded);

        Commit headCommit = getHeadCommit();
        HashMap<String, String> headCommitBlobMap = headCommit.getBlobMap();

        //if this file already is tracked
        if (headCommitBlobMap.containsKey(addFileName)) {
            String fileAddedInHash = headCommit.getBlobMap().get(addFileName);
            String commmitContent = getBlobContentFromName(fileAddedInHash);

            //if staged content same with addFile's content ,do nothing
            //and if exists remove from staged, and remove from removal
            if (commmitContent.equals(fileAddedcontent)) {
                List<String> filesAdd = plainFilenamesIn(ADD_STAGE_DIR);
                List<String> filesRM = plainFilenamesIn(REMOVE_STAGE_DIR);

                if (filesAdd.contains(addFileName)) {
                    join(ADD_STAGE_DIR, addFileName).delete();
                }
                if(filesRM.contains(addFileName)) {
                    join(REMOVE_STAGE_DIR, addFileName).delete();
                }

                return;
            }
        }

        //put the file into the stage area
        String fileContent = readContentsAsString(fileAdded);
        String blobName = sha1(fileContent);

        Blob blobAdd = new Blob(fileContent, blobName);
        blobAdd.saveBlob();

        //write pointer to addStage,file name is addFileName, content is staging area saved path
        File blobPoint = join(ADD_STAGE_DIR, addFileName);
        writeContents(blobPoint, blobAdd.getFilePath().getName());
    }
}
