import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScraperMain {

    private static final String INPUT_DIR = "/input";
    private static final String INPUT_HTML_DIR = "/input/html/";

    private static final String OUTPUT_DIR = "/output";
    private static final String OUTPUT_PDF_DIR = "/output/PDFOutput/";
    private static final String OUTPUT_TXT_DIR = "/output/TXTOutput/";

    private static final String HTM_SUFFIX = ".htm";
    private static final String PDF_SUFFIX = ".pdf";
    private static final String TXT_SUFFIX = ".txt";

    private static final String CONVERT_TEXT = "Converting file ";
    private static final String HTML_ERROR = "Error converting HTML: ";
    private static final String PDF_ERROR = "Error reading PDF: ";
    private static final String ERROR_ANALYZING_TEXT_FILE = "Error analyzing text file: ";
    private static final String ANALYSIS_COMPLETE_DISPLAY =
            "//////////////////////////////////////////////////////////////////////////////////\n" +
            "////**** Analysis complete -- Find results and log files in output folder ****////\n" +
            "//////////////////////////////////////////////////////////////////////////////////";
    private static final String DONE = "Done";
    private static final String CREATING_CSV = "Creating output CSV file... ";

    public static void main(String[] args) throws IOException {
        int fileCount = 1;
        String runningPath = Utility.getRunningPath();

        File[] inputSheets = new File(runningPath + INPUT_DIR).listFiles();

        if (inputSheets != null) {
            List<String> originalLinks = new ArrayList<>();
            Map<File, String> htmlFileMap = Utility.getFilesFromSheet(inputSheets, originalLinks);    // This should be the retrieved html files
            Utility.createOutputDirectories(OUTPUT_DIR, OUTPUT_PDF_DIR, OUTPUT_TXT_DIR, INPUT_HTML_DIR);
            Collections.sort(originalLinks);

            for (File htmlFile : htmlFileMap.keySet()) {       // Cycle through files in resources
                System.out.println(CONVERT_TEXT + "(" + fileCount + "/" + htmlFileMap.keySet().size() + "): " + htmlFile.getPath());

                String cleanHtml = HtmlParser.cleanHtml(new String(Files.readAllBytes(Paths.get(htmlFile.getPath()))));  // Clean HTML files

                File pdfOutputFile = htmlToPdf(htmlFile, cleanHtml, runningPath);
                pdfToText(pdfOutputFile, htmlFile, runningPath);  // Convert new PDF to text file
                fileCount++;
            }
            textToCsv(runningPath, originalLinks);
            removeTempFiles();
            System.out.println(ANALYSIS_COMPLETE_DISPLAY);
        }
    }

    private static File htmlToPdf(File htmlFile, String cleanHtml, String runningPath) {
        File pdfOutputFile = new File(runningPath + OUTPUT_PDF_DIR +
                htmlFile.getName().replace(HTM_SUFFIX, PDF_SUFFIX));  // Create empty output PDF file
        try {
            HtmlConverter.convertToPdf(cleanHtml, new FileOutputStream(pdfOutputFile)); // Create PDF
        } catch (NullPointerException | IOException e) {
            System.out.println(HTML_ERROR + htmlFile.getName());
        }
        return pdfOutputFile;
    }

    static private void pdfToText(File inputPdf, File inputHtml, String runningPath) {   // Convert PDF to text
        try {
            PdfReader reader = new PdfReader(inputPdf);
            PdfDocument doc = new PdfDocument(reader);

            StringBuilder completeTextFromPage = new StringBuilder();
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                PdfPage currPage = doc.getPage(i);
                completeTextFromPage.append(PdfTextExtractor.getTextFromPage(currPage));
            }
            reader.close();

            PrintWriter out = new PrintWriter(runningPath + OUTPUT_TXT_DIR +
                    inputHtml.getName().replace(HTM_SUFFIX, TXT_SUFFIX));
            out.println(completeTextFromPage);
            out.close();
        } catch (Exception e) {
            System.out.println(PDF_ERROR + inputPdf.getName());
        }
    }

    private static void textToCsv(String runningPath, List<String> originalLinks) {
        File[] inputFiles = new File(runningPath + OUTPUT_TXT_DIR).listFiles();
        List<File> files = Utility.getFiles(inputFiles);
        TextParser parser = new TextParser();

        for (File textFile : files) {
            try {
                System.out.println("Analyzing text (" + (files.indexOf(textFile) + 1) + "/" + files.size() + "): "
                        + textFile.getName());
                String currLink = "";
                if (originalLinks.size() > files.indexOf(textFile)) {
                    currLink = originalLinks.get(files.indexOf(textFile));
                }
                parser.analyzeTextFile(textFile, currLink);
            } catch (IOException e) {
                System.out.println(ERROR_ANALYZING_TEXT_FILE + textFile.getName());
            }
        }
        System.out.print(CREATING_CSV);
        parser.createCsv();
        System.out.println(DONE);
    }

    private static void removeTempFiles() {
        Utility.deleteDirectories(OUTPUT_TXT_DIR, OUTPUT_PDF_DIR, INPUT_HTML_DIR);
    }

}
