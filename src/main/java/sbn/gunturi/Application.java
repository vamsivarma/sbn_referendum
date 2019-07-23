package sbn.gunturi;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.stilo.g.algo.ConnectedComponents;
import it.stilo.g.algo.HubnessAuthority;
import it.stilo.g.algo.KppNeg;
import it.stilo.g.algo.SubGraph;
import it.stilo.g.structures.DoubleValues;
import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.util.NodesMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import sbn.gunturi.algorithms.ComunityLPA;
import sbn.gunturi.entities.TweetTerm;
import sbn.gunturi.entities.ClusterGraph;
import sbn.gunturi.manager.PoliticiansIndexManager;
import sbn.gunturi.manager.TweetsIndexManager;
import sbn.gunturi.factory.ClusterGraphFactory;
import static sbn.gunturi.manager.IndexManager.searcher;
import sbn.gunturi.manager.SupportersIndexManager;

/**
 * Bootstrap point for the application 
 * @author Vamsi Gunturi
 */
public class Application {

    /**
     * Main method from which the whole analysis starts and is divided in to 
     * three parts: 
     * 1) Temporal Analysis 
     * 2) Identify mentions of candidates or YES/NO supporter 
     * 3) Spread of Influence
     * @param args
     */
    public static void main(String[] args) throws IOException {
        
        temporalAnalysis();
        
        if (!Files.exists(Paths.get("output/relWords.json"))
                || !Files.exists(Paths.get("output/relComps.json"))
                || !Files.exists(Paths.get("output/relCores.json"))) {
            //temporalAnalysis();
        }
        
                
        if (!Files.exists(Paths.get("output/yesAuthorities.txt"))
                || !Files.exists(Paths.get("output/noAuthorities.txt"))
                || !Files.exists(Paths.get("output/yesHubs.txt"))
                || !Files.exists(Paths.get("output/noHubs.txt"))
                || !Files.exists(Paths.get("output/yesBrokers.txt"))
                || !Files.exists(Paths.get("output/noBrokers.txt"))) {
            //part1();
        } 
        
        part1();

        part2();
    }

