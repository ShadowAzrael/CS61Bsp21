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


/**
 * Represents a gitlet repository.
 * does at a high level.
 *
 * @author ethanyi
 */
public class Repository {
    /**
     * <p>
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     * <p>
     * <p>
     * the path we created as below:
     * <p>
     * .gitlet (folder)
//     *      |── objects (folder) // save The Commit Object File
     *          |-- commits
     *          |-- blobs
     *      |── refs (folder)
     *          |── heads (folder) //point To The HEAD Branch
     *              |-- master (file)
     *              |-- other file      //paths Other Branches
     *          |-- HEAD (file)     // save The Correspondent hashName Of The HEAD Pointer
     *      |-- addstage (folder)       // stagingArea
     *      |-- removestage (folder)
     */


    /**
     * initialize the folder instructure
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

    /**
     * Check if the ARGS of java gitlet.Main is empty
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
     * To get Date obj a format to transform the object to String.
     *
     * @param date a Date obj
     * @return timestamp in standrad format
     */
    public static String dateToTimeStamp(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }


    public static void printCommitLog(Commit commit) {
        System.out.println("===");
        System.out.println("commit " + commit.getHashName());
        System.out.println("Date: " + dateToTimeStamp(commit.getTimestamp()));
        System.out.println(commit.getMessage());
        System.out.print("\n");
    }

    /**
     * @param field      the Title Area Of The Print
     * @param files
     * @param branchName
     */
    public static void printStatusPerField(String field, Collection<String> files,
                                           String branchName) {
        System.out.println("=== " + field + " ===");
        if (field.equals("Branches")) {
            for (var file : files) {
                // is it is HEAD
                if (file.equals(branchName)) {
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

        System.out.print("\n");
    }


    /**
     * Modifications Not Staged For Commit print out
     *
     * @param field         the Title Area Of The Print
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


    public static boolean untrackFileExists(Commit commit) {
        List<String> workFileNames = plainFilenamesIn(CWD);
        Set<String> currTrackSet = commit.getBlobMap().keySet();
        /* check whether exist not tracked by current branch in CWD*/
        for (String workFile : workFileNames) {
            if (!currTrackSet.contains(workFile)) {
                return true;
            }
        }
        return false;
    }


    /**
     * files of headCommit and otherHeadCommit both modified
     *
     * @param headCommit
     * @param otherHeadCommit
     */
    public static void processConflict(Commit headCommit, Commit otherHeadCommit,
                                       String splitTrackName) {
        String otherBlobFile = "";
        String otherBlobContent = "";

        String headBlobFile = "";
        String headBlobContent = "";

        HashMap<String, String> headCommitBolbMap = headCommit.getBlobMap();
        HashMap<String, String> otherHeadCommitBolbMap = otherHeadCommit.getBlobMap();


        message("Encountered a merge conflict.");

        if (otherHeadCommitBolbMap.containsKey(splitTrackName)) {
            otherBlobFile = otherHeadCommitBolbMap.get(splitTrackName);
            otherBlobContent = getBlobContentFromName(otherBlobFile);
        }

        if (headCommitBolbMap.containsKey(splitTrackName)) {
            headBlobFile = headCommitBolbMap.get(splitTrackName);
            headBlobContent = getBlobContentFromName(headBlobFile);
        }

        /* modify The Contents Of The Work File*/
        StringBuilder resContent = new StringBuilder();
        resContent.append("<<<<<<< HEAD\n");
        resContent.append(headBlobContent);
        resContent.append("=======" + "\n");
        resContent.append(otherBlobContent);
        resContent.append(">>>>>>>" + "\n");

        String resContentString = resContent.toString();
        writeContents(join(CWD, splitTrackName), resContentString);
        addStage(splitTrackName);
    }

    /* ---------------------- FunctionImplementation --------------------- */

    /**
     * java gitlet.Main init
     */
    public static void initPersistence() {
        // if .gitlet dir existed
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control "
                    + "system already exists in the current directory.");
            exit(0);
        }
        // create the folders in need
        setupPersistence();
        // create timestamp,Commit and save commit into files
        Date timestampInit = new Date(0);
        Commit initialCommit = new Commit("initial commit", timestampInit,
                "", null, null);
        initialCommit.saveCommit();

        // save the hashname to heads dir
        String commitHashName = initialCommit.getHashName();
        String branchName = "master";
        saveBranch(branchName, commitHashName);

        // Point the HEAD pointer to the file in the commit that represents the head
        saveHEAD("master", commitHashName);

    }

