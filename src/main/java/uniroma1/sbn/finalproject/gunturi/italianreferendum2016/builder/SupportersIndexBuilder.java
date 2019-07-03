package uniroma1.sbn.finalproject.gunturi.italianreferendum2016.builder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.ScoreDoc;
import twitter4j.TwitterException;
import uniroma1.sbn.finalproject.gunturi.italianreferendum2016.Entities.Supporter;
import uniroma1.sbn.finalproject.gunturi.italianreferendum2016.Manager.PoliticiansIndexManager;
import uniroma1.sbn.finalproject.gunturi.italianreferendum2016.Manager.SupportersIndexManager;
import uniroma1.sbn.finalproject.gunturi.italianreferendum2016.Manager.TweetsIndexManager;

/**
 * Builder that creates index of supporters
 *
 * @author Vamsi Gunturi
 */
public class SupportersIndexBuilder extends IndexBuilder {

    // List of expressions of yes
    private ArrayList<String> yesExpressions;
    // List of expressions of no
    private ArrayList<String> noExpressions;

    // Document Structure
    private Document supporter;
    private StringField name;
    private StringField id;
    private IntField yesPolsMentioned;
    private IntField noPolsMentioned;
    private IntField yesConstructionsUsed;
    private IntField noConstructionsUsed;
    private IntField yesExpressionsUsed;
    private IntField noExpressionsUsed;
    private IntField isAYesPol;
    private IntField isANoPol;
    private StringField vote;

    /**
     * Initialize Builder params
     *
     * @param indexPath where the index will be stored
     * @param sourcePath where the data to create the index are stored
     * @param yesExpressions List of expressions of yes
     * @param noExpressions List of expressions of no
     */
    public SupportersIndexBuilder(String indexPath, String sourcePath, ArrayList<String> yesExpressions, ArrayList<String> noExpressions) {
        super();

        this.indexPath = indexPath;
        this.yesExpressions = yesExpressions;
        this.noExpressions = noExpressions;
        this.sourcePath = sourcePath;

        // Initialize the document
        this.supporter = new Document();
        // Initialize its fields
        this.name = new StringField("name", "", Field.Store.YES);
        this.id = new StringField("id", "", Field.Store.YES);
        this.yesPolsMentioned = new IntField("yesPolsMentioned", 0, Field.Store.YES);
        this.noPolsMentioned = new IntField("noPolsMentioned", 0, Field.Store.YES);
        this.yesConstructionsUsed = new IntField("yesConstructionsUsed", 0, Field.Store.YES);
        this.noConstructionsUsed = new IntField("noConstructionsUsed", 0, Field.Store.YES);
        this.yesExpressionsUsed = new IntField("yesExpressionsUsed", 0, Field.Store.YES);
        this.noExpressionsUsed = new IntField("noExpressionsUsed", 0, Field.Store.YES);
        this.isAYesPol = new IntField("isAYesPol", 0, Field.Store.YES);
        this.isANoPol = new IntField("isANoPol", 0, Field.Store.YES);
        this.vote = new StringField("vote", "", Field.Store.YES);

        // Add the fields to the document
        this.supporter.add(this.name);
        this.supporter.add(this.id);
        this.supporter.add(this.yesPolsMentioned);
        this.supporter.add(this.noPolsMentioned);
        this.supporter.add(this.yesConstructionsUsed);
        this.supporter.add(this.noConstructionsUsed);
        this.supporter.add(this.yesExpressionsUsed);
        this.supporter.add(this.noExpressionsUsed);
        this.supporter.add(this.isAYesPol);
        this.supporter.add(this.isANoPol);
        this.supporter.add(this.vote);
    }