    private static void temporalAnalysis() throws IOException {
        //Generate the indices needed in firt task of part 0
        indexCreation();
        
        System.out.println("Index Created");
        // Create a TweetsIndexManager for yes tweets
        TweetsIndexManager yesTim = new TweetsIndexManager("index/AllYesTweetsIndex");
        // Create a TweetsIndexManager for no tweets
        TweetsIndexManager noTim = new TweetsIndexManager("index/AllNoTweetsIndex");

        // Define a time interval for SAX procedure (12h)
        long timeInterval = 43200000L;
        
        // @TODO: Need to double check the validity of this regular expression
        // Define the regex to be match that shows a pattern of collective attention
        String regex = "a+b+a*b*a*";
        // Relevant field in which search relevant words
        String[] fieldNames = {"tweetText", "hashtags"};
        // Get Yes and No relevant words
        ArrayList<TweetTerm> yesList = yesTim.getRelFieldTerms(fieldNames, regex, timeInterval);
        ArrayList<TweetTerm> noList = noTim.getRelFieldTerms(fieldNames, regex, timeInterval);
        
        HashMap<String, Integer> yesW = new HashMap<String, Integer>();
        HashMap<String, Integer> noW = new HashMap<String, Integer>();
        
        
        //System.out.println("Yes list length: " + yesList.size());
        //System.out.println("No list length: " + noList.size());
        
        int termCounter = 0;
        
        System.out.println("Top 10 Yes List");
        
        for (TweetTerm yesT: yesList) {
            
            
            
            yesW.put(yesT.getWord(), (int) yesT.getFrequency());
            
            termCounter++;
            
            if(termCounter <= 10) {
                
                System.out.println("---------------------------------------");      
                System.out.println("Term: " + yesT.getWord());
                System.out.println("Date: " + yesT.getSaxRep());
                //System.out.println("Frequency: " + yesT.getFrequency());
                System.out.println("---------------------------------------");
            
            }
               
        }
        
        
        System.out.println("Top 10 No List");
        
        termCounter = 0;
        
        for (TweetTerm noT: noList) {
            
            noW.put(noT.getWord(), (int) noT.getFrequency());
            
            termCounter++;
            
            if(termCounter <= 10) {
                System.out.println("---------------------------------------");      
                System.out.println("Term: " + noT.getWord());
                //System.out.println("Date: " + yesT.getType());
                System.out.println("Frequency: " + noT.getFrequency());
                System.out.println("---------------------------------------");
            }
            
        }
        
        
        // @TODO: Need to assign these 2 dynamically from Elbow method

        // N° of cluster in witch divide the words found
        int nCluster = 10;
        // max number of iteration for k-means
        int nIter = 1000;

        // Initialize a ClusterGraphFactory in order to create graphs generated by k-means
        // Just created, these graph compute their cc and cores storing them in attributes.
        ClusterGraphFactory cgf = new ClusterGraphFactory(nCluster, nIter);

        // Yes graphs generated by the factory
        ArrayList<ClusterGraph> yesGraphs = cgf.generate(yesList, yesTim);
        // No graphs generated by the factory
        ArrayList<ClusterGraph> noGraphs = cgf.generate(noList, noTim);

        // Set the time interval to 3 hours
        //timeInterval = 3600000L;

        // List of all the words of yes and no
        ArrayList<String> ccYesWordsList = new ArrayList<String>();
        ArrayList<String> ccNoWordsList = new ArrayList<String>();
        
        ArrayList<String> coreYesWordsList = new ArrayList<String>();
        ArrayList<String> coreNoWordsList = new ArrayList<String>();

        // Initialize a map in which put all relevant words for keys "yes" and "no".
        HashMap<String, ArrayList<String>> ccWords = new HashMap<String, ArrayList<String>>();
        
        HashMap<String, ArrayList<String>> coreWords = new HashMap<String, ArrayList<String>>();
        // Initialize a map in which put all relevant componens for keys "yes" and "no". Each key has a list of 10 elements as value
        HashMap<String, ArrayList<ArrayList<String>>> relComps = new HashMap<String, ArrayList<ArrayList<String>>>();
        // Initialize a map in which put all relevant Cores for keys "yes" and "no". Each key has a list of 10 elements as value
        HashMap<String, ArrayList<ArrayList<String>>> relCores = new HashMap<String, ArrayList<ArrayList<String>>>();

        // Initialize values for the two maps already created
        relComps.put("yes", new ArrayList<ArrayList<String>>());
        relComps.put("no", new ArrayList<ArrayList<String>>());
        relCores.put("yes", new ArrayList<ArrayList<String>>());
        relCores.put("no", new ArrayList<ArrayList<String>>());

        // For each yes cluster...
        for (ClusterGraph cg : yesGraphs) {

            // ...Get the core elements and save them in coreList
            int[] core = cg.getCore().seq;
            ArrayList<Integer> coreList = new ArrayList<Integer>();
            for (int k = 0; k < core.length; k++) {
                coreList.add(core[k]);
            }

            // Get all the labels of the words in the core and save them in relCores
            relCores.get("yes").add(cg.getWords(coreList));
            
            
            for (String word : cg.getWords(coreList)) {
               coreYesWordsList.add(word);
            }
            
            

            // Get cluster comps
            Set<Set<Integer>> comps = cg.getComps();
            // For each comp
            for (Set<Integer> comp : comps) {
                // Get all the elements of the comp
                ArrayList<Integer> compElems = new ArrayList<Integer>();
                for (int elem : comp) {
                    compElems.add(elem);
                }

                // Get all the labels of the words in the comp elements and save them in relComps
                relComps.get("yes").add(cg.getWords(compElems));

                // Add all the words found in the list of the yes words
                for (String word : cg.getWords(compElems)) {
                    ccYesWordsList.add(word);
                }
            }
        }

        // same for no graphs
        for (ClusterGraph cg : noGraphs) {
            int[] core = cg.getCore().seq;
            ArrayList<Integer> coreList = new ArrayList<Integer>();
            for (int k = 0; k < core.length; k++) {
                coreList.add(core[k]);
            }

            relCores.get("no").add(cg.getWords(coreList));
            
            for (String word : cg.getWords(coreList)) {
               coreNoWordsList.add(word);
            }

            Set<Set<Integer>> comps = cg.getComps();
            ArrayList<Integer> compElems = new ArrayList<Integer>();
            for (Set<Integer> comp : comps) {
                compElems = new ArrayList<Integer>();
                for (int elem : comp) {
                    compElems.add(elem);
//                  String nodeName = cg.nodeMapper.getNode(elem);
//                    if (nodeName.startsWith("#")) {
//                        System.out.println(nodeName + ": " + Arrays.toString(noTim.getTermTimeSeries(nodeName, "hashtags", timeInterval)));
//                    } else {
//                        System.out.println(nodeName + ": " + Arrays.toString(noTim.getTermTimeSeries(nodeName, "tweetText", timeInterval)));
//                    }
                }
                
                relComps.get("no").add(cg.getWords(compElems));
                
                
                
                // @TODO: Need to check if this is the right approach

                // For each no word found
                /*for (String word : cg.getWords(compElems)) {
                    // If a word is already in the list of yes words
                    if (representativeYesWordsList.contains(word)) {
                        // Remove it from the Yes list and skip the adding
                        int flag = representativeYesWordsList.indexOf(word);
                        representativeYesWordsList.remove(flag);
                    } else {
                        // add it to the no list
                        representativeNoWordsList.add(word);
                    }
                }*/
                
                
                // Add all the words found in the list of the no words
                for (String word : cg.getWords(compElems)) {
                    ccNoWordsList.add(word);
                }
            }
        }

        // Add Words to the words map
        ccWords.put("yes", ccYesWordsList);
        ccWords.put("no", ccNoWordsList);
        
        coreWords.put("yes", coreYesWordsList);
        coreWords.put("no", coreNoWordsList);

        // Save maps obtained in json
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(new File("output/ccWords.json"), ccWords);
            mapper.writeValue(new File("output/coreWords.json"), coreWords);
            mapper.writeValue(new File("output/relComps.json"), relComps);
            mapper.writeValue(new File("output/relCores.json"), relCores);
            mapper.writeValue(new File("output/yesWords.json"), yesW);
            mapper.writeValue(new File("output/noWords.json"), noW);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void indexCreation() throws IOException {
        
        // Initialize a TweetsIndexManager for the index of all tweets
        TweetsIndexManager tim = new TweetsIndexManager("index/AllTweetsIndex");

        // If the index of all tweets doesn't exist
        Path dir = Paths.get("index/AllTweetsIndex");
        
        if (!Files.exists(dir)) {
            // Create it
            tim.create("input/stream");
            //tim.create("input/basic");
        } else {
            // Advise the index already exist
            System.out.println(dir.toString() + ": Index already created!");
        }
        
        int totalTweetSize = tim.getIndexSizes();
        
        System.out.println("TOTAL TWEETS INDEXED: " + totalTweetSize);
        
        

        // Initialize a PoliticiansIndexManager for the index of all Politicians found
        PoliticiansIndexManager pim = new PoliticiansIndexManager("index/AllPoliticiansIndex");
        
        // If the index of all politicians doesn't exist
        dir = Paths.get("index/AllPoliticiansIndex");
        
        if (!Files.exists(dir)) {
            // Create it
            pim.create("input/all_politicians.csv");
        } else {
            // Advise the index already exist
            System.out.println(dir.toString() + ": Index already created!");
        }
        
        
        // Divide politicians in YES and NO
        ArrayList<Document> yesPoliticians = pim.searchForField("vote", "si", 1000);
        ArrayList<Document> noPoliticians = pim.searchForField("vote", "no", 1000);

        // Show how many politicians we got
        if (yesPoliticians != null && noPoliticians != null) {
            System.out.println("YES POLITICIANS: " + yesPoliticians.size());
            System.out.println("NO POLITICIANS: " + noPoliticians.size());
        }
        
        
       // ArrayList<Document> nameP = tim.searchForField("screenName", "giotoni", 5);
        //System.out.println("P size: " + nameP);
        
        /*for (Document doc: nameP){
           
            System.out.println("---------------------------------------");
                  
            System.out.println("User ID: " + doc.get("userId"));
            System.out.println("Date: " + doc.get("date"));
            System.out.println("Name: " + doc.get("name"));
            System.out.println("Screen Name: " + doc.get("screenName"));
            System.out.println("Tweet text: " + doc.get("tweetText"));
            System.out.println("Hashtags: " + doc.get("hashtags"));
            System.out.println("Mentioned: " + doc.get("mentioned"));
            System.out.println("Followers: " + doc.get("followers"));


            System.out.println("---------------------------------------"); 
        }*/
        
        
        /*yesPoliticians = pim.searchForField("vote", "si", 1000);
        noPoliticians = pim.searchForField("vote", "no", 1000);

        // Show how many politicians we got
        if (yesPoliticians != null && noPoliticians != null) {
            System.out.println("YES POLITICIANS: " + yesPoliticians.size());
            System.out.println("NO POLITICIANS: " + noPoliticians.size());
        }*/
        
        
        

        // Initialize a TweetsIndexManager for the index of all yes tweets based on yes pols
        TweetsIndexManager yesTim = new TweetsIndexManager("index/AllYesTweetsIndex");

        // If the index of all yes tweets doesn't exist
        dir = Paths.get("index/AllYesTweetsIndex");
        if (!Files.exists(dir)) {
            // Create it collecting all the yes ploticians screen name
            ArrayList<String> yesScreenNames = pim.searchFilteredValueField("vote", "si", "screenName", 10000);
            System.out.println("YES Screen names count: " + yesScreenNames.size());
            
            yesTim.create("index/AllTweetsIndex", "", yesScreenNames);
        } else {
            // Advise the index already exist
            System.out.println(dir.toString() + ": Index already created!");
        }

        // Initialize a TweetsIndexManager for the index of all no tweets based on no pols
        TweetsIndexManager noTim = new TweetsIndexManager("index/AllNoTweetsIndex");

        // If the index of all no tweets doesn't exist
        dir = Paths.get("index/AllNoTweetsIndex");
        if (!Files.exists(dir)) {
            // Create it collecting all the no ploticians screen name
            ArrayList<String> noScreenNames = pim.searchFilteredValueField("vote", "no", "screenName", 10000);
            noTim.create("index/AllTweetsIndex", "", noScreenNames);
        } else {
            // Advise the index already exist
            System.out.println(dir.toString() + ": Index already created!");
        }

        // Get all tweets of interest(YES and NO tweets related to our pols)
        int yesSize = yesTim.getIndexSizes();
        int noSize = noTim.getIndexSizes();

        // And print the sizes
        System.out.println("");
        System.out.println("YES TWEETS: " + yesSize);
        System.out.println("NO TWEETS: " + noSize);
        System.out.println("TOTAL TWEETS: " + (yesSize + noSize));
        
        
        /* Start - Getting the timestamps of both YES and NO tweets */
        
        ArrayList<Document> allYes = yesTim.getAllDocs();
        System.out.println("YES TWEETS COUNT: " + allYes.size());
        
        
        // Generating YES tweet timestamps in to a single CSV file
        /*try (PrintWriter writer = new PrintWriter(new File("yes_tt.csv"))) {
             StringBuilder sb = new StringBuilder();
             
             for (Document doc: allYes){

                    //System.out.println("---------------------------------------");

                    //System.out.println("Hashtags: " + doc.get("hashtags"));
                    
                    //System.out.println("Mentioned: " + doc.get("mentioned"));
                    
                    //System.out.println("Mentioned: " + doc.get("followers"));
                    
                     sb.append(doc.get("date"));
                    
                     sb.append('\n');

                    //System.out.println("---------------------------------------"); 

             }
             
             writer.write(sb.toString());
             
             System.out.println("done!");
     

        } catch (FileNotFoundException e) {
              System.out.println(e.getMessage());
        }*/
        
        ArrayList<Document> allNo = noTim.getAllDocs();
        System.out.println("NO TWEETS COUNT: " + allNo.size());
        
        
        // Generating YES tweet timestamps in to a single CSV file
        /*try (PrintWriter writer = new PrintWriter(new File("no_tt.csv"))) {
             StringBuilder sb = new StringBuilder();
             
             for (Document doc: allNo){

                    //System.out.println("---------------------------------------");

                    //System.out.println("Date: " + doc.get("date"));
                    
                     sb.append(doc.get("date"));
                    
                     sb.append('\n');

                    //System.out.println("---------------------------------------"); 

             }
             
             writer.write(sb.toString());
             
             System.out.println("done!");
     

        } catch (FileNotFoundException e) {
              System.out.println(e.getMessage());
        }*/
        
        /* End: Getting the timestamps of both YES and NO tweets */
    }

    private static void part1() {

        try {

            // Create lists of yes and no expressionas
            ArrayList<String> yesExp = new ArrayList<String>();
            ArrayList<String> noExp = new ArrayList<String>();

            yesExp.add("#iovotosi");
            yesExp.add("#iovotosì");
            yesExp.add("#iodicosi");
            yesExp.add("#iodicosì");
            yesExp.add("#iohovotatosi");
            yesExp.add("#iohovotatosì");
            yesExp.add("#votasi");
            yesExp.add("#votosi");
            yesExp.add("#votosì");
            yesExp.add("#bastaunsi");
            yesExp.add("#bufaledelno");
            yesExp.add("#bufaladelno");
            yesExp.add("#si");
            yesExp.add("#sì");

            noExp.add("#iovotono");
            noExp.add("#iodicono");
            noExp.add("#iohovotatono");
            noExp.add("#votano");
            noExp.add("#votono");
            noExp.add("#bastaunno");
            noExp.add("#bufaledelsi");
            noExp.add("#bufaledelsì");
            noExp.add("#no");
            noExp.add("#noivotiamono");
            noExp.add("#ragionidelno");
            noExp.add("#unitixilno");
            noExp.add("#votiamono");
            
            
            // Initialize a TweetsIndexManager for the index of all yes tweets based on yes pols
            /*TweetsIndexManager yesTim = new TweetsIndexManager("index/AllYesTweetsIndex");
            
            
            ArrayList<Document> allYes = yesTim.getAllDocs();
            
            System.out.println("YES TWEETS COUNT: " + allYes.size());
        
        
        
             
             //for (Document doc: allYes){

                    //System.out.println("---------------------------------------");

                    //System.out.println("Hashtags: " + doc.get("hashtags"));
                    
                    //System.out.println("Mentioned: " + doc.get("mentioned"));
                    
                    //System.out.println("Mentioned: " + doc.get("followers"));

                    //System.out.println("---------------------------------------"); 

             //}*/
             
            

            // Initialize a SupportersIndexManager
            SupportersIndexManager sim = new SupportersIndexManager("index/SupportersIndex", yesExp, noExp);
            // If the index doesn't exist yet
            Path dir = Paths.get("index/SupportersIndex");
            if (!Files.exists(dir)) {
                // Create it
                // @TODO: Identify difference in results from CC and Cores
                sim.create("output/relCores.json");
            } else {
                System.out.println(dir.toString() + ": Index already created!");
            }
            
            //sim.getAllSupportersTweets();
            
            // Get all supporters ids and save them in a list
            ArrayList<String> nodes = sim.getFieldValuesList(sim.getAllDocs(), "id");
            TweetsIndexManager tim = new TweetsIndexManager("index/AllTweetsIndex");
            
            System.out.println("Total supporters: " + nodes.size());
            
            /*HashSet noDupSet = new HashSet();
            
            for(String node: nodes) {
                noDupSet.add(node);            
            }
            
            System.out.println("Number of unique supporters: " + noDupSet.size());*/
            
            boolean supportersExist = true;
            long noSupporterTweets = 0;
            
            int sIndex = 0;
            
            /*
            while(supportersExist) {
                
                System.out.println(sIndex);
                
                int endIndex = sIndex + 1000;
                
                if(endIndex > nodes.size() - 1) {
                    endIndex = nodes.size() - 1;
                    supportersExist = false;
                }
                
                ArrayList<String> subSList = new ArrayList(nodes.subList(sIndex, endIndex));
                
                //ScoreDoc[] sdst =  tim.searchTermsInAField(subSList, "userId");
                
                ArrayList<Document> sdst = tim.searchForField("userId", subSList, 10000);
                
                System.out.println("Found " + sdst.size() + " Tweets");
                
                //for(ScoreDoc d: sdst) {
                 noSupporterTweets += sdst.size();
                //}
                
                sIndex = endIndex;
                
                
            } 
             
            System.out.println("+++ Total Number of Supporters Tweets: " + noSupporterTweets);
            */
            
            System.out.println("Total supporters: " + nodes.size());
            
            // Set number of workers
            int worker = (int) (Runtime.getRuntime().availableProcessors());

            // Connected Component SubGraph
            WeightedDirectedGraph ccsg;
            // A mapper for node label and id
            NodesMapper<String> nodeMapper = new NodesMapper<String>();

            dir = Paths.get("output/ccsg.txt");
            // If the list of the edges of ccsg doesn't exist
            if (!Files.exists(dir)) {
                // Create it
                createCCSG(nodes);
            } 

            // Build the graph
            FileReader fr = new FileReader("output/ccsg.txt");
            BufferedReader br = new BufferedReader(fr);

            String line;
            // Getting the graph size
//            HashSet<Integer> nodeIds = new HashSet<Integer>();
//            while ((line = br.readLine()) != null) {
//                String[] splittedLine = line.split(" ");
//                nodeIds.add(nodeMapper.getId(splittedLine[0]));
//                nodeIds.add(nodeMapper.getId(splittedLine[1]));
//            }
//            System.out.println(nodeIds.size()); // 46296

            ccsg = new WeightedDirectedGraph(46296 + 1);
            nodeMapper = new NodesMapper<String>();
            // Creating the graph
            while ((line = br.readLine()) != null) {
                String[] splittedLine = line.split(" ");
                ccsg.add(nodeMapper.getId(splittedLine[1]), nodeMapper.getId(splittedLine[0]), Integer.parseInt(splittedLine[2]));
            }

//            HashMap<String, String> nameMapper = new HashMap<>();
//            TweetsIndexManager tim = new TweetsIndexManager("index/AllTweetsIndex");
//            
//            for(int i = 1; i < ccsg.size; i++){
//                String id = nodeMapper.getNode(i);
//                String name = tim.searchFilteredValueField("userId", id, "name", 1).get(0);
//                nameMapper.put(id, name);
//            }
            br.close();
            fr.close();

            // Supporters            
            HashMap<String, String> yesSup = new HashMap<String, String>();
            HashMap<String, String> noSup = new HashMap<String, String>();
            
            int nfCounter = 0;
            
            //TweetsIndexManager tim = new TweetsIndexManager("index/AllTweetsIndex");

            for(int i = 1; i < ccsg.size; i++){
                //System.out.println("UserId: " + nodeMapper.getNode(i));
                if(nodeMapper.getNode(i) != null) {
                    
                    ArrayList<String> row = new ArrayList<>();
                    
                    ArrayList<Document> fMapper = sim.searchForField("id", nodeMapper.getNode(i), 10);
                    
                    if(!fMapper.isEmpty()) {
                        Document supporter = fMapper.get(0);
                        
                        //System.out.println(supporter.get("vote"));
                    
                        if (supporter.get("vote").equals("yes")) {
                            yesSup.put(supporter.get("id"), supporter.get("name"));
                        } else if (supporter.get("vote").equals("no")) {
                            noSup.put(supporter.get("id"), supporter.get("name"));
                        }
                    } else {
                        nfCounter++;
                    }
                } 
            }
            
            System.out.println("Supporters not found: " + nfCounter);
            
            FileWriter fileWriter = new FileWriter("output/yesSup.txt");
            PrintWriter printWriter = new PrintWriter(fileWriter);
            
            int sCounter = 0;

            for (String supKey : yesSup.keySet()) {
                
                sCounter++;
                
                printWriter.print(supKey + "; "
                        + yesSup.get(supKey) + "; "
                        + nodeMapper.getId(supKey) + "\n");
            }
            printWriter.close();
            
            System.out.println("Number of YES supporters: " + sCounter);
            
            sCounter = 0;
            
            fileWriter = new FileWriter("output/noSup.txt");
            printWriter = new PrintWriter(fileWriter);

            for (String supKey : noSup.keySet()) {
                
                sCounter++;
                
                printWriter.print(supKey + "; "
                        + noSup.get(supKey) + "; "
                        + nodeMapper.getId(supKey) + "\n");
            }
            printWriter.close();
            
            System.out.println("Number of NO supporters: " + sCounter);
            
            // Compute HITS
            ArrayList<ArrayList<DoubleValues>> hitsResult = HubnessAuthority.compute(ccsg, 0.00001, worker);
            // Get authorities
            ArrayList<DoubleValues> authorities = hitsResult.get(0);
            
            System.out.println("No of authorities: " + authorities.size());
            
            // Save authorities
            fileWriter = new FileWriter("output/authorities.txt");
            printWriter = new PrintWriter(fileWriter);

            for (DoubleValues authority : authorities) {
                printWriter.print(authority.index + "; " + authority.value + "\n");
            }
            printWriter.close();
            fileWriter.close();

            HashMap<String, String> yesAuthorities = new HashMap<String, String>();
            HashMap<String, String> noAuthorities = new HashMap<String, String>();
            HashMap<String, String> unclassifiedAuthorities = new HashMap<String, String>();
            // Get the vote of the authorities
            for (int i = 0; i < (9000 < authorities.size() ? 9000 : authorities.size()); i++) {
                
                ArrayList<Document> aMapper = sim.searchForField("id", nodeMapper.getNode(authorities.get(i).index), 10);
                    
                if(!aMapper.isEmpty()) {
                    
                    // get the authority
                    Document supporter = aMapper.get(0);
                    
                    if ((yesAuthorities.size() >= 1000) && (noAuthorities.size() >= 1000)) {
                       break;
                    }
                    
                    if (supporter.get("vote").equals("yes") && yesAuthorities.size() < 1000) {
                        yesAuthorities.put(supporter.get("id"), supporter.get("name"));
                    } else if (supporter.get("vote").equals("no") && noAuthorities.size() < 1000) {
                        noAuthorities.put(supporter.get("id"), supporter.get("name"));
                    } else {
                        unclassifiedAuthorities.put(supporter.get("id"), supporter.get("name"));
                    }
                    
                    
                }
            }

            System.out.println("YES AUTHORITIES: " + yesAuthorities.size());
            System.out.println("NO AUTHORITIES: " + noAuthorities.size());
            System.out.println("UNCLASSIFIED AUTHORITIES: " + unclassifiedAuthorities.size());
            // Save yes authorities
            fileWriter = new FileWriter("output/yesAuthorities.txt");
            printWriter = new PrintWriter(fileWriter);

            for (String authorityKey : yesAuthorities.keySet()) {
                printWriter.print(authorityKey + "; "
                        + yesAuthorities.get(authorityKey) + "; "
                        + nodeMapper.getId(authorityKey) + "\n");
            }
            printWriter.close();
            // Save no authorities
            fileWriter = new FileWriter("output/noAuthorities.txt");
            printWriter = new PrintWriter(fileWriter);

            for (String authorityKey : noAuthorities.keySet()) {
                printWriter.print(authorityKey + "; "
                        + noAuthorities.get(authorityKey) + "; "
                        + nodeMapper.getId(authorityKey) + "\n");
            }
            printWriter.close();
            // Save unclassified authorities
            fileWriter = new FileWriter("output/unclassifiedAuthorities.txt");
            printWriter = new PrintWriter(fileWriter);

            for (String authorityKey : unclassifiedAuthorities.keySet()) {
                printWriter.print(authorityKey + "; "
                        + unclassifiedAuthorities.get(authorityKey) + "; "
                        + nodeMapper.getId(authorityKey) + "\n");
            }
            printWriter.close();
            // Do the same for hubs
            HashMap<String, String> yesHubs = new HashMap<String, String>();
            HashMap<String, String> noHubs = new HashMap<String, String>();

            ArrayList<DoubleValues> hubs = hitsResult.get(1);
            // Save all the hubs
            fileWriter = new FileWriter("output/hubs.txt");
            printWriter = new PrintWriter(fileWriter);

            for (DoubleValues hub : hubs) {
                printWriter.print(hub.index + "; " + hub.value + "\n");
            }
            printWriter.close();

            // Classify each hub
            for (int i = 0; i < hubs.size(); i++) {
                
                //if the max number of authorities is reached stop
                if ((yesHubs.size() >= 1000) && (noHubs.size() >= 1000)) {
                    break;
                }
                
                ArrayList<Document> hMapper = sim.searchForField("id", nodeMapper.getNode(hubs.get(i).index), 10);
                    
                if(!hMapper.isEmpty()) {
                    Document supporter = hMapper.get(0);
                
                    // If it is a politician the vote is clear
                    if (supporter.get("vote").equals("yes") && yesHubs.size() < 1000) {
                        yesHubs.put(supporter.get("id"), supporter.get("name"));
                    } else if (supporter.get("vote").equals("no") && noHubs.size() < 1000) {
                        noHubs.put(supporter.get("id"), supporter.get("name"));
                    }
                    
                    
                }
            }

            fileWriter = new FileWriter("output/yesHubs.txt");
            printWriter = new PrintWriter(fileWriter);
            // Save yes hubs
            for (String hubKey : yesHubs.keySet()) {
                printWriter.print(hubKey + "; " + yesHubs.get(hubKey) + "; " + nodeMapper.getId(hubKey) + "\n");
            }
            printWriter.close();

            fileWriter = new FileWriter("output/noHubs.txt");
            printWriter = new PrintWriter(fileWriter);
            // Save no hubs
            for (String hubKey : noHubs.keySet()) {
                printWriter.print(hubKey + "; " + noHubs.get(hubKey) + "; " + nodeMapper.getId(hubKey) + "\n");
            }
            printWriter.close();

            System.out.println();
            System.out.println("YES HUBS: " + yesHubs.size());
            System.out.println("NO HUBS: " + noHubs.size());

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Save the Connected component subGraph of an initial graph based on our supporters
    private static void createCCSG(ArrayList<String> nodes) throws FileNotFoundException, IOException, InterruptedException {

        // Create a weighted directed graph that will be saved
        WeightedDirectedGraph ccsg;
        // Set number of workers
        int worker = (int) (Runtime.getRuntime().availableProcessors());

        // Relative path to the original graph
        String sourcePath = "input/Official_SBN-ITA-2016-Net.gz";

        // Zip file reader
        FileInputStream fstream = new FileInputStream(sourcePath);
        GZIPInputStream gzstream = new GZIPInputStream(fstream);
        InputStreamReader isr = new InputStreamReader(gzstream, "UTF-8");
        BufferedReader br = new BufferedReader(isr);

        String line;
        // Inizialize a mapper
        NodesMapper<String> nodeMapper = new NodesMapper<String>();

        // To get the number of nodes of the graph
//            HashSet<Integer> nodeIds = new HashSet<Integer>();
//
//            while ((line = br.readLine()) != null) {
//                String[] splittedLine = line.split("\t");
//                nodeIds.add(nodeMapper.getId(splittedLine[0]));
//                nodeIds.add(nodeMapper.getId(splittedLine[1]));
//            }
//            
//            System.out.println(nodeIds.size()); //450193
        //Initial graph
        WeightedDirectedGraph g = new WeightedDirectedGraph(450193 + 1);
        // Populate the graph
        while ((line = br.readLine()) != null) {
            String[] splittedLine = line.split("\t");
            g.add(nodeMapper.getId(splittedLine[0]), nodeMapper.getId(splittedLine[1]), Integer.parseInt(splittedLine[2]));
        }

        br.close();
        isr.close();
        gzstream.close();
        fstream.close();

        // Get all the nodes ids
        int[] ids = new int[nodes.size()];

        int i = 0;
        // For each supporter id
        for (String node : nodes) {
            // If the node id corresponding to its id is in the graph
            if (nodeMapper.getId(node) < 450193) {
                // Add it
                ids[i] = nodeMapper.getId(node);
                i++;
            }
        }
        // Resize the array of supporters id (to remove null pointers in the array).
        ids = Arrays.copyOf(ids, i);

        // Extract the sub graph of the supporters
        WeightedDirectedGraph sg = SubGraph.extract(g, ids, worker);

        // get the connected components
        Set<Set<Integer>> comps = ConnectedComponents.rootedConnectedComponents(sg, ids, worker);

        System.out.println("CONNECTED COMPONENTS DONE");

        // get the one higher on
        int max = 0;
        Set<Integer> maxElem = new HashSet<Integer>();

        for (Set<Integer> comp : comps) {
            if (comp.size() > max) {
                max = comp.size();
                maxElem = comp;
            }
        }

        // get the array version of max cc nodes
        Integer[] maxElemArray = maxElem.toArray(new Integer[maxElem.size()]);

        // Parse the ids in int
        int[] ccids = new int[maxElemArray.length];
        for (i = 0; i < maxElemArray.length; i++) {
            ccids[i] = maxElemArray[i].intValue();
        }

        // Exstract the connected component subgraph
        ccsg = SubGraph.extract(sg, ccids, worker);

        // Save its edges
        FileWriter fileWriter = new FileWriter("output/ccsg.txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);

        for (i = 0; i < ccsg.out.length; i++) {
            if (ccsg.out[i] != null) {
                for (int j = 0; j < ccsg.out[i].length; j++) {
                    printWriter.print(nodeMapper.getNode(i) + " " + nodeMapper.getNode(ccsg.out[i][j]) + " 1\n");
                }
            }
        }
        printWriter.close();
    }

    // compute label propagation in the entire graph using as prelabeled nodes
    // 1) Authorities
    // 2) Hubs
    // 3) Brokers
    private static void part2() {
        // Path of the entire graph
        String sourcePath = "input/Official_SBN-ITA-2016-Net.gz";
        FileInputStream fstream;
        try {
            // Zip file reader
            fstream = new FileInputStream(sourcePath);
            GZIPInputStream gzstream = new GZIPInputStream(fstream);
            InputStreamReader isr = new InputStreamReader(gzstream, "UTF-8");
            BufferedReader br = new BufferedReader(isr);

            String line;
            NodesMapper<String> nodeMapper = new NodesMapper<String>();
            WeightedDirectedGraph g = new WeightedDirectedGraph(450193 + 1);

            // import the entire graph
            while ((line = br.readLine()) != null) {
                String[] splittedLine = line.split("\t");
                g.add(nodeMapper.getId(splittedLine[1]), nodeMapper.getId(splittedLine[0]), Integer.parseInt(splittedLine[2]));
            }

            // Get the number of workers
            int worker = (int) (Runtime.getRuntime().availableProcessors());
            // obtain the initial label by the files of yes and no Supporters
            int[] initLabels = getInitLabel("output/yesSup.txt", "output/noSup.txt", g);
            // Compute LPA for Supporters
            int[] labelsSupporters = ComunityLPA.compute(g, .99d, worker, initLabels);
            int yes = 0, no = 0, unclassified = 0;
            // Count the nodes for each label
            for (int label : labelsSupporters) {
                switch (label) {
                    case 1:
                        yes++;
                        break;
                    case 2:
                        no++;
                        break;
                    default:
                        unclassified++;
                        break;
                }
            }
            System.out.println("+++ SUPPORTERS (M) +++");
            System.out.println("YES: " + yes + ", NO: " + no + ", UNCLASSIFIED: " + unclassified);

            // obtain the initial label by the files of yes and no hubs
            initLabels = getInitLabel("output/yesHubs.txt", "output/noHubs.txt", g);
            // Compute LPA for hubs
            int[] labelsHubs = ComunityLPA.compute(g, .99d, worker, initLabels);
            // Count the nodes for each label
            yes = 0;
            no = 0;
            unclassified = 0;
            for (int label : labelsHubs) {
                switch (label) {
                    case 1:
                        yes++;
                        break;
                    case 2:
                        no++;
                        break;
                    default:
                        unclassified++;
                        break;
                }
            }
            System.out.println("+++ HUBS (M') +++");
            System.out.println("YES: " + yes + ", NO: " + no + ", UNCLASSIFIED: " + unclassified);
            
            
            // obtain the initial label by the files of yes and no hubs
            initLabels = getInitLabel("output/yesAuthorities.txt", "output/noAuthorities.txt", g);
            // Compute LPA for hubs
            int[] labelsAuthorities = ComunityLPA.compute(g, .99d, worker, initLabels);
            // Count the nodes for each label
            yes = 0;
            no = 0;
            unclassified = 0;
            for (int label : labelsAuthorities) {
                switch (label) {
                    case 1:
                        yes++;
                        break;
                    case 2:
                        no++;
                        break;
                    default:
                        unclassified++;
                        break;
                }
            }
            System.out.println("+++ AUTHORITIES (M'') +++");
            System.out.println("YES: " + yes + ", NO: " + no + ", UNCLASSIFIED: " + unclassified);
            

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static int[] getInitLabel(String yesPath, String noPath, WeightedDirectedGraph g) {
        int[] initLabel = new int[g.size];
        
        //System.out.println("Graph size: " + g.size);

        FileReader fr;
        try {
            fr = new FileReader(yesPath);
            BufferedReader br = new BufferedReader(fr);

            String line;
            
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                
                String s = line.split("; ")[2];
                
                if(isInteger(s)) {
                    int id = Integer.parseInt(s);
                    
                    if(initLabel.length >= id && id >=0) {
                        initLabel[id] = 1;
                    }
                }
            }
            br.close();

            fr = new FileReader(noPath);
            br = new BufferedReader(fr);

            while ((line = br.readLine()) != null) {
                
                String s = line.split("; ")[2];
                
                if(isInteger(s)) {
                    int id = Integer.parseInt(s);
                    
                    //System.out.println("Index: " + id);
                    
                    if(initLabel.length > id && id >=0) {
                        initLabel[id] = 2;
                    }
                }
            }
            br.close();

            return initLabel;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static boolean isInteger(String s) {
        return isInteger(s,10);
    }

    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }
}
