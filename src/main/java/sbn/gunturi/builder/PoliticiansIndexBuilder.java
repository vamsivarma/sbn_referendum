package sbn.gunturi.builder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.SimpleFSDirectory;
import static org.apache.lucene.util.Version.LUCENE_41;
import twitter4j.TwitterException;
import sbn.gunturi.reader.CSVReader;
import sbn.gunturi.manager.TweetsIndexManager;

/**
 * Builder that creates index of politicians
 *
 * @author Vamsi Gunturi
 */
public class PoliticiansIndexBuilder extends IndexBuilder {

    //Politic-Groups and its vote decision
    private HashMap<String, String> groupVote;

    //Document
    private Document politician;
    private StringField vote;
    private StringField name;
    private StringField screenName;
    
    // Map that define which field is going to have which analyzer
    private Map<String, Analyzer> analyzerPerField;

    /**
     * Initialize builder parameters
     *
     * @param sourcePath where the data to create the index are stored
     * @param indexPath where the index will be stored
     */
    public PoliticiansIndexBuilder(String sourcePath, String indexPath) {
        // Initialize the document
        this.politician = new Document();

        // Initialize its fields
        this.name = new StringField("name", "", Field.Store.YES);
        this.screenName = new StringField("screenName", "", Field.Store.YES);
        this.vote = new StringField("vote", "", Field.Store.YES);

        // Add the fields to the document
        this.politician.add(this.name);
        this.politician.add(this.screenName);
        this.politician.add(this.vote);

        // Fill the hashmap of politic group and their decisions
        groupVote = new HashMap<String, String>();
        groupVote.put("AP", "si");
        groupVote.put("DS-CD", "si");
        groupVote.put("FI-PDL", "no");
        groupVote.put("FDI", "no");
        groupVote.put("LEGA", "no");
        groupVote.put("M5S", "no");
        groupVote.put("PD", "si");
        groupVote.put("SCPI", "si");
        groupVote.put("SI-SEL", "no");
        groupVote.put("AL-A", "si");
        groupVote.put("AUT", "si");
        groupVote.put("COR", "no");
        groupVote.put("GAL", "no");
        groupVote.put("MISTO", "no");

        // Set paths
        this.sourcePath = sourcePath;
        this.indexPath = indexPath;
    }

    /**
     * Create the index starting from a csv file of politicians
     * @throws IOException
     * @throws TwitterException
     */
    @Override
    public void build() throws IOException, TwitterException {
        // Read the csv file
        CSVReader csvr = new CSVReader(";", sourcePath);
        // Will contains the csv file rows
        ArrayList<String[]> rows;
        // Set builder params
        setBuilderParams(indexPath);
        // Get the whole CSV rows
        rows = csvr.readCSV();
        
        TweetsIndexManager tim = new TweetsIndexManager("index/AllTweetsIndex");

        // Mantain the id of a user
        String id;
        // Manatain the id and the number of the followers of the user
        String[] result;
        // Mantain the number of followers of that user
        int followers;
        
        int tNotFound = 0;
        int tFound = 0;
        
        // For each politicians in the file read
        for (String[] row : rows) {
            // Get the politician name
            String name = row[0];
            // Get the politician surname
            //String surname = row[1];
            // Get all users that have the same name of the politician
            result =  findUserTwitterId(name, tim);
            // Get the userId
            id =    result[0];  // row[1]; 
            // Get the number of followers
            followers = Integer.parseInt(result[1]);
            
            //System.out.println("Current twitter ID: " + id);
            
            if(id.equals("")) {
                tNotFound++;
                id= row[1];
                followers = 800;
            } else {
                tFound++;
            }
            
            //System.out.println("Politicians Builder index path: " + this.indexPath);
            
            //System.out.println("Search for : " + name + ", followers: " + followers);
            // If a user was found and it has at list a certain value of followers
            if (!id.equals("") && followers >= 800) {
                // Create a new document for the index
                this.name.setStringValue((name).toLowerCase());
                this.screenName.setStringValue(id);

                //System.out.println("");
                //System.out.println(this.politician.get("name"));
                //System.out.println(this.politician.get("screenName"));

                // Define the politician vote
                // If the pol vote is equal to his group vote
                /*if (row[2].equals(groupVote.get(row[3]))) {
                    // Return his vote and write the document
                    this.vote.setStringValue(row[2]);
                    this.writer.addDocument(this.politician);
                    System.out.println(this.politician.get("vote"));
                // If the pol vote is defined in the csv file
                } else if (row[2].equals("si") || row[2].equals("no")) {
                    // Return his vote and write the document
                    this.vote.setStringValue(row[2]);
                    this.writer.addDocument(this.politician);
                    System.out.println(this.politician.get("vote"));
                // If the pol vote is not defined in the csv file
                } else if (row[2].equals("-") && !row[3].equals("MISTO")) {
                    // Return his group vote and write the document
                    this.vote.setStringValue(groupVote.get(row[3]));
                    this.writer.addDocument(this.politician);
                    System.out.println(this.politician.get("vote"));
                }*/
           
                
                this.vote.setStringValue(groupVote.get(row[2]));
                
                //this.vote.setStringValue("si");
                
                //System.out.println(this.politician.get("vote"));
                
                this.writer.addDocument(this.politician);
            }
            
            //System.out.println("Party: " + row[2]);
            //System.out.println("----------------");
            
            this.writer.commit();
           
        }
        
        System.out.println("Twitter IDs not found: " + tNotFound);
        
        System.out.println("Twitter IDs found: " + tFound);
        
        this.writer.close();
    }

    @Override
    public void build(String fieldName, ArrayList<String> fieldValues) throws IOException {
        // Not implemented
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Find all the userId related to the politicians name and surname
     * @param name Politician name
     * @param surname Politician surname
     * @return Values of the best result
     * @throws IOException
     */
    
    public String[] findUserTwitterId(String name, TweetsIndexManager tim) throws IOException {
        // Initialize a new Tweets index builder for the index of the all tweets
        
        String[] name_ary = name.split(" ");
        

        ArrayList<String> fieldValues = new ArrayList<String>();
        // Search user that match name + surname
        fieldValues.add((name_ary[0] + " " + name_ary[1]).toLowerCase());
        // Search user that match surname + name
        fieldValues.add((name_ary[1] + " " + name_ary[0]).toLowerCase());
        
        ArrayList<Document> results = tim.searchForField("name", fieldValues, 10000);
        
        // Variable that will mantain the max number of followers among the users found
        int max = 0;
        // User id related to the max
        String id = "";
        
        // For each document found 
        for (Document doc : results) {
            // check if the user that made it has the more influencer of our actual max
            if (Integer.parseInt(doc.get("followers")) >= max) {
                // And in case take it as new max
                max = Integer.parseInt(doc.get("followers"));
                id = doc.get("screenName");
            }
        }
        // Return the max
        String[] result = {id, new Integer(max).toString()};
        return result;
    }
    
    /**
     * Set builder params
     * @param dirName dir in which inizialize the directory
     * @throws IOException
     */
    @Override
    public void setBuilderParams(String dirName) throws IOException {
        
        this.dir = new SimpleFSDirectory(new File(dirName));
        this.analyzer = new ItalianAnalyzer(LUCENE_41);
        
        // Configure the multianalyzer map
        this.analyzerPerField = new HashMap<String, Analyzer>();
        this.analyzerPerField.put("screenName", new StandardAnalyzer(LUCENE_41));
        
        this.cfg = new IndexWriterConfig(LUCENE_41, analyzer);
        this.writer = new IndexWriter(dir, cfg);
        
        
    }
    
}
