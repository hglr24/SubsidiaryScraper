import java.io.FileNotFoundException;
import java.io.PrintWriter;

class Logger {

    private static final String LOG_DIRECTORY = "/output/logs/";
    private static final String LOG_FILE_SUFFIX = "_log.txt";
    private static final String ERROR_GENERATING_LOG_FILE = "Error generating log file";

    private StringBuilder myLog;

    Logger() {
        myLog = new StringBuilder();
        Utility.createOutputDirectories(LOG_DIRECTORY);
    }

    void addEvent(LoggerEvent event, String fileName, String desc) {
        String newLine = "[" + event + ", " + fileName + "]: " + desc + "\n";
        myLog.append(newLine);
    }

    void addEvent(LoggerEvent event, String fileName, String desc, String line) {
        String newLine = "[" + event + ", " + fileName + "]: " + desc + " [ORIGINAL LINE: " + line + "]\n";
        myLog.append(newLine);
    }

    void finalizeLog(String timeData) {
        try {
            PrintWriter writer = new PrintWriter(Utility.getRunningPath() + LOG_DIRECTORY + timeData + LOG_FILE_SUFFIX);
            writer.print(myLog.toString());
            writer.close();
        } catch (FileNotFoundException e) {
            System.out.println(ERROR_GENERATING_LOG_FILE);
        }
    }
}
