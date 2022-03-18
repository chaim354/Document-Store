package edu.yu.cs.com1320.project.stage5.impl;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import edu.yu.cs.com1320.project.stage5.Document;
import java.util.HashMap;

public class DocumentImpl  implements Document{

    private String text;
    private byte[] binaryData;
    private URI uri;
    private Map<String, Integer> wordCount;
    private Long lastUseTime;

    public DocumentImpl(URI uri, String txt) {
        if (txt == null || txt.isEmpty() || uri == null || uri.toString() == ""){
            throw new IllegalArgumentException();
        }

            this.wordCount = new HashMap<String, Integer> ();
            String string = txt;
            string = string.replaceAll("[^a-zA-Z0-9 ]", "");
            string = string.toLowerCase();
            String[] strings = string.split(" ");
            for (String s:strings) {
              if (s.length() == 0) {
                  continue;
              }
              if (!wordCount.containsKey(s)) {  // first time we've seen this string
                wordCount.put(s, 1);
              }
              else {
                int count = wordCount.get(s);
                wordCount.put(s, count + 1);
              }
            }
        this.text = txt;
        this.uri = uri;
        this.binaryData = null;
    }

    public DocumentImpl(URI uri, byte[] binaryData) {
        if (binaryData == null || uri == null || binaryData.length == 0 || uri.toString() == ""){
            throw new IllegalArgumentException();
        }
        this.binaryData = binaryData;
        this.uri = uri;
    }
    protected DocumentImpl (URI uri, String txt, Map<String,Integer> wordMap) {
        this.uri = uri;
        this.text = txt;
        this.wordCount = wordMap;
        this.binaryData = null;
    }
    /**
     * @return content of text document
     */
    public String getDocumentTxt(){
        return this.text;
    }

    /**
     * @return content of binary data document
     */
    public byte[] getDocumentBinaryData() {
        return this.binaryData;
    }

    /**
     * @return URI which uniquely identifies this document
     */
    public URI getKey() {
        return this.uri;
    }
        /**
     * how many times does the given word appear in the document?
     * @param word
     * @return the number of times the given words appears in the document. If it's a binary document, return 0.
     */
    public int wordCount(String word) {
        word = word.replaceAll("[^a-zA-Z0-9]", "");
        word = word.toLowerCase();
        if (wordCount == null || wordCount.get(word) == null) {
            return 0;
        }
        return wordCount.get(word);
    }

        /**
     * @return all the words that appear in the document
     */
    public Set<String> getWords() {
        if (wordCount == null) {
            return Collections.emptySet();
        }
        return wordCount.keySet();
    }
        /**
     * return the last time this document was used, via put/get or via a search result
     * (for stage 4 of project)
     */
    public long getLastUseTime() {
        return lastUseTime;

    }
    public void setLastUseTime(long timeInNanoseconds) {
        this.lastUseTime = timeInNanoseconds;
    }

    /**
     * @return a copy of the word to count map so it can be serialized
     */
    public Map<String,Integer> getWordMap() {
        return Map.copyOf(wordCount);
    }

    /**
     * This must set the word to count map during deserialization
     * @param wordMap
     */
    public void setWordMap(Map<String,Integer> wordMap) {
        this.wordCount = wordMap;
    }

    @Override
    public int hashCode() {
        int result = uri.hashCode();
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(binaryData);
        return result;
    }

    @Override
    public boolean equals(Object document) {
        if (this.hashCode() == document.hashCode()) {
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(Document document) {
        if (this.lastUseTime == document.getLastUseTime()) {
            return 0;
        } else if (this.lastUseTime >  document.getLastUseTime()){
            return 1;
        } else if (this.lastUseTime < document.getLastUseTime()) {
            return -1;
        }
        return 0;
    }
}