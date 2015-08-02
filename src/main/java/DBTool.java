import java.util.Arrays;

public class DBTool {

    private static void usage(String msg) {
        System.err.println("Error: " + msg);
        System.err.println("Usage: DBTool cli|execute");
        System.exit(2);
    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            usage("No argument");
        }
        String subCommand = args[0];
        String[] extraArgs = Arrays.copyOfRange(args, 1, args.length);
        if ("execute".equals(subCommand)) {
            ExecuteStatements.main(extraArgs);
        } else if ("cli".equals(subCommand)) {
            CLI.main(extraArgs);
        } else {
            usage("Unknown command: " + subCommand);
        }
    }
}
