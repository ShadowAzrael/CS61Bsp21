package gitlet;

import java.io.File;
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
                if(args.length != 1) {
                    message("Incorrect operands.");
                    exit(0);
                }
                initPersistence();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                String addFileName = args[1];
                addStage(addFileName);
                break;
            // TODO: FILL THE REST IN
        }
    }
}