    /**
     * java gitlet.Main add [file name]
     *
     * @param addFileName
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
        String fileAddedContent = readContentsAsString(fileAdded);

        Commit headCommit = getHeadCommit();
        HashMap<String, String> headCommitBlobMap = headCommit.getBlobMap();

        /* ifThisFileHasAlreadyBeenTracked */
        if (headCommitBlobMap.containsKey(addFileName)) {
            String fileAddedInHash = headCommit.getBlobMap().get(addFileName);
            String commitContent = getBlobContentFromName(fileAddedInHash);

            /* If the staged content is the same as the content you want to add,
            it is not included in the staging area,
            Also remove it from the staging area (if it exists) and remove it from the renewal area */
            if (commitContent.equals(fileAddedContent)) {
                List<String> filesAdd = plainFilenamesIn(ADD_STAGE_DIR);
                List<String> filesRm = plainFilenamesIn(REMOVE_STAGE_DIR);
                /* If it exists in the staging area, it is deleted from the staging area */
                if (filesAdd.contains(addFileName)) {
                    join(ADD_STAGE_DIR, addFileName).delete();
                }
                /* if The Removal Area Exists Remove It*/
                if (filesRm.contains(addFileName)) {
                    join(REMOVE_STAGE_DIR, addFileName).delete();
                }

                return;
            }

        }

        /* Put the file into the staging area,
        the blob file name is the hash value of the content,
        and the content is the source file content */
        String fileContent = readContentsAsString(fileAdded);
        String blobName = sha1(fileContent);

        Blob blobAdd = new Blob(fileContent, blobName); // 使用blob进行对象化管理
        blobAdd.saveBlob();

