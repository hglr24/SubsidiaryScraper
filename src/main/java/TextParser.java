import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TextParser {

    private static final int RELEVANT_TIME = 17;
    private static final int MIN_VALID_LENGTH = 5;
    private static final int MIN_SUB_NAME_FLAG = 8;
    private static final int MAX_SUB_NAME_FLAG = 60;
    private static final int EMPTY_DATA_LINE_COUNT_FLAG = 5;
    private static final int MANY_COUNTRIES_THRESHOLD = 2;
    private static final double DATA_LOSS_THRESHOLD = 0.30;
    private static final int MIN_DATA_LOSS_SUSPICION = 30;

    private static final String OUTPUT_DIR = "/output/";
    private static final String CSV_OUTPUT_FILE = "_results.csv";
    private static final String COUNTRIES_LIST_FILE = "/resources/country_list.txt";
    private static final String BLACKLIST_FILE = "/resources/blacklist_terms.txt";

    private static final List<String> CSV_HEADER = Arrays.asList("FIRMID", "SUBSIDIARY NAME", "LOCATION");
    private static final List<String> REMOVE_LINE_ENDINGS = Arrays.asList(" the", " a", " an", " of", " in");
    private static final String WHITESPACE_REGEX = "\\p{Z}";
    private static final String DOT_REGEX = "\\.";
    private static final String QUESTION_REGEX = "\\?";
    private static final String BULLET_REGEX = "[\u2022]";
    private static final String COUNTRY_ABBR_PREFIX = "(\\p{Z}|^|\\()";
    private static final String COUNTRY_ABBR_SUFFIX = "(\\p{Z}|$|\\)|,)";
    private static final String NOT_CAP_OR_NUM_REGEX = "([A-Z]|[0-9])";
    private static final String TXT_SUFFIX = ".txt";
    private static final String CSV_ERROR = "Error creating CSV results file";
    private static final String ERROR_ANALYZING_HTML_FILE = "Error analyzing HTML file ";
    private static final String ERROR_AT_LINE = " at line: ";
    private static final String SHORT_SUB_NAME_LOG = "Short subsidiary name (<8 chars)";
    private static final String LONG_SUB_NAME_LOG = "Long subsidiary name (>60 chars)";
    private static final String FIRST_CHAR_PROBLEM_LOG = "First character of subsidiary is not upper case or number";
    private static final String DATA_LOSS_EMPTY_LOG = "Potential data loss: No results generated for non-empty file";
    private static final String DATA_LOSS_THRESHOLD_LOG = "Potential data loss: Less than 35% of original file lines translated to results";
    private static final String MANY_COUNTRIES_LOG = "More than two countries found on line, subsidiary name may contain unnecessary data";

    private List<List<String>> myDataLines = new ArrayList<>();
    private Logger myLogger = new Logger();

    void analyzeTextFile(File textFile) throws IOException {
        if (!myDataLines.contains(CSV_HEADER))
            myDataLines.add(CSV_HEADER);

        StringBuilder builder = new StringBuilder();
        Scanner scan = new Scanner(textFile);
        int lineCount = 0;

        while (scan.hasNextLine()) {         // File to String
            String currentLine = scan.nextLine();
            while(currentLine.length() > 1 && currentLine.charAt(0) == ' ') {   // Delete leading Whitespace
                currentLine = currentLine.substring(1);
            }
            builder.append(currentLine);
            builder.append('\n');
            lineCount++;
        }
        scan.close();
        int oldLineCount = myDataLines.size();
        generateDataLines(builder.toString(), textFile.getName());
        logCheckDataLoss(lineCount, oldLineCount, textFile.getName().replace(TXT_SUFFIX, ""));
    }

    private void generateDataLines(String dataString, String idStuff)  {
            Scanner scan = new Scanner(dataString);
            while (scan.hasNextLine()) {
                String currentLine = scan.nextLine();
                try {
                    scanForCountries(currentLine, idStuff.replace(TXT_SUFFIX, ""));
                } catch (IOException e) {
                    System.out.println(ERROR_ANALYZING_HTML_FILE + idStuff + ERROR_AT_LINE + currentLine);
                }
            }
            scan.close();
    }

    private void scanForCountries(String currentLine, String firmId) throws IOException {
        List<String> currentCountryList;
        String lastCountryAbbr = "";
        String lastFormalCountry = "";
        int lastAbbrStartIndex = 0;
        int lastAbbrEndIndex = 0;
        int foundCountries = 0;
        Scanner scan = new Scanner(new File(Utility.getRunningPath() + COUNTRIES_LIST_FILE));

        while (scan.hasNextLine()) {
            currentCountryList = new ArrayList<>(Arrays.asList(scan.nextLine().split("=")));
            List<String> upperCaseTerms = new ArrayList<>();
            for (String term : currentCountryList)
                upperCaseTerms.add(term.toUpperCase());
            for (String upper : upperCaseTerms) {
                if (!currentCountryList.contains(upper))
                    currentCountryList.add(0, upper);
            }
            for (String countryAbbr : currentCountryList) {
                String regex = COUNTRY_ABBR_PREFIX + countryAbbr + COUNTRY_ABBR_SUFFIX;
                Matcher matchLine = Pattern.compile(regex).matcher(currentLine);
                if (matchLine.find()) {
                    foundCountries++;
                    int matchStartIndex = matchLine.start();
                    int matchEndIndex = matchLine.end();
                    while (matchLine.find()) {
                        matchStartIndex = matchLine.start();
                        matchEndIndex = matchLine.end();
                    }
                    if (matchEndIndex > lastAbbrEndIndex ||
                            (matchEndIndex == lastAbbrEndIndex && countryAbbr.length() > lastCountryAbbr.length())) {
                        lastCountryAbbr = countryAbbr;
                        lastFormalCountry = currentCountryList.get(currentCountryList.size() - 1);
                        lastAbbrStartIndex = matchStartIndex;
                        lastAbbrEndIndex = matchEndIndex;
                    }
                }
            }
        }
        scan.close();
        addDataLine(currentLine, firmId, lastCountryAbbr, lastFormalCountry, lastAbbrStartIndex);
        logManyCountries(currentLine, firmId, foundCountries);
    }

    private void addDataLine(String currentLine, String firmId, String lastCountryAbbr, String lastFormalCountry, int lastAbbrIndex) throws IOException {
        String subName;
        if (lastCountryAbbr != null && currentLine.lastIndexOf(lastCountryAbbr) != 0) {  // Make sure non-null and not first thing before adding data line
            subName = currentLine.substring(0, lastAbbrIndex);
            subName = cleanName(subName); // Clean name before adding data line

            if (checkValidity(subName)) { // Check validity before adding data line
                List<String> newDataLine = Arrays.asList(firmId, subName, lastFormalCountry);
                myDataLines.add(newDataLine);
                logNameSyntax(currentLine, firmId, subName);
            }
        }
    }

    private String cleanName(String subName) {
        subName = stripName(subName, BULLET_REGEX);
        subName = stripName(subName, QUESTION_REGEX);
        subName = stripName(subName, WHITESPACE_REGEX);
        subName = stripName(subName, BULLET_REGEX);
        subName = stripName(subName, QUESTION_REGEX);
        subName = stripName(subName, DOT_REGEX);
        subName = removePercentageIfPresent(subName);
        subName = subName.replace(",", "");     // For CSV formatting

        for (String ending : REMOVE_LINE_ENDINGS) {
            if (subName.toLowerCase().endsWith(ending)) {
                subName = subName.substring(0, subName.length() - ending.length());
            }
        }
        return subName;
    }

    void createCsv() {
        String timeString = java.time.LocalDateTime.now().toString()
                .replace(".", "").replace(":", "");
        File csvOutput = new File(Utility.getRunningPath() + OUTPUT_DIR + timeString.substring(0, RELEVANT_TIME) + CSV_OUTPUT_FILE);
        try (PrintWriter pw = new PrintWriter(csvOutput)) {
            myDataLines.forEach(pw::println);
        } catch (FileNotFoundException e) {
            System.out.println(CSV_ERROR);
        }
        myLogger.finalizeLog(timeString.substring(0, RELEVANT_TIME));
    }

    private boolean checkValidity(String name) throws IOException {
        if (name.length() < MIN_VALID_LENGTH)
            return false;

        Scanner scan = new Scanner(new File(Utility.getRunningPath() + BLACKLIST_FILE));
        while (scan.hasNextLine()) {
            String currentKeyword = scan.nextLine();
            if (Arrays.asList(name.toLowerCase().split(WHITESPACE_REGEX)).contains(currentKeyword)) {   // If line contains isolated blacklisted word
                scan.close();
                return false;
            }
        }
        scan.close();
        return true;
    }

    private String stripName(String input, String stripPattern) {
        String rtn = input;
        while (rtn.length() > 0 && rtn.substring(0, 1).matches(stripPattern)) {                        // Preceding spaces removal
            rtn = rtn.substring(1);
        }
        while (rtn.length() > 0 && rtn.substring(rtn.length() - 1).matches(stripPattern)) {        // Following spaces removal
            rtn = rtn.substring(0, rtn.length() - 1);
        }
        if (stripPattern.equals(DOT_REGEX) && !input.equals(rtn))
            rtn = rtn + ".";
        return rtn;
    }

    private String removePercentageIfPresent(String subName) {
        if (subName.contains("%")) {
            int beforePerc = subName.lastIndexOf(' ', subName.indexOf('%'));      // Percentage removal
            if (beforePerc != -1) {
                subName = subName.substring(0, beforePerc);
                subName = cleanName(subName);
            }
        }
        return subName;
    }

    private void logCheckDataLoss(int lineCount, int oldLineCount, String firmId) {
        if (myDataLines.size() == oldLineCount && lineCount > EMPTY_DATA_LINE_COUNT_FLAG) {
            myLogger.addEvent(LoggerEvent.FILE_EVENT, firmId, DATA_LOSS_EMPTY_LOG);
        }
        else if ((myDataLines.size() - oldLineCount) != 0 && lineCount > MIN_DATA_LOSS_SUSPICION &&
                ((1.0 * (myDataLines.size() - oldLineCount)) / lineCount) < DATA_LOSS_THRESHOLD) {
            myLogger.addEvent(LoggerEvent.FILE_EVENT, firmId, DATA_LOSS_THRESHOLD_LOG);
        }
    }

    private void logNameSyntax(String currentLine, String firmId, String subName) {
        if (subName.length() < MIN_SUB_NAME_FLAG)
            myLogger.addEvent(LoggerEvent.LINE_EVENT, firmId, SHORT_SUB_NAME_LOG, currentLine);
        if (subName.length() > MAX_SUB_NAME_FLAG)
            myLogger.addEvent(LoggerEvent.LINE_EVENT, firmId, LONG_SUB_NAME_LOG, currentLine);
        if (!subName.substring(0, 1).matches(NOT_CAP_OR_NUM_REGEX))
            myLogger.addEvent(LoggerEvent.LINE_EVENT, firmId, FIRST_CHAR_PROBLEM_LOG, currentLine);
    }

    private void logManyCountries(String currentLine, String firmId, int foundCountries) {
        if (foundCountries > MANY_COUNTRIES_THRESHOLD) {
            myLogger.addEvent(LoggerEvent.LINE_EVENT, firmId, MANY_COUNTRIES_LOG, currentLine);
        }
    }
}
