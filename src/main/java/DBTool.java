import java.util.Arrays;

public class DBTool {

    private static void usage(String msg) {
        System.err.println("Error: " + msg);
        System.err.println("Usage: DBTool flatxml|csv");
        System.exit(2);
    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            usage("No argument");
        }
        String subCommand = args[0];
        String[] extraArgs = Arrays.copyOfRange(args, 1, args.length);
        if ("flatxml".equals(subCommand)) {
            FlatXmlExport.main(extraArgs);
        } else if ("csv".equals(subCommand)) {
            CSVExport.main(extraArgs);
        } else if ("tables".equals(subCommand)) {
            ListTables.main(extraArgs);
        } else {
            usage("Unknown command: " + subCommand);
        }
    }
}
