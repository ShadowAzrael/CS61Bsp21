package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Index implements Serializable {
    // The HashMap of staged files
    // <filename, blob's id>
    private HashMap<String, String> added;
    // <filename>
    private HashSet<String> removed;

    private HashSet<String> tracked;

    public Index(){
        added = new HashMap<>();
        removed = new HashSet<>();
        tracked = new HashSet<>();
    }

    public void addFile(String filename, String blobId) {
        added.put(filename, blobId);
        removed.remove(filename);
    }

    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty();
    }

    public HashMap<String, String> getAdded() {
        return added;
    }

    public HashSet<String> getRemoved() {
        return removed;
    }

    public ArrayList<String> getStagedFileName() {
        ArrayList<String> ret = new ArrayList<>();
        ret.addAll(added.keySet());
        ret.addAll(removed);
        return ret;
    }
}
