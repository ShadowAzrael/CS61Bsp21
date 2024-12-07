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
 *  @author Li
 */
public class Repository {
    /**
     *
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

            //delete file in Cwd, only when this file was tracked
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
     * java gitlet.Main rm-branch [branchname]
     */
    public static void removeBranch(String branchName) {
        File branchFile = join(HEAD_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            exit(0);
        }
        //branch is current branch or not
        Commit headCommit = getHeadCommit();
        if (getHeadBranchName().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            exit(0);
        }
        //delete this branch pointer file
        File branchHeadPoint = join(HEAD_DIR, branchName);
        branchHeadPoint.delete();
    }

    /**
     * java gitlet.Main reset [commit id]
     *
     *
     */
    public static void reset (String commitId) {
        if(getCommitFromId(commitId) == null) {
            System.out.println("No commit with that id exists.");
            exit(0);
        }
        Commit headCommit = getHeadCommit();
        Commit commit = getCommitFromId(commitId);
        HashMap<String, String> commitBlobMap = commit.getBlobMap();

        //detect whether there are files in the CWD that are not tracked by the current branch
        List <String> workFileNames = plainFilenamesIn(CWD);
        if (untrackFileExists(headCommit)) {
            Set<String> currTrackSet = headCommit.getBlobMap().keySet();
            Set<String> resetTrackSet = commit.getBlobMap().keySet();
            boolean isUntracked = false;

            /* workfile not in headCommit and commit, deleted in addStage, but saved in CWD */
            for (String workFile : workFileNames) {
                if (!currTrackSet.contains(workFile) && !resetTrackSet.contains(workFile)) {
                    removeStage(workFile);
                    isUntracked = true;
                    break;
                }
            }
            if(!isUntracked) {
                message("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                exit(0);
            }
        }

        for (String workFile : workFileNames) {
            restrictedDelete(join(CWD, workFile));
        }

        for (var trackedFileName : commitBlobMap.keySet()) {
            File workFile = join(CWD, trackedFileName);
            String blobHash = commitBlobMap.get(trackedFileName);
            String blobFromNameContent = getBlobContentFromName(blobHash);
            writeContents(workFile, blobFromNameContent);
        }

        //branch and HEAD point to this commit
        saveBranch(getHeadBranchName(), commitId);
        saveHEAD(commit.getHashName(), commitId);
    }

    /**
     * Merges files from the given branch into the current branch.
     * If the split point is the same commit as the given branch,do nothing;
     * the merge is complete, and the operation ends with the message:
     * Given branch is an ancestor of the current branch.
     *  <p>
     * If the split point is the current branch, then the effect is to check out the given branch,
     * and the operation ends after printing the message: Current branch fast-forwarded.
     * Otherwise, we continue with the steps below.
     *
     * @apiNote
     * case1. other: modified       HEAD:not modified -> CWD: other, need to be added
     * case2. other: not modified   HEAD:modified     -> CWD: HEAD
     * case3. other: modified       HEAD:modified     -> CWD: HEAD (consistent modified),do nothing
     *                                               \-> CWD: Conflict (not consistent)
     * case4. split: not exists     other:not exists     HEAD:be added   -> CWD: HEAD
     * case5. split: not exists     other:be added       HEAD:not exists -> CWD; other, do nothing
     * case6. other: be deleted     HEAD:not modified -> CWD: be deleted, and stage in removal
     * case7. other: not mofidied   HEAD:be deleted   -> CWD: be deleted
     */
    public static void mergeBranch(String branchName) {
        checkSafetyInMerge(branchName);
        Commit headCommit = getHeadCommit();
        Commit otherHeadCommit = getBranchHeadCommit(branchName, "A branch with that name does not exist.");

        //get the spiltCommit Object
        Commit splitCommit = getSplitCommit(headCommit, otherHeadCommit);
        if (splitCommit.getHashName().equals(otherHeadCommit.getHashName())) {
            message("Given branch is an ancestor of the current branch.");
            exit(0);
        }

        HashMap<String, String> splitCommitBolbMap = splitCommit.getBlobMap();
        Set<String> splitkeySet = splitCommitBolbMap.keySet();
        HashMap<String, String> headCommitBolbMap = headCommit.getBlobMap();
        Set<String> headKeySet = headCommitBolbMap.keySet();
        HashMap<String, String> otherHeadCommitBolbMap = otherHeadCommit.getBlobMap();
        Set<String> otherKeySet = otherHeadCommitBolbMap.keySet();

        processSplitCommit(splitCommit, headCommit, otherHeadCommit);

        /* To resolve the deletion operation */
        for (var headTrackName : headKeySet) {
            if (!otherHeadCommitBolbMap.containsKey(headTrackName)) {
                if (!splitCommitBolbMap.containsKey(headTrackName)) {
                    /* case4：If it don't have this file in either other or split */
                    continue;
                } else {
                    /* split：exists  other：deleted */
                    if (!headCommitBolbMap.get(headTrackName)
                            .equals(splitCommitBolbMap.get(headTrackName))) {
                        /* HEAD：modified */
                        /* conflict */
                        processConflict(headCommit, otherHeadCommit, headTrackName);
                    }
                    /* In other cases, case 6 has been deal with */
                }
            } else if (otherHeadCommitBolbMap.containsKey(headTrackName)
                    && !splitCommitBolbMap.containsKey(headTrackName)) {
                /* case3b other exists, split not exists，not consistent modified*/
                if (!otherHeadCommitBolbMap.get(headTrackName)
                        .equals(headCommitBolbMap.get(headTrackName))) {
                    /*if not consistent modified,processConflict,if consistent continue*/
                    processConflict(headCommit, otherHeadCommit, headTrackName);
                }
            }
        }
        for (var otherTrackName : otherKeySet) {
            if (!headCommitBolbMap.containsKey(otherTrackName)
                    && !splitCommitBolbMap.containsKey(otherTrackName)) {
                /* case5: not exists in head and split */
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
        /* do An Automatic Commit */
        String commitMsg = String.format("Merged %s into %s.", branchName, getHeadBranchName());
        commitFileForMerge(commitMsg, branchName);
    }

    public static void checkSafetyInMerge(String branchName) {
        List<String> addStageFiles = plainFilenamesIn(ADD_STAGE_DIR);
        List<String> removeStageFiles = plainFilenamesIn(REMOVE_STAGE_DIR);

        if(!addStageFiles.isEmpty() || !removeStageFiles.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            exit(0);
        }

        Commit headCommit = getHeadCommit();

        String errMsg = "A branch with that name does not exist.";

        Commit otherHeadcommit = getBranchHeadCommit(branchName, errMsg);

        if(getHeadBranchName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
        }

        if (untrackFileExists(headCommit)) {
            message("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            exit(0);
        }
    }

    public static void processSplitCommit(Commit splitCommit, Commit headCommit, Commit otherHeadCommit) {
        HashMap<String, String> splitCommitBolbMap = splitCommit.getBlobMap();
        Set<String> splitKeySet = splitCommitBolbMap.keySet();
        HashMap<String, String> headCommitBolbMap = headCommit.getBlobMap();
        Set<String> headKeySet = headCommitBolbMap.keySet();
        HashMap<String, String> otherHeadCommitBolbMap = otherHeadCommit.getBlobMap();
        Set<String> otherKeySet = otherHeadCommitBolbMap.keySet();

        //start in split
        for (var splitTrackName : splitKeySet) {
            //if files not modified in HEAD,include not being deleted
            if (headCommitBolbMap.containsKey(splitTrackName)
                    && headCommitBolbMap.get(splitTrackName).equals(splitCommitBolbMap.get(splitTrackName))) {
                //if exists in other
                if (otherHeadCommitBolbMap.containsKey(splitTrackName)) {
                    //case1 not modified in HEAD, modified in other
                    if(!otherHeadCommitBolbMap.get(splitTrackName)
                            .equals(splitCommitBolbMap.get(splitTrackName))) {
                        // use checkout to overwrite in CWD with other files, and add in addStage
                        String[] checkArgs = {"checkout", otherHeadCommit.getHashName(), "--", splitTrackName}
                        checkOut(checkArgs);
                        addStage(splitTrackName);
                    }
                } else {
                    //case6 not modified in HEAD, deleted in other
                    removeStage(splitTrackName);
                }
            } else {
                //modified in HEAD
                if (otherHeadCommitBolbMap.containsKey(splitTrackName)
                && otherHeadCommitBolbMap.get(splitTrackName).equals(splitCommitBolbMap.get(splitTrackName))) {
                    /*case2 other not modified, HEAD modified, do nothing
                    case7 other not modified, HEAD deleted, do nothing
                     */
                    continue;
                } else {
                    //modified and deleted in head
                    if (!otherHeadCommitBolbMap.containsKey(splitTrackName)
                    && !headCommitBolbMap.containsKey(splitTrackName)) {
                        //case3a Consistent deleted
                        continue;
                    } else if (!otherHeadCommitBolbMap.containsKey(splitTrackName)
                    || headCommitBolbMap.containsKey(splitTrackName)) {
                        // only one headCommit delted, continue, move HEAD and other pointer
                        continue;
                    } else {
                        if (otherHeadCommitBolbMap.get(splitTrackName)
                                .equals(headCommitBolbMap.get(splitTrackName))) {
                            //case3a consistent deleted
                            continue;
                        } else {
                            // case3b not consistent modified, not include being deleted
                            processConflict(headCommit, otherHeadCommit, splitTrackName);
                        }
                    }
                }
            }

        }
    }

    //for file both deleted in 2 headCommit
    public static void processConflict(Commit headCommit, Commit otherHeadCommit, String splitTrackName) {
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

    /**
     * 根据commit重载的方法，作用是为了进行merge时候的自动commit
     *
     * @param commitMsg
     * @param branchName
     */
    public static void commitFileForMerge(String commitMsg, String branchName) {


        /* 获取addstage中的filename和hashname */
        List<String> addStageFiles = plainFilenamesIn(ADD_STAGE_DIR);
        List<String> removeStageFiles = plainFilenamesIn(REMOVE_STAGE_DIR);
        /* 错误的情况，直接返回 */
        if (addStageFiles.isEmpty() && removeStageFiles.isEmpty()) {
            System.out.println("No changes added to the commit.");
            exit(0);
        }

        if (commitMsg == null) {
            System.out.println("Please enter a commit message.");
            exit(0);
        }

        /* 获取最新的commit*/
        Commit oldCommit = getHeadCommit();
        Commit branchHeadCommit = getBranchHeadCommit(branchName, null);

        /* 创建新的commit，newCommit根据oldCommit进行调整*/
        Commit newCommit = new Commit(oldCommit);
        newCommit.setDirectParent(oldCommit.getHashName());  // 指定父节点
        newCommit.setTimestamp(new Date(System.currentTimeMillis())); // 修改新一次的commit的时间戳为目前时间
        newCommit.setMessage(commitMsg); // 修改新一次的commit的时间戳为目前时间
        newCommit.setOtherParent(branchHeadCommit.getHashName());   // 指定另一个父节点

        /* 对每一个addstage中的fileName进行其路径的读取，保存进commit的blobMap */
        for (String stageFileName : addStageFiles) {
            String hashName = readContentsAsString(join(ADD_STAGE_DIR, stageFileName));
            newCommit.addBlob(stageFileName, hashName);     // 在newCommit中更新blob
            join(ADD_STAGE_DIR, stageFileName).delete();
        }

        HashMap<String, String> blobMap = newCommit.getBlobMap();

        /* 对每一个rmstage中的fileName进行其路径的读取，删除commit的blobMap中对应的值 */
        for (String stageFileName : removeStageFiles) {
            if (blobMap.containsKey(stageFileName)) {
                join(BLOBS_FOLDER, blobMap.get(stageFileName)).delete(); // 删除blobs中的文件
                newCommit.removeBlob(stageFileName);   // 在newCommit中删除removeStage中的blob
            }
            join(REMOVE_STAGE_DIR, stageFileName).delete();
        }

        newCommit.saveCommit();

        /* 更新HEAD指针和master指针 */
        saveHEAD(getHeadBranchName(), newCommit.getHashName());
        saveBranch(getHeadBranchName(), newCommit.getHashName());
    }
}
