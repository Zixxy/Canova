package org.nd4j.nlp.movingwindow;



import org.nd4j.api.berkeley.Pair;
import org.nd4j.api.berkeley.StringUtils;
import org.nd4j.api.util.MultiDimensionalMap;
import org.nd4j.nlp.tokenization.tokenizer.Tokenizer;
import org.nd4j.nlp.tokenization.tokenizerfactory.TokenizerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Context Label Retriever
 * @author Adam Gibson
 */
public class ContextLabelRetriever {


    private static String BEGIN_LABEL = "<([A-Za-z]+|\\d+)>";
    private static String END_LABEL = "</([A-Za-z]+|\\d+)>";


   private ContextLabelRetriever() {}


    /**
     * Returns a stripped sentence with the indices of words
     * with certain kinds of labels.
     * @param sentence the sentence to process
     * @return a pair of a post processed sentence
     * with labels stripped and the spans of
     * the labels
     */
    public static Pair<String,MultiDimensionalMap<Integer,Integer,String>> stringWithLabels(String sentence,TokenizerFactory tokenizerFactory) {
        MultiDimensionalMap<Integer,Integer,String> map = MultiDimensionalMap.newHashBackedMap();
        Tokenizer t = tokenizerFactory.create(sentence);
        List<String> currTokens = new ArrayList<>();
        String currLabel = null;
        String endLabel = null;
        List<Pair<String,List<String>>> tokensWithSameLabel = new ArrayList<>();
        while(t.hasMoreTokens()) {
            String token = t.nextToken();
            if(token.matches(BEGIN_LABEL)) {
                if(endLabel != null)
                    throw new IllegalStateException("Tried parsing sentence; found an end label when the begin label has not been cleared");
                currLabel = token;

                //no labels; add these as NONE and begin the new label
                if(!currTokens.isEmpty()) {
                    tokensWithSameLabel.add(new Pair<>("NONE",(List<String>) new ArrayList<>(currTokens)));
                    currTokens.clear();

                }

            }
            else if(token.matches(END_LABEL)) {
                if(currLabel ==  null)
                    throw new IllegalStateException("Found an ending label with no matching begin label");
                endLabel = token;
            }
            else
                currTokens.add(token);

            if(currLabel != null && endLabel != null) {
                currLabel = currLabel.replaceAll("[<>/]","");
                endLabel = endLabel.replaceAll("[<>/]","");
                assert !currLabel.isEmpty() : "Current label is empty!";
                assert !endLabel.isEmpty() : "End label is empty!";
                assert currLabel.equals(endLabel) : "Current label begin and end did not match for the parse. Was: " + currLabel + " ending with " + endLabel;

                tokensWithSameLabel.add(new Pair<>(currLabel,(List<String>) new ArrayList<>(currTokens)));
                currTokens.clear();


                //clear out the tokens
                currLabel = null;
                endLabel = null;
            }


        }

        //no labels; add these as NONE and begin the new label
        if(!currTokens.isEmpty()) {
            tokensWithSameLabel.add(new Pair<>("none",(List<String>) new ArrayList<>(currTokens)));
            currTokens.clear();

        }

        //now join the output
        StringBuffer strippedSentence = new StringBuffer();
        for(Pair<String,List<String>> tokensWithLabel : tokensWithSameLabel) {
            String joinedSentence = StringUtils.join(tokensWithLabel.getSecond(), " ");
            //spaces between separate parts of the sentence
            if(!(strippedSentence.length() < 1))
                strippedSentence.append(" ");
            strippedSentence.append(joinedSentence);
            int begin = strippedSentence.toString().indexOf(joinedSentence);
            int end = begin + joinedSentence.length();
            map.put(begin,end,tokensWithLabel.getFirst());
        }



        return new Pair<>(strippedSentence.toString(),map);
    }



}
