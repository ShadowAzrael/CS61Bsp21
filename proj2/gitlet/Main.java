package gitlet;

import java.io.File;

import static gitlet.MyUtils.exit;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                //Get the current working directort.
                Repository.init(args);
                //Branches? Here we need initialize a master branch and have it point to initial commit.
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                if (args[1] == null) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            // TODO: FILL THE REST IN
            default :
                exit("No command with that name exists.");
        }
    }
}