        /* Write logic is executed regardless of whether it exists or not */
        /* write pointer to addStage,filename is addFileName,
        the content is the path where the staging area is saved */
        File blobPoint = join(ADD_STAGE_DIR, addFileName);
        writeContents(blobPoint, blobAdd.getFilePath().getName());
    }

    /**
     * java gitlet.Main commit [message]
     */
    public static void commitFile(String commitMsg) {
        /* get filename and hashName in staging area */
        List<String> addStageFiles = plainFilenamesIn(ADD_STAGE_DIR);
        List<String> removeStageFiles = plainFilenamesIn(REMOVE_STAGE_DIR);

        if (addStageFiles.isEmpty() && removeStageFiles.isEmpty()) {
            System.out.println("No changes added to the commit.");
            exit(0);
        }
        if (commitMsg == null || commitMsg.isEmpty()) {
            System.out.println("Please enter a commit message.");
            exit(0);
        }


        /* get latest commit*/
        Commit oldCommit = getHeadCommit();

        /* create new commit, adjust newCommit according oldCommit*/
        Commit newCommit = new Commit(oldCommit);
        newCommit.setDirectParent(oldCommit.getHashName());  // setTheParentNode
        newCommit.setTimestamp(new Date(System.currentTimeMillis()));
        newCommit.setMessage(commitMsg);
//        newCommit.setBranchName(oldCommit.getBranchName()); // In the log or status, you need to display the branch of the commit


        /* Read the path of the fileName in each addstage and save it to the blobMap of the commit */
        for (String stageFileName : addStageFiles) {
            String hashName = readContentsAsString(join(ADD_STAGE_DIR, stageFileName));
            newCommit.addBlob(stageFileName, hashName);
            join(ADD_STAGE_DIR, stageFileName).delete();
        }

        HashMap<String, String> blobMap = newCommit.getBlobMap();

        /* Read the path of the fileName in each rmstage, and delete the corresponding value in the blobMap of the commit */
        for (String stageFileName : removeStageFiles) {
            if (blobMap.containsKey(stageFileName)) {
                newCommit.removeBlob(stageFileName);
            }
            join(REMOVE_STAGE_DIR, stageFileName).delete();
        }

        newCommit.saveCommit();

        /* updated HEADpointer and current branch head pointer */
        saveHEAD(getHeadBranchName(), newCommit.getHashName());
        saveBranch(getHeadBranchName(), newCommit.getHashName());

    }

    /**
     * For automatic commit of the merge
     *
     * @param commitMsg
     * @param branchName
     */
    public static void commitFileForMerge(String commitMsg, String branchName) {

        List<String> addStageFiles = plainFilenamesIn(ADD_STAGE_DIR);
        List<String> removeStageFiles = plainFilenamesIn(REMOVE_STAGE_DIR);

        if (addStageFiles.isEmpty() && removeStageFiles.isEmpty()) {
            System.out.println("No changes added to the commit.");
            exit(0);
        }

        if (commitMsg == null) {
            System.out.println("Please enter a commit message.");
            exit(0);
        }

        Commit oldCommit = getHeadCommit();
        Commit branchHeadCommit = getBranchHeadCommit(branchName, null);

        Commit newCommit = new Commit(oldCommit);
        newCommit.setDirectParent(oldCommit.getHashName());
        newCommit.setTimestamp(new Date(System.currentTimeMillis()));
        newCommit.setMessage(commitMsg);
        newCommit.setOtherParent(branchHeadCommit.getHashName());

        for (String stageFileName : addStageFiles) {
            String hashName = readContentsAsString(join(ADD_STAGE_DIR, stageFileName));
            newCommit.addBlob(stageFileName, hashName);
            join(ADD_STAGE_DIR, stageFileName).delete();
        }

        HashMap<String, String> blobMap = newCommit.getBlobMap();

        for (String stageFileName : removeStageFiles) {
            if (blobMap.containsKey(stageFileName)) {
                join(BLOBS_FOLDER, blobMap.get(stageFileName)).delete();
                newCommit.removeBlob(stageFileName);
            }
            join(REMOVE_STAGE_DIR, stageFileName).delete();
        }

        newCommit.saveCommit();

        saveHEAD(getHeadBranchName(), newCommit.getHashName());
        saveBranch(getHeadBranchName(), newCommit.getHashName());
    }

    /**
     * java gitlet.Main rm [file name]
     *
     * @param removeFileName
     */
    public static void removeStage(String removeFileName) {
        /* If the file name is empty, or if the CWD does not have the file */
        if (removeFileName == null || removeFileName.isEmpty()) {
            System.out.println("Please enter a file name.");
            exit(0);
        }

        /* If this file does not exist in the staging area, it does not exist in the commit */
        Commit headCommit = getHeadCommit();
        HashMap<String, String> blobMap = headCommit.getBlobMap();
        List<String> addStageFiles = plainFilenamesIn(ADD_STAGE_DIR);

        if (!blobMap.containsKey(removeFileName)) {
            if (!addStageFiles.contains(removeFileName)) {
                System.out.println("No reason to remove the file.");
                exit(0);
            }

        }

        /* if It Exists In Staging Area Delete It */
        File addStageFile = join(ADD_STAGE_DIR, removeFileName);
        if (addStageFile.exists()) {
            addStageFile.delete();
        }


        /* When this file is being tracked */
        if (blobMap.containsKey(removeFileName)) {
            /* add to removeStage */
            File remoteFilePoint = new File(REMOVE_STAGE_DIR, removeFileName);
            writeContents(remoteFilePoint, "");

            /* Delete files in the working directory, and only delete the files when they are tracked */
            File fileDeleted = new File(CWD, removeFileName);
            restrictedDelete(fileDeleted);
        }

    }

    /**
     * java gitlet.Main log
     */
    public static void printLog() {

        Commit headCommit = getHeadCommit();
        Commit commit = headCommit;

        while (!commit.getDirectParent().equals("")) {
            printCommitLog(commit);
            commit = getCommit(commit.getDirectParent());
        }
        /* print the first commit*/
        printCommitLog(commit);
    }


    /**
     * java gitlet.Main global-log
     *
     * @apiNote This is not focusing on the branch, just printing out the contents of the folder
     */
    public static void printGlobalLog() {

        List<String> commitFiles = plainFilenamesIn(COMMIT_FOLDER);
        for (String commitFileName : commitFiles) {
            Commit commit = getCommit(commitFileName);
            printCommitLog(commit);
        }
    }

    /**
     * java gitlet.Main find [commit message]
     */
    public static void findCommit(String commitMsg) {
        boolean found = false;

        List<String> commitFiles = plainFilenamesIn(COMMIT_FOLDER);
        for (String commitFile : commitFiles) {
            Commit commit1 = getCommit(commitFile);
            if (commit1.getMessage().equals(commitMsg)) {
                message(commit1.getHashName());
                found = true;
            }
        }

        if (!found) {
            System.out.println("Found no commit with that message.");
        }

    }


    /**
     * java gitlet.Main status
     */
    public static void showStatus() {
        File gitletFile = join(CWD, ".gitlet");
        if (!gitletFile.exists()) {
            message("Not in an initialized Gitlet directory.");
            exit(0);
        }

        Commit headCommit = getHeadCommit();
        String branchName = getHeadBranchName();

        List<String> filesInHead = plainFilenamesIn(HEAD_DIR);
        List<String> filesInAdd = plainFilenamesIn(ADD_STAGE_DIR);
        List<String> filesInRm = plainFilenamesIn(REMOVE_STAGE_DIR);
        HashMap<String, String> blobMap = headCommit.getBlobMap();
        Set<String> trackFileSet = blobMap.keySet();  // file was tracked in commit, its files name set
        LinkedList<String> modifiedFilesList = new LinkedList<>();
        LinkedList<String> deletedFilesList = new LinkedList<>();
        LinkedList<String> untrackFilesList = new LinkedList<>();

        printStatusPerField("Branches", filesInHead, branchName);
        printStatusPerField("Staged Files", filesInAdd, branchName);
        printStatusPerField("Removed Files", filesInRm, branchName);

        /* Modifications Not Staged For Commit */
        /* being added in Staging area, but the content is different from the content in the working directory */
        for (String fileAdd : filesInAdd) {
            /* If the file exists in the staging area but not in the workspace, it is directly entered  modifiedFilesList */
            if (!join(CWD, fileAdd).exists()) {
                deletedFilesList.add(fileAdd);
                continue;
            }
            String workFileContent = readContentsAsString(join(CWD, fileAdd));
            String addStageBlobName = readContentsAsString(join(ADD_STAGE_DIR, fileAdd));
            String addStageFileContent = readContentsAsString(join(BLOBS_FOLDER, addStageBlobName));
            if (!workFileContent.equals(addStageFileContent)) {
                // If the content of the CWD is inconsistent with the Chinese content of addStage, enter the modifiedFilesList
                modifiedFilesList.add(fileAdd);
            }
        }

        /* Tracked in the current commit, changed in the working directory, but not staged */
        for (String trackFile : trackFileSet) {
            if (trackFile.isEmpty() || trackFile == null) {
                continue;
            }
            File workFile = join(CWD, trackFile);
            File fileInRmStage = join(REMOVE_STAGE_DIR, trackFile);
            if (!workFile.exists()) {      // When the workspace file does not exist
                if (!fileInRmStage.exists()) {
                    deletedFilesList.add(trackFile);       // There is no such file in rmStage and workspace
                }
                continue;
            }
            if (!filesInAdd.contains(trackFile)) { // When this file is not exist in addStage
                String workFileContent = readContentsAsString(workFile);
                String blobFileContent = readContentsAsString(join(BLOBS_FOLDER,
                        blobMap.get(trackFile)));
                if (!workFileContent.equals(blobFileContent)) {
                    // When the file being tracked was modified, not exist in addStage, enter modifiedFilesList
                    modifiedFilesList.add(trackFile);
                }
            }
        }
        printStatusWithStatus("Modifications Not Staged For Commit",
                modifiedFilesList, deletedFilesList);
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
     * java gitlet.Main checkout -- [file name]
     * java gitlet.Main checkout [commit id] -- [file name]
     * java gitlet.Main checkout [branch name]
     *
     * @param args
     */
    public static void checkOut(String[] args) {
        String fileName;
        if (args.length == 2) {
            //  git checkout branchName
            checkoutBranch(args[1]);
        } else if (args.length == 4) {
            //  git checkout [commit id] -- [file name]
            if (!args[2].equals("--")) {
                message("Incorrect operands.");
                exit(0);
            }

            fileName = args[3];
            String commitId = args[1];
            Commit commit = getHeadCommit();


            if (getCommitFromId(commitId) == null) {
                System.out.println("No commit with that id exists.");
                exit(0);
            } else {
                commit = getCommitFromId(commitId);
            }

            if (!commit.getBlobMap().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                exit(0);
            }
            String blobName = commit.getBlobMap().get(fileName);
            String targetBlobContent = getBlobContentFromName(blobName);

            /* overWriteFileWithBlob in CWD */
            File fileInWorkDir = join(CWD, fileName);
            overWriteFileWithBlob(fileInWorkDir, targetBlobContent);

        } else if (args.length == 3) {
            //  git checkout -- [file name]
            fileName = args[2];
            Commit headCommit = getHeadCommit();
            if (!headCommit.getBlobMap().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                exit(0);
            }
            String blobName = headCommit.getBlobMap().get(fileName);
            String targetBlobContent = getBlobContentFromName(blobName);

            File fileInWorkDir = join(CWD, fileName);
            overWriteFileWithBlob(fileInWorkDir, targetBlobContent);

        }
    }

    /**
     * only for
     * java gitlet.Main checkout [branch name]
     *
     * @param branchName
     */
    public static void checkoutBranch(String branchName) {
        Commit headCommit = getHeadCommit();

        if (branchName.equals(getHeadBranchName())) {
            System.out.println("No need to checkout the current branch.");
            exit(0);
        }

        Commit branchHeadCommit = getBranchHeadCommit(branchName, "No such branch exists");
        HashMap<String, String> branchHeadBlobMap = branchHeadCommit.getBlobMap();
        Set<String> fileNameSet = branchHeadBlobMap.keySet();

        List<String> workFileNames = plainFilenamesIn(CWD);

        if (untrackFileExists(headCommit)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            exit(0);
        }

        /* clean CWD */
        for (String workFile : workFileNames) {
            restrictedDelete(join(CWD, workFile));
        }

        /* writeContents in CWD with fileNameSet */
        for (var trackedfileName : fileNameSet) {
            File workFile = join(CWD, trackedfileName);
            String blobHash = branchHeadBlobMap.get(trackedfileName);
            String blobFromNameContent = getBlobContentFromName(blobHash);
            writeContents(workFile, blobFromNameContent);
        }

        /* The currently given branch as the current branch */
        saveHEAD(branchName, branchHeadCommit.getHashName());
    }


    /**
     * java gitlet.Main branch [branch name]
     */
    public static void createBranch(String branchName) {
        Commit headCommit = getHeadCommit();
        List<String> fileNameinHeadDir = plainFilenamesIn(HEAD_DIR);
        if (fileNameinHeadDir.contains(branchName)) {
            message("A branch with that name already exists.");
            exit(0);
        }

        saveBranch(branchName, headCommit.getHashName());

    }


    /**
     * java gitlet.Main rm-branch [branch name]
     */
    public static void removeBranch(String branchName) {
        File brancheFile = join(HEAD_DIR, branchName);
        if (!brancheFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            exit(0);
        }

        if (getHeadBranchName().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            exit(0);
        }

        File branchHeadPoint = join(HEAD_DIR, branchName);
        branchHeadPoint.delete();
    }

    /**
     * java gitlet.Main reset [commit id]
     *
     * @apiNote Convert all the contents of the file to the file in [commit id].
     */
    public static void reset(String commitId) {

        if (getCommitFromId(commitId) == null) {
            System.out.println("No commit with that id exists.");
            exit(0);
        }
        Commit headCommit = getHeadCommit();
        Commit commit = getCommitFromId(commitId);  // commit for reset
        HashMap<String, String> commitBlobMap = commit.getBlobMap();

        List<String> workFileNames = plainFilenamesIn(CWD);
        /* checking files were tracked in CWD by current branch */
        if (untrackFileExists(headCommit)) {
            Set<String> currTrackSet = headCommit.getBlobMap().keySet();
            Set<String> resetTrackSet = commit.getBlobMap().keySet();
            boolean isUntrackInBoth = false;

            /* workfile not in headCommit, and also not in commit, be deleted in addStage,but saved in CWD */
            for (String workFile : workFileNames) {
                if (!currTrackSet.contains(workFile) && !resetTrackSet.contains(workFile)) {
                    removeStage(workFile);
                    isUntrackInBoth = true;
                    break;
                }
            }
            if (!isUntrackInBoth) {
                message("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                exit(0);
            }

        }

        for (String workFile : workFileNames) {
            restrictedDelete(join(CWD, workFile));
        }

        for (var trackedfileName : commit.getBlobMap().keySet()) {
            File workFile = join(CWD, trackedfileName);
            String blobHash = commitBlobMap.get(trackedfileName);
            String blobFromNameContent = getBlobContentFromName(blobHash);
            writeContents(workFile, blobFromNameContent);
        }

        /* branchHEAD point to commit*/
        saveBranch(getHeadBranchName(), commitId);
        /* also HEAD pointer point to commit */
        saveHEAD(getHeadBranchName(), commitId);
    }


    /**
     * Merges files from the given branch into the current branch.
     * If the split point is the same commit as the given branch,
     * then we do nothing; the merge is complete, and the operation ends with the message:
     * Given branch is an ancestor of the current branch.
     * <p>
     * If the split point is the current branch, then the effect is to check out the given branch,
     * and the operation ends after printing the message: Current branch fast-forwarded.
     * Otherwise, we continue with the steps below.
     *
     * @apiNote cases
     * 1. other：modified      HEAD：not modified --->  working DIR: other, need to be added
     * 2. other：not modified    HEAD：modified   --->  working DIR: HEAD
     * 3. other：modified      HEAD：modified   --->  （consistent Modifications）  working DIR: HEAD, do nothing
     * |->  （not consistent Modifications）  working DIR: Conflict
     * 4. split：not exists      other：not exists    HEAD：be added   --->  working DIR: HEAD
     * 5. split：not exists      other：be added    HEAD：not exists   --->  working DIR: other, need to be added
     * 6. other：be deleted      HEAD：not modified   --->  working DIR: be deleted,saved in removal
     * 7. other：not modified     HEAD：be deleted   --->  working DIR: be deleted
     */
    public static void mergeBranch(String branchName) {
        checkSafetyInMerge(branchName);
        Commit headCommit = getHeadCommit();
        Commit otherHeadCommit = getBranchHeadCommit(branchName,
                "A branch with that name does not exist.");
        /* get currently splitCommit */
        Commit splitCommit = getSplitCommit(headCommit, otherHeadCommit);
        if (splitCommit.getHashName().equals(otherHeadCommit.getHashName())) {
            message("Given branch is an ancestor of the current branch.");
            exit(0);
        }

        HashMap<String, String> splitCommitBolbMap = splitCommit.getBlobMap();
        Set<String> splitKeySet = splitCommitBolbMap.keySet();
        HashMap<String, String> headCommitBolbMap = headCommit.getBlobMap();
        Set<String> headKeySet = headCommitBolbMap.keySet();
        HashMap<String, String> otherHeadCommitBolbMap = otherHeadCommit.getBlobMap();
        Set<String> otherKeySet = otherHeadCommitBolbMap.keySet();

        processSplitCommit(splitCommit, headCommit, otherHeadCommit);
        /* to Resolve The Deletion Operation */
        for (var headTrackName : headKeySet) {
            if (!otherHeadCommitBolbMap.containsKey(headTrackName)) {
                if (!splitCommitBolbMap.containsKey(headTrackName)) {
                    /* case4：If it don't have this file in either other or split commit*/
                    continue;
                } else {
                    /* split：exists  other：be deleted */
                    if (!headCommitBolbMap.get(headTrackName)
                            .equals(splitCommitBolbMap.get(headTrackName))) {
                        /* HEAD：be modified */
                        /* exists conflict */
                        processConflict(headCommit, otherHeadCommit, headTrackName);
                    }
                    /* other cases are case6 were dealt with */
                }
            } else if (otherHeadCommitBolbMap.containsKey(headTrackName)
                    && !splitCommitBolbMap.containsKey(headTrackName)) {
                /* case3b if file exists other commit, file not exists in split commit,not consistent modified*/
                if (!otherHeadCommitBolbMap.get(headTrackName)
                        .equals(headCommitBolbMap.get(headTrackName))) {
                    /*if is not consistent modified, processConflict,if consistent continue*/
                    processConflict(headCommit, otherHeadCommit, headTrackName);
                }
            }
        }
        for (var otherTrackName : otherKeySet) {
            if (!headCommitBolbMap.containsKey(otherTrackName)
                    && !splitCommitBolbMap.containsKey(otherTrackName)) {
                /* case5：if not exists in head and split */
                String[] checkOutArgs = {"checkout",
                                         otherHeadCommit.getHashName(),
                                         "--",
                                         otherTrackName};
                checkOut(checkOutArgs);
                addStage(otherTrackName);
            }
        }
        if (splitCommit.getHashName().equals(headCommit.getHashName())) {
            message("Current branch fast-forwarded.");
        }
        /* automatic commit */
        String commitMsg = String.format("Merged %s into %s.", branchName, getHeadBranchName());
        commitFileForMerge(commitMsg, branchName);
    }


    public static void processSplitCommit(Commit splitCommit, Commit headCommit,
                                          Commit otherHeadCommit) {
        HashMap<String, String> splitCommitBolbMap = splitCommit.getBlobMap();
        Set<String> splitKeySet = splitCommitBolbMap.keySet();
        HashMap<String, String> headCommitBolbMap = headCommit.getBlobMap();
        Set<String> headKeySet = headCommitBolbMap.keySet();
        HashMap<String, String> otherHeadCommitBolbMap = otherHeadCommit.getBlobMap();
        Set<String> otherKeySet = otherHeadCommitBolbMap.keySet();

        /* starts in split */
        for (var splitTrackName : splitKeySet) {
            // if not be modified in HEAD(including be deleted)
            if (headCommitBolbMap.containsKey(splitTrackName)
                    && headCommitBolbMap.get(splitTrackName)
                            .equals(splitCommitBolbMap.get(splitTrackName))) {
                // if file not exists in other
                if (otherHeadCommitBolbMap.containsKey(splitTrackName)) {
                    /* case1 not be modified in HEAD,be modified in other*/
                    if (!otherHeadCommitBolbMap.get(splitTrackName)
                            .equals(splitCommitBolbMap.get(splitTrackName))) {
                        // use checkout to overwrite with other commit files in CWD,and put it into staging area
                        String[] checkOutArgs = {"checkout",
                                                 otherHeadCommit.getHashName(),
                                                 "--",
                                                 splitTrackName};
                        checkOut(checkOutArgs);
                        addStage(splitTrackName);
                    }
                } else {
                    /* case6: when HEAD not modified,be deleted in other commit*/
                    removeStage(splitTrackName);
                }
            } else {
                // be modified in HEAD(including be deleted)
                if (otherHeadCommitBolbMap.containsKey(splitTrackName)
                        && otherHeadCommitBolbMap.get(splitTrackName)
                                .equals(splitCommitBolbMap.get(splitTrackName))) {
                    /* case2 other commit not be modified,be modified,do nothingin HEAD
                       case7 other commit be modified,be deleted,do nothing in HEAD*/
                    continue;
                } else {
                    /* other commit be modified or be deleted */
                    if (!otherHeadCommitBolbMap.containsKey(splitTrackName)
                            && !headCommitBolbMap.containsKey(splitTrackName)) {
                        /* case3a consistent deleted */
                        continue;
                    } else if (!otherHeadCommitBolbMap.containsKey(splitTrackName)
                            || !headCommitBolbMap.containsKey(splitTrackName)) {
                        /* only exists deleted file in one side,continue,Later, the HEAD and other pointers are operated separately */
                        continue;
                    } else {
                        if (otherHeadCommitBolbMap.get(splitTrackName)
                                .equals(headCommitBolbMap.get(splitTrackName))) {
                            /* case3a consistent modified */
                            continue;
                        } else {
                            /* case3b not consistent modified,not including deleted */
                            processConflict(headCommit, otherHeadCommit, splitTrackName);
                        }
                    }
                }
            }
        }
    }


    /**
     * check merge instruction failure case
     *
     * @param branchName
     */
    public static void checkSafetyInMerge(String branchName) {

        List<String> addStageFiles = plainFilenamesIn(ADD_STAGE_DIR);
        List<String> rmStageFiles = plainFilenamesIn(REMOVE_STAGE_DIR);
        /* if exists in staging area */
        if (!addStageFiles.isEmpty() || !rmStageFiles.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            exit(0);
        }
        Commit headCommit = getHeadCommit();
        // if not exists this branch
        String errMsg = "A branch with that name does not exist.";
        Commit otherHeadCommit = getBranchHeadCommit(branchName, errMsg);

        if (getHeadBranchName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            exit(0);
        }
        /* check whether untracked files exists in CWD*/
        if (untrackFileExists(headCommit)) {
            message("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            exit(0);
        }
    }
}
