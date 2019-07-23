package sbn.gunturi.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import twitter4j.TwitterException;
import sbn.gunturi.builder.PoliticiansIndexBuilder;

/**
 * This class manage the creation and the comunication with indices of politicians
 * @author Vamsi Gunturi
 */
public class PoliticiansIndexManager extends IndexManager{

    /**
     * Inizialize the path in which the index is or is going to be put
     * @param indexPath
     */
    public PoliticiansIndexManager(String indexPath) throws IOException{
        super(indexPath);
        //this.setReader(this.indexPath);
    }
    
    @Override
    public void create(String sourcePath) {
        System.out.println("Politicians Index Creation!");
        // inizialize a new politician indec builder
        PoliticiansIndexBuilder tib = new PoliticiansIndexBuilder(sourcePath, indexPath);      
        try {
            tib.build();
        } catch (IOException ex) {
            System.out.println("---> Problems with source files: IOException <---");
            ex.printStackTrace();
        } catch (TwitterException ex) {
            Logger.getLogger(PoliticiansIndexManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void create(String sourcePath, String fieldName, ArrayList<String> fieldValues){
        // Not implemented version
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