    @Override
    public void build() throws IOException, TwitterException {
        // Set builder params
        setBuilderParams(indexPath);
        // Get all the supporters
        HashMap<String, Supporter> supporters = collectIndexElements();

        // For each supporter id
        for (String key : supporters.keySet()) {
            // Get the supporter
            Supporter s = supporters.get(key);
            // Fill the fields with the supporter info
            this.name.setStringValue(s.getName());
            this.id.setStringValue(s.getId());
            this.yesPolsMentioned.setIntValue(s.getYesPolsMentioned());
            this.noPolsMentioned.setIntValue(s.getNoPolsMentioned());
            this.yesConstructionsUsed.setIntValue(s.getYesCostructionsUsed());
            this.noConstructionsUsed.setIntValue(s.getNoCostructionsUsed());
            this.yesExpressionsUsed.setIntValue(s.getYesExpressionsUsed());
            this.noExpressionsUsed.setIntValue(s.getNoExpressionsUsed());
            // If the supporter is a yes politician
            if (s.getIsAYesPol()) {
                this.isAYesPol.setIntValue(1);
                this.isANoPol.setIntValue(0);
                this.vote.setStringValue("yes");
                // If the supporter is a no politician
            } else if (s.getIsANoPol()) {
                this.isAYesPol.setIntValue(0);
                this.isANoPol.setIntValue(1);
                this.vote.setStringValue("no");
                // Otherwise
            } else {
                this.isAYesPol.setIntValue(0);
                this.isANoPol.setIntValue(0);
                float finalScore = computeSupporterScore(s);
                if (finalScore > 1.45) {
                    this.vote.setStringValue("yes");
                } else if (finalScore < 0.7) {
                    this.vote.setStringValue("no");
                } else {
                    this.vote.setStringValue("-");
                }
            }

            // Write document
            this.writer.addDocument(this.supporter);
        }
        // Make a commit

        this.writer.commit();
        // Close the writer

        this.writer.close();
    }

