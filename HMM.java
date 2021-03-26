import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class HMM {
    Map<String, Map<String, Double>> observationScr;
    Map<String, Map<String, Double>> transitionScr;

    public HMM(String trainPath_Tags, String trainPath_Sentence){
        if (trainPath_Sentence!= null && trainPath_Tags!=null)
        train(trainPath_Tags, trainPath_Sentence);
    }

    public void train(String trainPath_Tags, String trainPath_Sentence){
        ParserAndTrainerHMM trainerHMM = new ParserAndTrainerHMM(trainPath_Tags, trainPath_Sentence);
        observationScr = trainerHMM.getObservationScores();
        transitionScr = trainerHMM.getTransitionScores();
    }

    /**
     * Outputs data from viterbi algorithm into a text file
     * @param testSentence_Path Output txt file path
     */
    public void viterbi_ToFile (String testSentence_Path){

        String solutionFile = testSentence_Path.substring(0, testSentence_Path.length()-4)+"_soln.txt";
        String[] line;
        try {
            Scanner obsFile = new Scanner(new FileReader(testSentence_Path));
            BufferedWriter solnFile = new BufferedWriter(new FileWriter(solutionFile));


            Map<String, String> prevStateMap;
            Map<String, Double> currScores;
            Map<String, Double> nextScores;

            Set<String> currStates = new HashSet<>();
            Set<String> nextStates = new HashSet<>();

            Double score; double U = -100.0;
            List<Map<String, String>> backtracks;

            while (obsFile.hasNext()){

                line = obsFile.nextLine().split("\\s+");

                currStates.clear();
                currStates.add("start");

                int i = -1; Double obsScr; Double tScr; //Transition score

                prevStateMap = new HashMap<>();
                currScores = new HashMap<>();

                currScores.put("start", 0.0);
                prevStateMap.put("start", null);

                backtracks = new ArrayList<>();
                while (++i < line.length){


                    nextScores = new HashMap<>();
                    for (String currState: currStates) {


                        if (transitionScr.containsKey(currState)) {
                            for (String nxtState : transitionScr.get(currState).keySet()) {

                                //Observation scores change depending on next state
                                //Set observation score to U if current observation doesn't exist in library or
                                // is not found in next state.
                                if (observationScr.containsKey(line[i])) {
                                    obsScr = observationScr.get(line[i]).getOrDefault(nxtState, U);
                                } else {
                                    obsScr = U;
                                }
                                //transition score
                                tScr = transitionScr.get(currState).get(nxtState);

                                score = tScr + obsScr + currScores.get(currState);

                                if (nextScores.containsKey(nxtState)) {
                                    //Check for which is maximum and update backtrack and score accordingly
                                    if (score > nextScores.get(nxtState)) {
                                        nextScores.put(nxtState, score);
                                        prevStateMap.put(nxtState, currState);
                                    }

                                } else {
                                    nextScores.put(nxtState, score);
                                    prevStateMap.put(nxtState, currState);
                                }
                                nextStates.add(nxtState);
                            }
                        }
                    }
                    //Update current states and current scores.
                    currStates = nextStates;
                    nextStates = new HashSet<>();

                    currScores = nextScores;

                    backtracks.add(prevStateMap);
                    prevStateMap = new HashMap<>();


                }
                solnFile.write(getTagsFromBackTrack(currScores, backtracks));
            }
            obsFile.close();
            solnFile.close();
        }

        catch (IOException e){
            System.out.println(e.getCause()+e.getMessage());
        }
        System.out.println("Check "+solutionFile+" for generated tags");


        String test_Tag_Path = testSentence_Path.substring(0, testSentence_Path.length() - 13);
        test_Tag_Path = test_Tag_Path.concat("tags.txt");
        TestFileEquality(solutionFile,test_Tag_Path );
    }


    /**
     * Simple Command Line interface for quick console and file-driven tests
     */
    public void CLI(){
        System.out.println("Type 'help'/'h' for cmdline information");
        String help =
                "------------------------------------------Console--------------------------------\n"+
                "cs : Test trained data from simple sentences on single line sentences in console\n" +
                        "cb: Test trained data from brown sentences on single line sentences in console [recommended]\n"+
                        "---------------------------------------Output to file-------------------------\n"+
                        "ts : Test trained data on the 'simple sentences' database\n" +
                        "tb : Test trained data on the 'brown sentences' database\n" +
                        "help: Displays help\n" +
                        "q : Quit program\n";
        boolean run = true;
        HMM brown = new HMM("src/brown-train-tags.txt",
                "src/brown-train-sentences.txt");

        HMM simple = new HMM ("src/simple-train-tags.txt",
                "src/simple-train-sentences.txt");


        while (run) {
            System.out.println("Input command here:  ");
            Scanner input = new Scanner(System.in);

            String cmd = input.next().strip().toLowerCase();
            if (cmd.equals("cb")) {
                System.out.println("[Console mode - 'brown database' file]");
                brown.viterbi_ToConsole();
            }
            else if (cmd.equals("cs")){
                System.out.println("[Console mode - 'simple sentences database']");
                simple.viterbi_ToConsole();
            }
            else if (cmd.equals("ts")){
                simple.viterbi_ToFile("src/simple-test-sentences.txt");
            }
            else if (cmd.equals("tb")){
                brown.viterbi_ToFile("src/brown-test-sentences.txt");
            }
            else if (cmd.equals("help") || cmd.equals("h")){
                System.out.println(help);
            }
            else if (cmd.equals("q")){
                run = false;
            }
            else {
                System.out.println("Unknown command");
            }
        }
    }

    /**
     * Returns a string of the tags in the right order
     * (items closer to the start state are displayed first)
     * @param currScores
     * @param backtracks
     * @return
     */
    private String getTagsFromBackTrack(Map<String, Double> currScores, List<Map<String, String>> backtracks) {
        StringBuilder result;
        Map<String, String> lastMap = backtracks.get(backtracks.size()-1);

        //Find highest score in last observation
       String maxState = "";
        double max = Double.NEGATIVE_INFINITY;

        for (String state: lastMap.keySet()){
            if (currScores.getOrDefault(state, Double.NEGATIVE_INFINITY) > max){
                max = currScores.get(state);
                maxState = state;
            }
        }

        result = new StringBuilder(maxState);
        String prev = maxState;

        for (int i =backtracks.size()-1; i>0;i--){

           prev = backtracks.get(i).get(prev);
            result.insert(0, prev+" ");
        }
        result.insert(result.length(), "\n");
        return result.toString();
    }

    /*
    Compares 2 files. [Used to compare file generated from the viterbi algorithm
    with file with file with correct tags]
     */
    public static void TestFileEquality (String file1, String file2){
        int errors = 0; int correct = 0;
        try {
            Scanner second = new Scanner(new FileReader(file2));
            Scanner first = new Scanner(new FileReader(file1));

            while (second.hasNext() && first.hasNext()){
                if (!first.next().equals(second.next())) errors++;
                else correct ++;
            }
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
        System.out.println("The model got "+correct+" tags right and "+errors+" tags wrong");
    }

    /**
     * Prints data from viterbi algorithm, tested on a single line sentence.
     */
    public void viterbi_ToConsole(){
        System.out.println("Input single line sentence here: ");
        Scanner consoleInput = new Scanner(System.in);

        String [] line = consoleInput.nextLine().split("\\s+");

        Set<String> currStates = new HashSet<>();
        Set<String> nextStates = new HashSet<>();

        Double score; Double obsScr; Double tScr;
        double U = -100.0;

        currStates.add("start");

        int i = -1;

        Map<String, String> prevStateMap = new HashMap<>();
        Map<String, Double> currScores = new HashMap<>();
        Map<String, Double> nextScores;

        currScores.put("start", 0.0);
        prevStateMap.put("start", null);

        List<Map<String, String>> backtracks  = new ArrayList<>();
        while (++i < line.length) {


            nextScores = new HashMap<>();
            for (String currState : currStates) {


                if (transitionScr.containsKey(currState)) {
                    for (String nxtState : transitionScr.get(currState).keySet()) {

                        //Observation scores change depending on next state
                        //Set observation score to U if current observation doesn't exist in library or
                        // is not found in next state.
                        if (observationScr.containsKey(line[i])) {
                            obsScr = observationScr.get(line[i]).getOrDefault(nxtState, U);
                        } else {
                            obsScr = U;
                        }

                        tScr = transitionScr.get(currState).get(nxtState);

                        score = tScr + obsScr + currScores.get(currState);

                        if (nextScores.containsKey(nxtState)) {
                            //Check for which is maximum and update backtrack and score accordingly
                            if (score > nextScores.get(nxtState)) {
                                nextScores.put(nxtState, score);
                                prevStateMap.put(nxtState, currState);
                            }

                        } else {
                            nextScores.put(nxtState, score);
                            prevStateMap.put(nxtState, currState);
                        }
                        nextStates.add(nxtState);
                    }
                }
            }
            //Update current states and current scores.
            currStates = nextStates;
            nextStates = new HashSet<>();

            currScores = nextScores;

            backtracks.add(prevStateMap);
            prevStateMap = new HashMap<>();


        }
        System.out.println("The parts of speech are: \n"+getTagsFromBackTrack(currScores, backtracks));
    }

    public static void main(String[] args) {
        HMM model = new HMM(null,null);
        model.CLI();
    }
}
