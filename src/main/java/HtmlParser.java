import java.util.Scanner;

class HtmlParser {

    private static final String END_BRACKET = ">";
    private static final String START_BRACKET = "<";
    private static final String TABLE_START_TAG = "<table";
    private static final String TABLE_END_TAG = "</table";
    private static final String LINE_BREAK = "<br>";
    private static final String MANUAL_TABLE_ENDROW = "</td></tr>";
    private static final String MANUAL_TABLE_STARTROW = "<tr><td>";
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
}
