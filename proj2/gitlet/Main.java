package gitlet;

import static gitlet.Repository.*;
import static gitlet.Utils.message;
import static java.lang.System.exit;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        checkArgsEmpty(args);
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                if (args.length != 1) {
                    message("Incorrect operands.");
                    exit(0);
                }
                initPersistence();
                break;
            case "add":
                String addFileName = args[1];
                addStage(addFileName);
                break;
            case "commit":
                String commitMsg = args[1];
                commitFile(commitMsg);
                break;
            case "rm":
                String rmFileName = args[1];
                removeStage(rmFileName);
                break;
            case "log":
                if (args.length != 1) {
                    message("Incorrect operands.");
                    exit(0);
                }
                printLog();
                break;
            case "global-log":
                if (args.length != 1) {
                    message("Incorrect operands.");
                    exit(0);
                }
                printGlobalLog();
                break;
            case "find":
                String findMsg = args[1];
                findCommit(findMsg);
                break;
            case "status":
                if (args.length != 1) {
                    message("Incorrect operands.");
                    exit(0);
                }
                showStatus();
                break;
            case "checkout":
                if (args.length == 1) {
                    message("Incorrect operands.");
                    exit(0);
                }
                checkOut(args);
                break;
        }
    }
}
