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

    public static boolean untrackFileExists(Commit commit) {
        List<String> workFileNames = plainFilenamesIn(CWD);
        Set<String> currTrackSet = commit.getBlobMap().keySet();

        for (String workFile : workFileNames) {
            if(!currTrackSet.contains(workFile)) {
                return true;
            }
        }
        return false;
    }

    /**
     *  overwrite blob. content to the file
     */
    public static void overWriteFileWithBlob(File file, String content) {
        writeContents(file, content);
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

    /**
     * java gitlet.Main commit [message]
     */

    public static void commitFile(String commitMsg) {
        //get addStageFiles and removeStageFiles as list
        List<String> addStageFiles = plainFilenamesIn(ADD_STAGE_DIR);
        List<String> removeStageFiles = plainFilenamesIn(REMOVE_STAGE_DIR);
        if(addStageFiles.isEmpty() && removeStageFiles.isEmpty()) {
            System.out.println("No changes added to the commit.");
            exit(0);
        }
        if (commitMsg == null || commitMsg.isEmpty()) {
            System.out.println("Please enter a commit message.");
            exit(0);
        }
        //get the newest commmit
        Commit prevCommit = getHeadCommit();

        //create new commit
        Commit commit = new Commit(prevCommit);
        commit.setDirectParent(prevCommit.getHashName());
        commit.setTimestamp(new Date(System.currentTimeMillis()));
        commit.setMessage(commitMsg);
        //        commit.setBranchName(prevCommit.getBranchName()); // 在log或者status中需要展示本次commit的分支

        for(String stageFileName : addStageFiles) {
            String hashName = readContentsAsString(join(ADD_STAGE_DIR, stageFileName));
            commit.addBlob(stageFileName, hashName);
            join(ADD_STAGE_DIR, stageFileName).delete();//delete addstage files
        }

        HashMap<String, String> blobMap = commit.getBlobMap();

        //delete commit blobMap corresponding value
        for (String stageFileName : removeStageFiles) {
            if (blobMap.containsKey(stageFileName)) {
                commit.removeBlob(stageFileName);
            }
            join(REMOVE_STAGE_DIR, stageFileName).delete();
        }

        commit.saveCommit();

        saveHEAD(getHeadBranchName(), commit.getHashName());
        saveBranch(getHeadBranchName(), commit.getHashName());
    }

    /**
     * java getlet.Main rm [file name]
     *
     * @param rmFileName
     */

    public static void removeStage(String rmFileName) {
        if (rmFileName == null || rmFileName.isEmpty()) {
            System.out.println("Please enter a file name.");
            exit(0);
        }

        //if addStage and commit don't have this file, exit
        Commit headcommit = getHeadCommit();
        HashMap<String, String> headCommitBlobMap = headcommit.getBlobMap();
        List<String> addStageFiles = plainFilenamesIn(ADD_STAGE_DIR);

        if(!headCommitBlobMap.containsKey(rmFileName)) {
            if (!addStageFiles.contains(rmFileName)) {
                System.out.println("No reason to remove the file.");
                exit(0);
            }
        }

        //if it exists
        File addStageFile = new File(ADD_STAGE_DIR, rmFileName);
        if (addStageFile.exists()) {
            addStageFile.delete();
        }

        //if it was tracked
        if (headCommitBlobMap.containsKey(rmFileName)) {
            File remoteFilePoint = new File(REMOVE_STAGE_DIR, rmFileName);
            writeContents(remoteFilePoint, "");

            File fileDeleted = new File(CWD, rmFileName);
            restrictedDelete(fileDeleted);
        }
    }

    /**
     * java gitlet.Main log
     */
    public static void printLog(){
        Commit headCommit = getHeadCommit();
        Commit commit = headCommit;

        while (!commit.getDirectParent().equals("")) {
            printCommitLog(commit);
            commit = getCommit(commit.getDirectParent());
        }
        //print the first commit
        printCommitLog(commit);
    }

    public static void printCommitLog(Commit commit) {
        System.out.println("===");
        System.out.println("commit " + commit.getHashName());
        System.out.println("Date: " + dateToTimeStamp(commit.getTimestamp()));
        System.out.println(commit.getMessage());
        System.out.print("\n");
    }

    /**
     * java gitlet.Main global-log
     *
     * don't care branch, just print the content of folder
     */
    public static void printGlobalLog() {
        List<String> commitFiles = plainFilenamesIn(COMMIT_FOLDER);
        for (String commitFileName : commitFiles) {
            Commit commit = getCommit(commitFileName);
            printCommitLog(commit);
        }
    }

    /**
     * java gitlet.Main find [findMsg]
     *
     * find commit exist or not in the commit folder
     *
     * @param findMsg
     */
    public static void findCommit(String findMsg) {
        boolean found = false;
        /* find in the commit folder */
        List<String> commitFiles = plainFilenamesIn(COMMIT_FOLDER);
        for (String commitFileName : commitFiles) {
            Commit commit1 = getCommit(commitFileName);
            if (commit1.getMessage().contains(findMsg)) {
                message(commit1.getHashName());
                found = true;
            }
        }

        if(!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    /**
     * java gitlet.Main status
     */
    public static void showStatus() {
        File gitletFile = join(CWD, ".gitlet");
        if(!gitletFile.exists()) {
            message("Not in an initialized Gitlet directory.");
            exit(0);
        }

        //get the current branch name
        Commit headCommit = getHeadCommit();
        String branchName = getHeadBranchName();

        List<String> filesInHead = plainFilenamesIn(HEAD_DIR);
        List<String> filesInAdd = plainFilenamesIn(ADD_STAGE_DIR);
        List<String> filesInRm = plainFilenamesIn(REMOVE_STAGE_DIR);
        HashMap<String, String> blobMap = headCommit.getBlobMap();
        Set<String> trackFileSet = blobMap.keySet();//file name tracked in the commmit
        LinkedList<String> modifiedFilesList = new LinkedList<>();
        LinkedList<String> deletedFilesList = new LinkedList<>();
        LinkedList<String> untrackFilesList = new LinkedList<>();

        printStatusPerField("Branches", filesInHead, branchName);
        printStatusPerField("Staged Files", filesInAdd, branchName);
        printStatusPerField("Removed Files", filesInRm, branchName);

        /* Modifications Not Staged For Commit */
        for (String fileAdd : filesInAdd) {
            //if file staged, but not in the working directory, put it into modifiedFilesList
            if(!join(CWD, fileAdd).exists()) {
                deletedFilesList.add(fileAdd);
                continue;
            }
            String workFileContent = readContentsAsString(join(CWD, fileAdd));
            String addStageBlobName = readContentsAsString(join(ADD_STAGE_DIR, fileAdd));
            String addStageFileContent = readContentsAsString(join(BLOBS_FOLDER, addStageBlobName));
            if (!workFileContent.equals(addStageFileContent)) {
                // when file content not identify on work area and addStage, put it into modifiedFilesList
                modifiedFilesList.add(fileAdd);
            }
        }

        //tracked in the current commit, changes in the working directory, but not staged
        for (String trackFile : trackFileSet) {
            if(trackFile.isEmpty() || trackFile == null) {
                continue;
            }
            File workFile = join(CWD, trackFile);
            File fileInRmStage = join(REMOVE_STAGE_DIR, trackFile);
            if (!workFile.exists()) { //workfile not exists in the working directory
                if(!fileInRmStage.exists()) {
                    deletedFilesList.add(trackFile); //it not exists in the working directory as well
                }
                continue;
            }
            if(!filesInAdd.contains(trackFile)) { //file not exist in the addStage
                String workFileContent = readContentsAsString(join(CWD, trackFile));
                String blobFileContent = readContentsAsString(join(BLOBS_FOLDER, blobMap.get(trackFile)));
                if (!workFileContent.equals(blobFileContent)) {
                    //When the file being tracked is modified, but there is no such file in addStage, enter the modifiedFilesList
                    modifiedFilesList.add(trackFile);
                }
            }
        }
        printStatusWithStatus("Modifications Not Staged For Commit", modifiedFilesList, deletedFilesList);

        /* Untracked Files */
        List<String> workFiles = plainFilenamesIn(CWD);
        for (String workFile : workFiles) {
            if (!filesInAdd.contains(workFile)
                    && !filesInRm.contains(workFile)
                    && !trackFileSet.contains(workFile)) {
                untrackFilesList.add(workFile);
                continue;
            }
            if (filesInRm.contains(workFile)) {
                untrackFilesList.add(workFile);
            }
        }
        printStatusPerField("Untracked Files", untrackFilesList, branchName);
    }

    /**
     *
     * @param field field name
     * @param files files in the folder
     * @param branchName
     */
    public static void printStatusPerField(String field, Collection<String> files,
                                           String branchName) {
        System.out.println("=== " + field + " ===");
        if(field.equals("Branches")) {
            for (var file : files) {
                //if it is a head file
                if(file.equals(branchName)) {
                    System.out.println("*" + file);
                } else {
                    System.out.println(file);
                }
            }
        } else {
            for (var file : files) {
                System.out.println(file);
            }
        }

        System.out.println("\n");
    }

    /**
     * print Modifications Not Staged For Commit
     *
     * @param field
     * @param modifiedFiles
     * @param deletedFiles
     */
    public static void printStatusWithStatus(String field, Collection<String> modifiedFiles,
                                             Collection<String> deletedFiles) {
        System.out.println("=== " + field + " ===");

        for (var file : modifiedFiles) {
            System.out.println(file + " " + "(modified)");
        }
        for (var file : deletedFiles) {
            System.out.println(file + " " + "(deleted)");
        }

        System.out.print("\n");
    }


    /**
     * java gitlet.Main checkout -- [file name]
     * java gitlet.Main checkout [commit id] -- [file name]
     * java gitlet.Main checkout [branch name]
     *
     * @param args
     */
    public static void checkOut(String[] args) {
        String fileName;
        if (args.length == 2) {
            //git checkout branchName
            checkoutBranch(args[1]);
        } else if(args.length == 4) {
            //git checkout [commit id] -- [file name]
            if(!args[2].equals("--")) {
                message("Incorrect operands.");
                exit(0);
            }
            // get the blob
            fileName = args[3];
            String commitId = args[1];
            Commit commit = getHeadCommit();

            if(getCommitFromId(commitId) == null) {
                System.out.println("No commit with that id exists.");
                exit(0);
            } else {
                commit = getCommitFromId(commitId);
            }

            if (!commit.getBlobMap().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
            }
            String blobName = commit.getBlobMap().get(fileName);
            String targetBlobContent = getBlobContentFromName(blobName);

            File fileInWorkDir = join(CWD, fileName);
            overWriteFileWithBlob(fileInWorkDir, targetBlobContent);
        } else if(args.length == 3) {
            //git checkout -- [file name]
            fileName = args[2];
            Commit headCommit = getHeadCommit();
            if(!headCommit.getBlobMap().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                exit(0);
            }
            String blobName = headCommit.getBlobMap().get(fileName);
            String targetBlobContent = getBlobContentFromName(blobName);

            File fileInWorkDir = join(CWD, fileName);
            overWriteFileWithBlob(fileInWorkDir, targetBlobContent);
        }
    }

    /** only for
     * java gitlet.Main checkout [branch name]
     */
    public static void checkoutBranch(String branchName) {
        Commit headCommit = getHeadCommit();

        if(branchName.equals(getHeadBranchName())) {
            System.out.println("No need to checkout the current branch.");
            exit(0);
        }
        //get head of branch name corresponding commit
        Commit branchHeadCommit = getBranchHeadCommit(branchName, "No such branch exists");
        HashMap<String, String> branchHeadBlobMap = branchHeadCommit.getBlobMap();
        Set<String> fileNameSet = branchHeadBlobMap.keySet();

        List<String> workFileNames = plainFilenamesIn(CWD);

        //detect track Files
        if (untrackFileExists(headCommit)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            exit(0);
        }

        //clean CWD
        for (String workFile : workFileNames) {
            restrictedDelete(join(CWD, workFile));
        }

        //rewrite to the CWD
        for (var trackedFileName : fileNameSet) {
            File workFile = join(CWD, trackedFileName);
            String blobHash = branchHeadBlobMap.get(trackedFileName);
            String blobFromNameContent = getBlobContentFromName(blobHash);
            writeContents(workFile, blobFromNameContent);
        }

        //save HEAD pointer
        saveHEAD(branchName, branchHeadCommit.getHashName());
    }
}
