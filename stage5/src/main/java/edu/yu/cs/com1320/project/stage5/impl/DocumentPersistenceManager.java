package edu.yu.cs.com1320.project.stage5.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;



public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    private final File baseDir;
    public DocumentPersistenceManager(File baseDir){
        if (baseDir == null) {
            this.baseDir = new File(System.getProperty("user.dir"));
        }
        else {
            this.baseDir = baseDir.getAbsoluteFile();
        }
    }
    class DocumentDeserializer implements JsonDeserializer<DocumentImpl> {

        @Override
        public DocumentImpl deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

            JsonObject object = jsonElement.getAsJsonObject();

            String docTxt = object.get("documentTxt").getAsString().trim();
            String uriString = object.get("uriString").getAsString();
            String wordsList = object.get("wordsList").getAsString();
            String countsList = object.get("countsList").getAsString();


            URI uri = null;
            try {
                uri = new URI(uriString);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            String[] words = this.convertToStringArray(wordsList);
            int[] wordCounts = this.convertToIntArray(countsList);
            Map<String, Integer> wordMap = new HashMap<>();
            for (int i = 0; i < words.length; i++) {
                wordMap.put(words[i], wordCounts[i]);
            }

            return new DocumentImpl(uri, docTxt,  wordMap);
        }

        private String[] convertToStringArray(String wordsList) {
            int numberOfWords = 0;
            char[] chars = wordsList.toCharArray();
            for (char c : chars) {
                if (c == '`') {
                    numberOfWords++;
                }
            }

            numberOfWords++;

            String[] words = new String[numberOfWords];
            int numberOfWordsAdded = 0;
            StringBuilder word = new StringBuilder();
            for (char c : chars) {
                if (c == '`') {
                    words[numberOfWordsAdded++] = word.toString();
                    word = new StringBuilder();
                } else {
                    word.append(c);
                }
            }
            
            words[numberOfWordsAdded++] = word.toString();
            return words;
        }

        private int[] convertToIntArray(String countsList) {
            int numberOfWords = 0;
            char[] chars = countsList.toCharArray();
            for (char c : chars) {
                if (c == '`') {
                    numberOfWords++;
                }
            }
            
            numberOfWords++;

            int[] wordCounts = new int[numberOfWords];
            int numberOfCountsAdded = 0;
            StringBuilder numberString = new StringBuilder();
            for (char aChar : chars) {
                if (aChar == '`') {
                    wordCounts[numberOfCountsAdded++] = Integer.parseInt(numberString.toString());
                    numberString = new StringBuilder();
                } else {
                    numberString.append(aChar);
                }
            }
            wordCounts[numberOfCountsAdded++] = Integer.parseInt(numberString.toString());
            return wordCounts;
        }
    }
    class DocumentSerializer implements JsonSerializer<DocumentImpl> {

        @Override
        public JsonElement serialize(DocumentImpl document, Type type, JsonSerializationContext jsonSerializationContext) {


            JsonObject jsonObject = new JsonObject();
            String docTxt = document.getDocumentTxt().trim();
            URI uri = document.getKey();

            String[] words = this.getDocWordSet(document);
            int[] wordCounts = this.getWordCounts(document, words);

            StringBuilder wordsList = new StringBuilder(words[0]);
            StringBuilder countsList = new StringBuilder(Integer.toString(wordCounts[0]));

            for (int i = 1; i < words.length; i++) {
                wordsList.append("`").append(words[i]);
            }
            for (int i = 1; i < wordCounts.length; i++) {
                countsList.append("`").append(wordCounts[i]);
            }

            jsonObject.addProperty("documentTxt", docTxt);
            jsonObject.addProperty("uriString", uri.toString());
            jsonObject.addProperty("wordsList", wordsList.toString());
            jsonObject.addProperty("countsList", countsList.toString());

            return jsonObject;
        }

        private String[] getDocWordSet(DocumentImpl document) {
            Set<String> words = document.getWords();
            String[] wordArray = new String[words.size()];
            int i = 0;
            for (String word : words) {
                wordArray[i++] = word;
            }
            return wordArray;
        }

        private int[] getWordCounts(DocumentImpl document, String[] words) {
            int[] wordCounts = new int[words.length];
            int i;
            for (i = 0; i < words.length; i++) {
                wordCounts[i] = document.wordCount(words[i]);
            }
            return wordCounts;
        }
    }
    @Override
    public void serialize(URI uri, Document val) throws IOException {

        String filePath = this.getAuthorityAndPath(uri);
        filePath = generifyUriPathString(filePath);
        filePath = filePath + ".json";
        filePath = this.baseDir.getAbsolutePath() + File.separator + filePath;

        filePath = this.saveUriDirStructureToSystem(filePath);

        Gson gson = new GsonBuilder().registerTypeAdapter(DocumentImpl.class, new DocumentSerializer()).create();
        Type type = new TypeToken<DocumentImpl>(){}.getType();
        String jsonString = gson.toJson(val, type);
        FileWriter fileWriter = new FileWriter(filePath);
        fileWriter.write(jsonString);
        fileWriter.close();
    }

    private String generifyUriPathString(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path string was null");
        }
        char[] chars = path.toCharArray();
        for (int i = 0; i < path.length(); i++) {
            if (chars[i] == '/') {
                chars[i] = File.separatorChar;
            }
        }
        return new String(chars);
    }

    @Override
    public Document deserialize(URI uri) throws IOException {
        if (uri == null) {
            throw new IllegalArgumentException("uri was null");
        }
        String filePath = this.getAuthorityAndPath(uri);
        filePath = this.generifyUriPathString(filePath);
        filePath = filePath + ".json";
        filePath = this.baseDir.getAbsolutePath() + File.separator + filePath;
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("hell");
            return null;
        }
        Scanner scanner = new Scanner(file);
        String jsonString = "";
        while (scanner.hasNext()) {
            jsonString += scanner.next();
            jsonString += " ";
        }
        scanner.close();
        Gson gson = new GsonBuilder().registerTypeAdapter(DocumentImpl.class, new DocumentDeserializer()).create();
        Type documentImplType = new TypeToken<DocumentImpl>(){}.getType();
        Document document = gson.fromJson(jsonString.trim(), documentImplType);

        
        Files.delete(file.toPath());
        this.postOrderDeleteEmptyDirs(file.getParentFile());
        System.out.println("dscsddcsdcsdc");
        return document;
    }
    private String getAuthorityAndPath(URI uri) {
        if (uri.getPath() == null && uri.getAuthority() == null) {
            return null;
        }
        if (uri.getPath() == null) {
            return uri.getAuthority();
        }
        if (uri.getAuthority() == null) {
            return uri.getPath();
        }
        String authority = uri.getAuthority();
        String path = uri.getPath();
        return authority + path;
    }

    private String saveUriDirStructureToSystem(String absolutePathName) {
        File file = new File(absolutePathName);
        if (!file.isAbsolute()) {
            throw new IllegalArgumentException("string absolutePathName was not an absolute path name");
        }
        File parentDir = new File(file.getParent());
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        return file.getAbsolutePath();
    }

    private void postOrderDeleteEmptyDirs(File f) throws IOException {
        if (f == null) {
            throw new  NullPointerException();
        }
        if (!f.exists()) {
            throw new IllegalArgumentException("file doesn't exist, can't delete it");
        }
        if (!f.isDirectory()) {
            throw new IllegalArgumentException("file was not a directory");
        }

        File file = f;
        File[] list = file.listFiles();
        while (list.length == 0) {
            Files.delete(file.toPath());
            file = file.getParentFile();
            list = file.listFiles();
        }
    }

    @Override
    public boolean delete(URI uri) throws IOException {
        return false;
    }
}