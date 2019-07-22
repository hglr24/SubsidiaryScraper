import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class Utility {

    private static final String HTTPS_PREFIX = "https://";
    private static final String HTTP_GENERIC_PREFIX = "http";
    private static final String HTML_DIRECTORY = "/input/html/";

    private static final String NOT_ENOUGH_DATA = "Not enough input data at row ";
    private static final String SHEET_ERROR = "Error processing input spreadsheet: ";
    private static final String ERROR_CREATING_OUTPUT_DIRECTORY = "Error creating output directory: ";
    private static final String JAR_PATH_ERROR = "Error retrieving running path of JAR file";
    private static final String EMPTY_INPUT_SHEET = "Empty input sheet: ";
    private static final String FILE_DOWNLOAD_ERROR = "Error downloading file: ";
    private static final String DIRECTORY_DEL_ERROR = "Error deleting file/directory (might be in use): ";
    private static final String DONE = "Done";
    private static final String DOWNLOADING_FILES = "Downloading files from input spreadsheet... ";

    static List<File> getFiles(File[] inputFiles) {   // Retrieves files from directory
        List<File> fileList = new ArrayList<>();
        if (inputFiles != null) {
            for (File f : inputFiles) {
                if (f.isDirectory())
                    fileList.addAll(getFiles(f.listFiles()));
                else
                    fileList.add(f);
            }
        }
        return fileList;
    }

    static void createOutputDirectories(String... directories) {
        for (String dir : directories) {
            if (!Files.exists(Paths.get(Utility.getRunningPath() + dir)))
                try {
                    Files.createDirectory(Paths.get(Utility.getRunningPath() + dir));
                } catch (IOException e) {
                    System.out.println(ERROR_CREATING_OUTPUT_DIRECTORY + dir);
                }
        }
    }

    static String getRunningPath() {
        String runningPath = "";
        try {
            runningPath = new File(Utility.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getParentFile().getPath();
        } catch (URISyntaxException e) {
            System.out.println(JAR_PATH_ERROR);
        }
        return runningPath;
    }

    static Map<File, String> getFilesFromSheet(File[] inputFiles) {
        Map<File, String> rtnMap = new TreeMap<>();

        for (File file : inputFiles) {
            if (!file.isDirectory()) {
                try {
                    FileInputStream stream = new FileInputStream(file);

                    XSSFWorkbook workbook = new XSSFWorkbook(stream);
                    XSSFSheet sheet = workbook.getSheetAt(0);
                    Iterator<Row> rowIterator = sheet.iterator();

                    if (!rowIterator.hasNext()) {
                        System.out.println(EMPTY_INPUT_SHEET + file.getName());
                        return new TreeMap<>();
                    }

                    downloadFile(rtnMap, rowIterator);
                    workbook.close();
                    stream.close();

                } catch (IOException e) {
                    System.out.println(SHEET_ERROR + file.getName());
                }
            }
        }
        return rtnMap;
    }

    private static void downloadFile(Map<File, String> rtnMap, Iterator<Row> rowIterator) {
        System.out.print(DOWNLOADING_FILES);
        while (rowIterator.hasNext()) {
            Row currentRow = rowIterator.next();

            if (currentRow.getRowNum() > 0) {
                if (currentRow.getPhysicalNumberOfCells() >= 2) {
                    String firmId = currentRow.getCell(0).getStringCellValue();
                    String exURL = currentRow.getCell(1).getStringCellValue();
                    if (!exURL.startsWith(HTTP_GENERIC_PREFIX)) {
                        exURL = HTTPS_PREFIX + exURL;
                    }

                    try {
                        File downloadedHtml = new File(getRunningPath() + HTML_DIRECTORY + firmId);
                        FileUtils.copyURLToFile(new URL(exURL), downloadedHtml);
                        rtnMap.put(downloadedHtml, firmId);
                    } catch (IOException e) {
                        System.out.println(FILE_DOWNLOAD_ERROR + firmId);
                    }
                } else
                    System.out.println(NOT_ENOUGH_DATA + currentRow.getRowNum());
            }
        }
        System.out.println(DONE);
    }

    static void deleteDirectories(String... directories) {
        for (String dir : directories) {
            File[] files = new File(Utility.getRunningPath() + dir).listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectories(f.getPath());
                    } else {
                        try {
                            Files.deleteIfExists(Paths.get(f.getPath()));
                        } catch (IOException e) {
                            System.out.println(DIRECTORY_DEL_ERROR + dir);
                        }
                    }
                }
            }
            try {
                Files.deleteIfExists(Paths.get(Utility.getRunningPath() + dir));
            } catch (IOException e) {
                System.out.println(DIRECTORY_DEL_ERROR + dir);
            }
        }
    }
}