    @Override
    public void build(String fieldName, ArrayList<String> fieldValues) throws IOException {
        // Method not implemented
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    // Get all the supporters from other indices
    private HashMap<String, Supporter> collectIndexElements() throws IOException {
        // initialize a TweetIndexManager for the index of all the tweets
        TweetsIndexManager tim = new TweetsIndexManager("index/AllTweetsIndex");
        // initialize a TweetIndexManager for the index of all the politicians
        PoliticiansIndexManager pim = new PoliticiansIndexManager("index/AllPoliticiansIndex");
        // Create an hashmap of supporters
        HashMap<String, Supporter> supporters = new HashMap<String, Supporter>();

        try {
            // Set TweetIndexManager params
            tim.setReader("index/AllTweetsIndex");
            // Get yes pols
            ArrayList<String> yesPols = pim.getFieldValuesList(pim.searchForField("vote", "si", 100000000), "screenName");
            // Get no pols
            ArrayList<String> noPols = pim.getFieldValuesList(pim.searchForField("vote", "no", 100000000), "screenName");

            int addedRecently = 0;

            System.out.println("Adding Politicians:");
            // Get the userId of the yes screenName just obtained
            ScoreDoc[] results = tim.searchTermsInAField(yesPols, "screenName");
            // And create a supporter for each politician
            for (ScoreDoc doc : results) {
                Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                supporter.setIsAYesPol(Boolean.TRUE);
                supporters.put(supporter.getId(), supporter);
            }

            // Do the same for the no politicians
            results = tim.searchTermsInAField(noPols, "screenName");
            for (ScoreDoc doc : results) {
                Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                supporter.setIsANoPol(Boolean.TRUE);
                supporters.put(supporter.getId(), supporter);
            }

            System.out.println("Total supporters: " + supporters.size());
            System.out.println("supporters added: " + (supporters.size() - addedRecently));
            addedRecently = supporters.size();

            System.out.println("Adding for mentioned:");
            // Search the tweets were is mentioned at leat one yes pol
            results = tim.searchTermsInAField(yesPols, "mentioned");
            // For each result
            for (ScoreDoc doc : results) {
                // Get the user id of the tweet
                String userId = tim.ir.document(doc.doc).get("userId");
                // If the supporter is already present in the map of supporters
                if (supporters.containsKey(userId)) {
                    // Add a +1 to the counter of yes pols mentioned
                    Supporter supporter = supporters.get(userId);
                    supporter.setYesPolsMentioned(supporter.getYesPolsMentioned() + 1);
                    // Otherwise create a new supporter 
                } else {
                    Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                    // Set the yes pols mentioned counter to 1
                    supporter.setYesPolsMentioned(1);
                    // And add it to the map
                    supporters.put(supporter.getId(), supporter);
                }
            }
            // Do the same for the no pols mentioned
            results = tim.searchTermsInAField(noPols, "mentioned");
            for (ScoreDoc doc : results) {
                String userId = tim.ir.document(doc.doc).get("userId");
                if (supporters.containsKey(userId)) {
                    Supporter supporter = supporters.get(userId);
                    supporter.setNoPolsMentioned(supporter.getNoPolsMentioned() + 1);
                } else {
                    Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                    supporter.setNoPolsMentioned(1);
                    supporters.put(supporter.getId(), supporter);
                }
            }

            System.out.println("Total supporters: " + supporters.size());
            System.out.println("supporters added: " + (supporters.size() - addedRecently));
            addedRecently = supporters.size();

            // If the structure selected is the relevant word
            if (sourcePath == "output/relWords.json") {
                // Get the rel words from the json and put it into a map
                ObjectMapper mapper = new ObjectMapper();

                HashMap<String, ArrayList<String>> representativeWordsMap;
                representativeWordsMap = mapper.readValue(new File(sourcePath),
                        new TypeReference<HashMap<String, ArrayList<String>>>() {
                });

                // Add them Yes words
                ArrayList<String> yesTerms = representativeWordsMap.get("yes");
                System.out.println("Adding for terms:");
                // Divide words by hashtags
                ArrayList<String> yesWords = new ArrayList<String>();
                ArrayList<String> yesTags = new ArrayList<String>();
                for (String term : yesTerms) {
                    if (term.startsWith("#")) {
                        yesTags.add(term);
                    } else {
                        yesWords.add(term);
                    }
                }
                // Get all the tweets were there is a rel word into
                results = tim.searchTermsInAField(yesWords, "tweetText");
                results = (ScoreDoc[]) ArrayUtils.addAll(results, tim.searchTermsInAField(yesTags, "hashtags"));
                // For each result increment yes construction count or create a new supporter
                for (ScoreDoc doc : results) {
                    String userId = tim.ir.document(doc.doc).get("userId");
                    if (supporters.containsKey(userId)) {
                        Supporter supporter = supporters.get(userId);
                        supporter.setYesCostructionsUsed(supporter.getYesCostructionsUsed() + 1);
                    } else {
                        Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                        supporter.setYesCostructionsUsed(1);
                        supporters.put(supporter.getId(), supporter);
                    }
                }

                // Do the same for no words
                ArrayList<String> noTerms = representativeWordsMap.get("no");
                ArrayList<String> noWords = new ArrayList<String>();
                ArrayList<String> noTags = new ArrayList<String>();
                for (String term : noTerms) {
                    if (term.startsWith("#")) {
                        noTags.add(term);
                    } else {
                        noWords.add(term);
                    }
                }
                results = tim.searchTermsInAField(noWords, "tweetText");
                results = (ScoreDoc[]) ArrayUtils.addAll(results, tim.searchTermsInAField(noTags, "hashtags"));
                for (ScoreDoc doc : results) {
                    String userId = tim.ir.document(doc.doc).get("userId");
                    if (supporters.containsKey(userId)) {
                        Supporter supporter = supporters.get(userId);
                        supporter.setNoCostructionsUsed(supporter.getNoCostructionsUsed() + 1);
                    } else {
                        Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                        supporter.setNoCostructionsUsed(1);
                        supporters.put(supporter.getId(), supporter);
                    }
                }
                // Otherwise, If core or connected components are used as construction
            } else {
                ObjectMapper mapper = new ObjectMapper();
                // Get the rel constructions from the json and put it into a map
                HashMap<String, ArrayList<ArrayList<String>>> representativeWordsMap;
                representativeWordsMap = mapper.readValue(new File(sourcePath),
                        new TypeReference<HashMap<String, ArrayList<ArrayList<String>>>>() {
                });

                // And do the same procedure as before
                ArrayList<ArrayList<String>> yesCores = representativeWordsMap.get("yes");
                System.out.println("Adding for Cores or CC:");
                results = tim.searchORANDCondInAField(yesCores);
                for (ScoreDoc doc : results) {
                    String userId = tim.ir.document(doc.doc).get("userId");
                    if (supporters.containsKey(userId)) {
                        Supporter supporter = supporters.get(userId);
                        supporter.setYesCostructionsUsed(supporter.getYesCostructionsUsed() + 1);
                    } else {
                        Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                        supporter.setYesCostructionsUsed(1);
                        supporters.put(supporter.getId(), supporter);
                    }
                }

                ArrayList<ArrayList<String>> noCores = representativeWordsMap.get("no");
                results = tim.searchORANDCondInAField(noCores);
                for (ScoreDoc doc : results) {
                    String userId = tim.ir.document(doc.doc).get("userId");
                    if (supporters.containsKey(userId)) {
                        Supporter supporter = supporters.get(userId);
                        supporter.setNoCostructionsUsed(supporter.getNoCostructionsUsed() + 1);
                    } else {
                        Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                        supporter.setNoCostructionsUsed(1);
                        supporters.put(supporter.getId(), supporter);
                    }
                }
            }

            System.out.println("Total supporters: " + supporters.size());
            System.out.println("supporters added: " + (supporters.size() - addedRecently));
            addedRecently = supporters.size();

            System.out.println("Adding for Expressions:");
            // At the end add Rel Expression with the same procedure
            results = tim.searchTermsInAField(yesExpressions, "hashtags");
            for (ScoreDoc doc : results) {
                String userId = tim.ir.document(doc.doc).get("userId");
                if (supporters.containsKey(userId)) {
                    Supporter supporter = supporters.get(userId);
                    supporter.setYesExpressionsUsed(supporter.getYesExpressionsUsed() + 1);
                } else {
                    Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                    supporter.setYesExpressionsUsed(1);
                    supporters.put(supporter.getId(), supporter);
                }
            }
            // Do the same for no exp
            results = tim.searchTermsInAField(noExpressions, "hashtags");
            for (ScoreDoc doc : results) {
                String userId = tim.ir.document(doc.doc).get("userId");
                if (supporters.containsKey(userId)) {
                    Supporter supporter = supporters.get(userId);
                    supporter.setNoExpressionsUsed(supporter.getNoExpressionsUsed() + 1);
                } else {
                    Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                    supporter.setNoExpressionsUsed(1);
                    supporters.put(supporter.getId(), supporter);
                }
            }

            System.out.println("Total supporters: " + supporters.size());
            System.out.println("supporters added: " + (supporters.size() - addedRecently));
            addedRecently = supporters.size();
            // Return supporters
            return supporters;

        } catch (IOException ex) {
            Logger.getLogger(SupportersIndexManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
    
    // Compute the vote of a supporter on the basis of who he mentioned and which expression and construction used
    private static float computeSupporterScore(Supporter supporter) {
        float yesScore = (float) (supporter.getYesPolsMentioned() + 0.5 * supporter.getYesCostructionsUsed() + 3 * supporter.getYesExpressionsUsed());

        float noScore = (float) (supporter.getNoPolsMentioned() + 0.5 * supporter.getNoCostructionsUsed() + 3 * supporter.getNoExpressionsUsed());
        // If the sum of the score is at least 8
        if (yesScore + noScore > 8) // return it
        {
            return yesScore / noScore;
        }
        // Otherwise it is not possible to determine his vote
        return 1;
    }
}
