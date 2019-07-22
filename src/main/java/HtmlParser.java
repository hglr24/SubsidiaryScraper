import java.util.Scanner;

class HtmlParser {

    private static final int NO_DATA_CUTOFF = 200;
    private static final String END_BRACKET = ">";
    private static final String START_BRACKET = "<";
    private static final String TABLE_START_TAG = "<table";
    private static final String TABLE_END_TAG = "</table";
    private static final String PAGE_START_TAG = "<page";
    private static final String PAGE_END_TAG = "</page";
    private static final String LINE_BREAK = "<br>";
    private static final String MANUAL_TABLE_ENDROW = "</td></tr>";
    private static final String MANUAL_TABLE_STARTROW = "<tr><td>";
    private static final String RELEVANT_START = "<h4><b>\n\n### StArT oF rElEvAnT dAtA ###\n\n</b></h4>";
    private static final String RELEVANT_END = "<h4><b>\n\n### EnD oF rElEvAnT dAtA ###\n\n</b></h4>";
    private static final String WHITESPACE_REGEX = "\\s";

    static String cleanHtml(String uncleanHtml) {
        StringBuilder htmlWithBreaks = new StringBuilder(uncleanHtml);
        Scanner scan = new Scanner(uncleanHtml);

        while (scan.hasNextLine()) {
            String currentLine = scan.nextLine();
            if (!currentLine.matches(WHITESPACE_REGEX) && !currentLine.isEmpty() && !(currentLine.contains(START_BRACKET) && currentLine.contains(END_BRACKET))) {

                int lineIndex = uncleanHtml.indexOf(currentLine);

                if (uncleanHtml.toLowerCase().lastIndexOf(TABLE_START_TAG, lineIndex) >
                        uncleanHtml.toLowerCase().lastIndexOf(TABLE_END_TAG, lineIndex)) {
                    htmlWithBreaks.insert(htmlWithBreaks.indexOf(currentLine), MANUAL_TABLE_STARTROW);
                    htmlWithBreaks.insert(htmlWithBreaks.indexOf(currentLine) + currentLine.length(), MANUAL_TABLE_ENDROW);
                }
                else
                    htmlWithBreaks.insert(htmlWithBreaks.indexOf(currentLine) + currentLine.length(), LINE_BREAK);
            }
        }
        scan.close();
        return htmlWithBreaks.toString();
    }

    String insertRelevancyMarkers(String htmlFile) {
        if (htmlFile.length() < NO_DATA_CUTOFF)               // For empty lists
            return htmlFile;

        String lowerCaseHtml = htmlFile.toLowerCase();
        StringBuilder htmlBuilder = new StringBuilder(htmlFile);

        int tableStart = lowerCaseHtml.indexOf(TABLE_START_TAG);              // Check for tables
        int tableEnd = lowerCaseHtml.lastIndexOf(TABLE_END_TAG);
        int pageStart = lowerCaseHtml.indexOf(PAGE_START_TAG);
        int pageEnd = lowerCaseHtml.lastIndexOf(PAGE_END_TAG);

        if (markOpenCloseTags(htmlBuilder, tableStart, tableEnd)) return htmlBuilder.toString(); // Preliminary check for tables and pages
        if (markOpenCloseTags(htmlBuilder, pageStart, pageEnd)) return htmlBuilder.toString();

        return htmlBuilder.toString();
    }

    private static boolean markOpenCloseTags(StringBuilder htmlBuilder, int start, int end) {
        if (start != -1) {
            htmlBuilder.insert(start, RELEVANT_START);
            if (end != -1) {
                htmlBuilder.insert(htmlBuilder.indexOf(END_BRACKET,
                        end + RELEVANT_START.length()) + 1, RELEVANT_END);
            }
            return true;
        }
        return false;
    }
}
