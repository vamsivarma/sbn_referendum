/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uniroma1.sbn.finalproject.avellinotrappolini.italianreferendum2016;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.lucene.document.Document;
import uniroma1.sbn.finalproject.gunturi.italianreferendum2016.Manager.TweetsIndexManager;

/**
 *
 * @author Vamsi Gunturi
 */
public class TweetsSearcher {

    public static void main(String[] args) throws IOException {
        //Yes Authorities
//        printTweets("77934969");
//        printTweets("3337657533");
//        printTweets("130851801");
//        printTweets("13294452");
//        printTweets("1017467430");
        //No Authorities
//        printTweets("89212874");
//        printTweets("372273460");
//        printTweets("322933929");
//        printTweets("134431419");
//        printTweets("387198659");

        //Yes Hubs
//        printTweets("1513014650");
//        printTweets("1034988296");
//        printTweets("2687289798");
//        printTweets("91350066");
//        printTweets("1041804853");
        //No Hubs
//        printTweets("3029167981");
//        printTweets("3034434386");
//        printTweets("372273460");
//        printTweets("322933929");
//        printTweets("1114768056");

        //Yes Brokers
//        printTweets("944721086");
//        printTweets("452343838");
//        printTweets("104239528");
//        printTweets("405792460");
//        printTweets("62022464");
        //No Brokers
//        printTweets("372273460");
//        printTweets("910314312");
//        printTweets("1385237533");
//        printTweets("1088677627");
        printTweets("330251483");
    }

    private static void printTweets(String userId) throws IOException {
        TweetsIndexManager tim = new TweetsIndexManager("index/AllTweetsIndex");
        ArrayList<Document> docs = tim.searchForField("userId", userId, 100000);

        System.out.println(docs.get(0).get("screenName") + " " + docs.get(0).get("name"));

        for (Document doc : docs) {
            System.out.println("post: " + doc.get("tweetText"));
            System.out.println("hashtags: " + doc.get("hashtags"));
            System.out.println("mentioned: " + doc.get("mentioned"));
            System.out.println("------------------------");
        }
    }
}
