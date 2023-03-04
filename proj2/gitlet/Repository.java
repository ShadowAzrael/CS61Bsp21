package gitlet;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

import static gitlet.MyUtils.*;
import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
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
     */

    /** Default branch name. */
    public File CWD;

    public Repository() {
        this.CWD = new File(System.getProperty("user.dir"));
        configDIRS();
    }

    private void configDIRS() {
        this.GITLET_DIR = join(CWD, ".gitlet");
        this.STAGING_DIR = join(GITLET_DIR, ".staging");
        this.STAGE = join(GITLET_DIR, "stage");
        this.BLOB_DIR = join(GITLET_DIR, "blobs");
        this.COMMITS_DIR = join(GITLET_DIR, ".commmits");
        this.REFS_DIR = join(GITLET_DIR, ".refs");
        this.HEADS_DIR = join(REFS_DIR, ".HEADS");
        this.REMOTES_DIR = join(REFS_DIR, ".remotes");
        this.HEAD = join(GITLET_DIR, "HEAD");
        this.CONFIG = join(GITLET_DIR, "config");
    }

    public File GITLET_DIR;
    public File STAGING_DIR;
    public File STAGE;
    public File BLOB_DIR;
    public File COMMITS_DIR;
    public File REFS_DIR;
    public File HEADS_DIR;
    public File REMOTES_DIR;
    public File HEAD;
    public File CONFIG;

    /* TODO: fill in the rest of this class. */
    /**
     * The .gitlet directory.
     * <p>
     * .gitlet
     * -- staging
     * -- [stage]
     * -- blobs
     * -- commits
     * -- refs
     *  -- heads -> [master][branch name]
     *  -- remotes
     *      -- [remote name] ->[branch name]
     * -- [HEAD]
     * -- [config]
     */
    public void init() {
        if (GITLET_DIR.exists() && GITLET_DIR.isDirectory()) {
            exit("A Gitlet version-control system already exists in the current directory.");
        }

        GITLET_DIR.mkdir();
        STAGING_DIR.mkdir();
        writeObject(STAGE, new Stage());
        BLOB_DIR.mkdir();
        COMMITS_DIR.mkdir();
        REFS_DIR.mkdir();
        HEADS_DIR.mkdir();
        REMOTES_DIR.mkdir();

        //initialize commit
        Commit initialCommit = new Commit("initial commit", null);
        writeCommmitToFile(initialCommit);

        //create branch master
        writeContents(HEAD, "master");
        File master = join(HEADS_DIR, "master");
        writeContents(master, initialCommit.getId());

        //create HEAD
        writeContents(HEAD, "master");

        writeContents(CONFIG, "");
    }

    private void writeCommmitToFile(Commit commit) {
        File file = join(COMMITS_DIR, commit.getId());
        writeObject(file, commit);
    }

    /**
     * Add file to the staging area.
     *
     * @param fileName Name of the file
     */
    public static void add(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            exit("File does not exists.");
        }
        if ()
    }
}
