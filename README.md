----------------------------------
#### Subsidiary Scraper
### README

----------------------------------
#### Requirements:
* Java Runtime Environment (JRE) Version 9 or greater (can be found at `java.com`)
* To use through command prompt (easiest way), Java must be added to the Windows PATH (this is an option during install, otherwise Google it)
* A version of Windows that is supported by the chosen Java version (Windows 7 or greater is definitely safe)
* A stable network connection for accessing and downloading from provided links

----------------------------------
#### Usage:

1. Locate `input` folder in the same directory as the included JAR and resource files

2. Within this folder, add any number of Excel spreadsheets containing links to SEC EX-21 filings 
(Important: first row ignored)  

EXAMPLE:  
| ID Info (Could be anything) | Link to Filing |  
| -------- | -------- |  
| 1347 PROPERTY INSURANCE HOLDINGS, INC | www.sec.gov/Archives/edgar/data/1591890/000149315218003887/ex21-1.htm |  


3. Once input spreadsheet is in place, double-click the .bat file located in the root JAR directory. This will open the
command prompt and begin the analysis process.
    * If for some reason the batch file does not run, open Command Prompt and run the following command:  
    `java -jar SubsidiaryScraper-1.0-jar-with-dependencies.jar`  

4. Ignore reflection and logger warnings, these are normal and will not impact the operation of the program.

5. Progress will be indicated on the left-hand side of the screen: (#/total)

6. Once complete, results and log file can be found in the newly created `output` directory

_This program will not overwrite results on consecutive executions, so there is no need to clear output directory
after each run unless desired._

----------------------------------
#### Interpreting Results:
Within the `output` directory, one execution produces a single CSV file with the collected data from all provided 
links in the `input` folder. To identify which file out of multiple runs is the desired one, refer to the filename, 
which begins with the date and time of execution. Also created are log files for each run in the `output/logs` folder.  
  
The CSV file is organized per row as such: `{ Firm ID, Subsidiary Name, Location }`  
How this information is picked out of the source material is discussed further down in this document.
  
The analysis process is not perfect, and problems arise from time to time. I recommend checking the log file for any 
given run to identify potential issues with the output data. The issues logged range in severity, but come in two types: 
`FILE_EVENT` and `LINE_EVENT`. Both entry types, when logged, record the firm ID of the affected original filing based on 
the downloaded HTML.  
  
`FILE_EVENT` refers to more severe events that could significantly impact a data set (for example, no data extracted to 
CSV from a filing that was long enough to most likely contain relevant data)  
  
`LINE_EVENT` refers to less severe events that concern specific data pieces extracted into the CSV file. These could 
relate to an output subsidiary name being abnormally long or short, containing strange syntax, etc. and always include 
the original line in question as part of the log entry.

----------------------------------
#### Analysis Process Breakdown:  
##### Stage 1: Downloading and Conversion
* A utility class attempts to download the HTML files from the provided input spreadsheet and temporarily stores them  
locally for use by other classes.
* The HTML files are analyzed sequentially for readability and cleaned before processing if necessary.
* Using libraries from iText, the individual HTML files are converted to temporary text files (very rarely, 
this step fails because of the contents and formatting of the HTML file, not generally an issue).  
* Each text file is read into a parser class that analyzes line-by-line to identify relevant information.

##### Stage 2: Processing and Validation
* The parser class checks each given line for a term found in the `country_list.txt` resource file. This file can be 
modified at will to incorporate unforeseen country-identifying terms by the user.
* If nothing is found, the parser moves to the next line. If a country term is found, the parser identifies the last 
occurrence of said country and tracks its location in the line.
* Once complete, the parser will have identified the last mention of any country term and isolated it from the rest 
of the given line of text.
* Any country abbreviation or alternate name for a country is formalized based on the contents of `country_list.txt`.
* The isolated beginning of the given line is now checked to determine validity as a subsidiary name. Some criteria 
will have a potential subsidiary name thrown out entirely while others will pass it through but record a log entry  
detailing a potential issue with that line.
* One step in the validation process involves checking the contents of the potential subsidiary name against a list  
of disallowed terms located in the `blacklist_terms.txt` resource file. As with the country term list, this text file  
can also be modified by the user to enable or disable certain filtering words.

##### Stage 3: Completion
* Each subsidiary name that passes validation is put through a final cleaning which removes potential non-relevant 
characters from the name.
* The final subsidiary name is stored in a local data structure with its corresponding firm ID and location until 
all entries have been processed.
* At this stage, all recorded data from the internal structure is transferred to the CSV output file.
* Finally, temporary files are deleted from the file system and the program completes execution.

-----------------------------------
#### Included Resources:
* `country_list.txt` (Comprises list of valid countries/regions and their alternate names/abbreviations)
* `blacklist_terms.txt` (Comprises list of terms not allowed to appear in finalized subsidiary names)  
Both of these files can be modified to change filtering parameters of the program.  

----------------------------------
#### Additional Notes:
* Do not rename the JAR file or the resources folder and its contents. This will cause the program to not function as 
it should.
* Do not attempt to run this program using individual classes within an IDE. It will not be able to access 
`input`/`output`/`resources` directories as required by the code. For now, please only run this program as a compiled 
JAR with dependencies.

-----------------------------------

###### _Harry Ross 2019_
