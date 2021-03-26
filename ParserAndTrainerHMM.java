import java.io.FileReader;

import java.io.IOException;
import java.util.*;

//TODO() change wordToTag to get a set of tags when word is inserted in the dictionary
public class ParserAndTrainerHMM {
    private Map<String, Map<String, Double>> wordScore; //Observation score {word -> state -> probability}

    private Map<String, Set<String>> wordToTag; //Observation - State

    private Map<String, Map<String, Double>> transScore;
    //Transition scores { currentState -> next -> Score }

    public ParserAndTrainerHMM(String trainTagPath, String trainSentencePath){

        try {
            Scanner sFile = new Scanner(new FileReader(trainSentencePath));
            Scanner tFile = new Scanner(new FileReader(trainTagPath));


            Map<String, Map<String, Integer>> wordFreq = new HashMap<>(); //{word -> tag -> freq}
            Map<String, Integer> tagFreq = new HashMap<>();
            wordToTag = new HashMap<>();

            //Transition freq { currentState -> nextState -> freq }
            Map<String, Map<String, Integer>> transFreq = new HashMap<>();

            Map<String, Integer> tempMap;
            String nextTag; String currWord;


            String[] words; String[] tags;
            while (tFile.hasNext() && sFile.hasNext()) {
                words = sFile.nextLine().split("\\s+");
                tags = tFile.nextLine().split("\\s+");

                if (words.length != tags.length){

                    System.out.println("Words and tags lengths not equal. Check txt file.");
                    return;
                }

                String currTag = "start";
                tagFreq.put(currTag, tagFreq.getOrDefault(currTag,0)+1);

                for (int i = 0; i < words.length; i++) {
                    nextTag = tags[i];
                    currWord = words[i].toLowerCase();

                    //Update transition frequency
                    tempMap = transFreq.getOrDefault(currTag, new HashMap<>());
                    tempMap.put(nextTag, tempMap.getOrDefault(nextTag, 0) + 1);
                    transFreq.put(currTag, tempMap);


                    wordFreq.put(currWord, wordFreq.getOrDefault(currWord, new HashMap<>()));
                    wordFreq.get(currWord).put(nextTag, wordFreq.get(currWord).getOrDefault(nextTag,0)+1);


                    tagFreq.put(nextTag, tagFreq.getOrDefault(nextTag,0)+1);


                    wordToTag.put(currWord, wordToTag.getOrDefault(currWord, new HashSet<>()));
                    wordToTag.get(currWord).add(nextTag);

                    currTag = nextTag;
                }
            }
            //Generate transition and observation scores;

            wordScore = new HashMap<>();
            double prob;
            for (String word: wordFreq.keySet()){
                /*prob = (double) wordFreq.get(word) / (double) tagFreq.get(wordToTag.get(word));
                wordScore.put(word, Math.log(prob));*/
                for (String tag: wordToTag.get(word)){
                    //prob (word (of state/tag = t)) = freq(word (of t)) / freq ( state t)
                    prob = wordFreq.get(word).get(tag) / (double) tagFreq.get(tag);

                    wordScore.put(word, wordScore.getOrDefault(word, new HashMap<>()));
                    wordScore.get(word).put(tag, Math.log(prob));
                }
            }

            transScore = new HashMap<>();
            Map<String,Double> tempM;
            for (String cTag: tagFreq.keySet()){ //Current Tag

                if (transFreq.containsKey(cTag)) { //Not all tags may have a transition
                    for (String nTag : transFreq.get(cTag).keySet()) {
                        //Probability(transition) = transFreq(transition) / totalFreq(currTag)
                        prob = transFreq.get(cTag).get(nTag) / (double) tagFreq.get(cTag);

                        //Update transition score
                        tempM = transScore.getOrDefault(cTag, new HashMap<>());
                        tempM.put(nTag, Math.log(prob));
                        transScore.put(cTag, tempM);

                    }
                }
            }

            sFile.close();
            tFile.close();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }

    }

    public Map<String, Map<String, Double>> getObservationScores() {
        return wordScore;
    }

    public Map<String, Map<String, Double>> getTransitionScores(){
        return transScore;
    }

    public Map<String, Set<String>> getWordToTag() {
        return wordToTag;
    }

    public static void main(String[] args) {
       ParserAndTrainerHMM parse = new ParserAndTrainerHMM("src/simple-train-tags.txt",
                "src/simple-train-sentences.txt");
       System.out.println(parse.getWordToTag());
    }

}
