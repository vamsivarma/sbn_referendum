package uniroma1.sbn.finalproject.gunturi.italianreferendum2016.DAO;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class read lines from a CSV
 * @author Vamsi Gunturi
 */
public class CSVReader {
    
    private BufferedReader br;
    private String delimiter;
    private String path;

    /**
     * Initialize the path in which is saved the file and the limiter
     * @param delimiter delimiter to use to parse the CSV file
     * @param path CSV file path
     */
    public CSVReader(String delimiter, String path) {
        this.delimiter = delimiter;
        this.path = path;
    }
    
    /**
     * Read the CSV file and save it in a list of array
     * @return A list of array
     * @throws FileNotFoundException in case the file doesn't exist
     * @throws IOException in case of problems with I/O functions
     */
    public ArrayList<String[]> readCSV() throws FileNotFoundException, IOException{
        // Initialize a new buffered reader
        br = new BufferedReader(new FileReader(path));

        // The line will contain the current row read
        String line;
        // Resulting list
        ArrayList<String[]> rows = new ArrayList<String[]>();
        
        // Remove the first line containing columns label
        br.readLine();
                    
        // Until the end of the file
        while ((line = br.readLine()) != null) {
            // Add the row to the output list, splitted by the delimiter
            rows.add(line.split(delimiter));
        }
        
        // Return the list
        return rows;
    }
}
