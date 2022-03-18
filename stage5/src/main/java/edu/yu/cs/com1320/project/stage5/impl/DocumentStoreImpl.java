package edu.yu.cs.com1320.project.stage5.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;


public  class DocumentStoreImpl implements DocumentStore
{
    private BTreeImpl storage;
    private StackImpl commandStack;
    private TrieImpl trie;
    private MinHeapImpl heap;
    private int maxDocumentCount;
    private int maxDocumentBytes;
    private int documentBytes;
    private int documentCount;

    public DocumentStoreImpl(){
        this(null);
    }    

    public DocumentStoreImpl(File baseDir){
        this.storage = new BTreeImpl();
        this.commandStack = new StackImpl();
        this.trie = new TrieImpl();
        this.heap = new MinHeapImpl();
        this.maxDocumentBytes = Integer.MAX_VALUE;
        this.maxDocumentCount = Integer.MAX_VALUE;
        this.documentBytes = 0;
        this.documentCount = 0;
        PersistenceManager<URI,Document> pm = new DocumentPersistenceManager(baseDir);
        this.storage.setPersistenceManager(pm);
    }
    /**
     * the two document formats supported by this document store.
     * Note that TXT means plain text, i.e. a String.
     */
    
    /**
     * @param input the document being put
     * @param uri unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     */
    public int putDocument(InputStream input, URI uri, DocumentFormat format) throws IOException {
        Document doc = null;
        if (uri == null || format == null) {
            throw new IllegalArgumentException() ;
        }
        if (input == null) {
            final Object output =  storage.put(uri,doc);
            commandStack.push(new GenericCommand(uri, (URI) -> {((Document)output).getWords().forEach(s -> trie.put(s, ((Document)output))); ((Document)output).setLastUseTime(System.nanoTime()); heap.insert(((Document)output)); if (storage.put(uri,output) == storage.get(uri)) return true; return false;}));
            if (output == null) {
                return 0;
            } else {
                return output.hashCode();
        }
        }
        if (format.equals(DocumentFormat.BINARY)) {
            byte[] data = input.readAllBytes();
            doc = new DocumentImpl(uri,data);
        } else if (format.equals(DocumentFormat.TXT)) {
            String data = new String(input.readAllBytes(),StandardCharsets.UTF_8);
            doc = new DocumentImpl(uri,data);
        }
        if (documentLimitCheck(doc)) {
            documentLimitFix(doc);
        }
        Object output;
        output =  storage.put(uri,doc);
        doc.setLastUseTime(System.nanoTime());
        addDocCounts(doc);
        heap.insert(doc);
        heap.reHeapify(doc);
        for(String s:doc.getWords()) {
            trie.put(s,doc);
        }
        final Document finalDoc = doc;
        commandStack.push(new GenericCommand(uri, (URI) -> {removeDocCounts(finalDoc); finalDoc.getWords().forEach(s -> trie.put(s, null)); finalDoc.setLastUseTime(System.nanoTime()); if (storage.put(uri,output) == storage.get(uri)) return true; return false;}));
        if (output == null) {
            return 0;
        } else {
            return output.hashCode();
        }
    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document
     */
    public Document getDocument(URI uri) {
        if ((Document)storage.get(uri) != null) {
            ((Document)storage.get(uri)).setLastUseTime(System.nanoTime());
        }
        return (Document)storage.get(uri);
    }
    private void deleteDocumentNoRoom(Document doc) {
        try {
            this.storage.moveToDisk(doc.getKey());
        } catch (Exception e) {
            //TODO: handle exception
        }
    }
       /* for (String s: doc.getWords()) {
            trie.delete(s, doc);
        }
        storage.put(doc.getKey(),null);
        StackImpl tempStack = new StackImpl();
        Undoable temp = null;
        while (commandStack.size() != 0) {
            temp = (Undoable) commandStack.pop();
            if (temp == null) {
            }
            if (temp instanceof CommandSet && ((CommandSet) temp).containsTarget(doc.getKey())) {
                while(((CommandSet) temp).iterator().hasNext()) {
                    GenericCommand command = (GenericCommand)((CommandSet) temp).iterator().next();
                    if (command.getTarget().equals(doc.getKey())) {
                        ((CommandSet) temp).iterator().remove();
                    }
                }
            } else if (temp instanceof GenericCommand && ((GenericCommand)temp).getTarget().equals(doc.getKey())){
                
            } else {
                tempStack.push(temp);
            }
        }
        while (tempStack.size() != 0) {
            commandStack.push(tempStack.pop());
        }

    } */
    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    public boolean deleteDocument(URI uri) {
        if (storage.get(uri) == null) {
            Object output;
            output =  storage.put(uri,null);
            commandStack.push(new GenericCommand(uri, (URI) -> {if (storage.put(uri,output) == storage.get(uri)) return true; return false;}));    
            return false;
        }
        Object output;
        Document doc = (Document)storage.get(uri);
        for (String s: doc.getWords()) {
            trie.delete(s, doc);
        }
        output =  storage.put(uri,null);
        removeDocCounts(((Document)output));
        ((Document)output).setLastUseTime(-1);
        heap.reHeapify((Document)output);
        heap.remove();
        commandStack.push(new GenericCommand(uri, (URI) -> {if(documentLimitCheck(doc)) documentLimitFix(doc); addDocCounts(doc); doc.getWords().forEach(s -> trie.put(s, doc)); ((Document)output).setLastUseTime(System.nanoTime()); heap.insert(((Document)output));if (storage.put(uri,output) == storage.get(uri)) return true; return false;}));
        return true;
    }
    /**
     * undo the last put or delete command
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
    public void undo() throws IllegalStateException {
        if (commandStack.size() == 0) {
            throw new IllegalStateException("hello");
        }
        Undoable action = (Undoable)commandStack.pop();
        action.undo();
    }

    /**
     * undo the last put or delete that was done with the given URI as its key
     * @param uri
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */
    public void undo(URI uri) throws IllegalStateException {
        if (commandStack.size() == 0) {
            throw new IllegalStateException("yay");
        }
        StackImpl tempStack = new StackImpl();
        Undoable temp = null;
        while (commandStack.size() != 0) {
            temp = (Undoable) commandStack.pop();
            if (temp == null) {
            }
            if (temp instanceof CommandSet && ((CommandSet) temp).containsTarget(uri)) {
                ((CommandSet) temp).undo(uri);
                for (int j = 0; j < tempStack.size(); j++){
                    commandStack.push(tempStack.pop());
                }
                return;
            } else if (temp instanceof GenericCommand && ((GenericCommand)temp).getTarget().equals(uri)){
                temp.undo();
                for (int j = 0; j < tempStack.size(); j++){
                    commandStack.push(tempStack.pop());
                }
                return;
            } else {
                tempStack.push(temp);
            }
        }
        while (tempStack.size() != 0) {
            commandStack.push(tempStack.pop());
        }
        throw new IllegalStateException();
    }
        /**
     * Retrieve all documents whose text contains the given keyword.
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE INSENSITIVE.
     * @param keyword
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    public List<Document> search(String keyword) { 
        List list = trie.getAllSorted(keyword, (d1, d2) -> {
            if (((Document) d1).wordCount(keyword) < ((Document) d2).wordCount(keyword)) {
                return -1;
            } else if (((Document) d2).wordCount(keyword) < ((Document) d1).wordCount(keyword)) {
                return 1;
            }
            return 0;
        });
        if (list == null ) {
            return new LinkedList<Document>();
        }
        Long time = System.nanoTime();
        for (Object d: list){
            storage.get(((DocumentImpl)d).getKey());
            ((Document)d).setLastUseTime(time);
            System.out.println("left the building" + ((Document)d).getDocumentTxt());
            heap.insert((Document)d);
            heap.reHeapify(((Document)d));
        }
        
        return list;
    }

    /**
     * Retrieve all documents whose text starts with the given prefix
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE INSENSITIVE.
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    public List<Document> searchByPrefix(String keywordPrefix) {
        List<Document>  list = trie.getAllWithPrefixSorted(keywordPrefix, (d1, d2) -> {
            if (((Document) d1).wordCount(keywordPrefix) < ((Document) d2).wordCount(keywordPrefix)) {
                return -1;
            } else if (((Document) d2).wordCount(keywordPrefix) < ((Document) d1).wordCount(keywordPrefix)) {
                return 1;
            }
            return 0;
        });
        Long time = System.nanoTime();
        for (Document d: list){
            d.setLastUseTime(time);
            heap.reHeapify(((Document)d));
        }
        if (list == null) {
            return new LinkedList<>();
        }
        return list;
    }

    /**
     * Completely remove any trace of any document which contains the given keyword
     * @param keyword
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAll(String keyword) {
        Set<Document> set =  trie.deleteAll(keyword);
        CommandSet undoSet = new CommandSet<>();
        for (Document d:set) {
            storage.put(d.getKey(), null);
            d.setLastUseTime(-1);
            heap.reHeapify(d);
            heap.remove();
            removeDocCounts(d);
            undoSet.addCommand(new GenericCommand(d.getKey(), (URI)-> {if(documentLimitCheck(d)) documentLimitFix(d); addDocCounts(d); trie.put(keyword,d); d.setLastUseTime(System.nanoTime()); heap.insert(d);if (storage.put(d.getKey(),d) == storage.get(d.getKey())) return true; return false; }));
        }
        commandStack.push(undoSet);
        Set<URI> output = new HashSet<>();
        for (Document d: set) {
            output.add(d.getKey());
        }
        return output;
    }

    /**
     * Completely remove any trace of any document which contains a word that has the given prefix
     * Search is CASE INSENSITIVE.
     * @param keywordPrefix
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        Set<Document> set =  trie.deleteAllWithPrefix(keywordPrefix);
        Set<URI> output = new HashSet<>();
        for (Document d: set) {
            output.add(d.getKey());
        }
        CommandSet undoSet = new CommandSet<>();
        for (Document d:set) {
            storage.put(d.getKey(), null);
            d.setLastUseTime(-1);
            heap.reHeapify(d);
            heap.remove();
            removeDocCounts(d);
            undoSet.addCommand(new GenericCommand(d.getKey(), (URI)-> {if(documentLimitCheck(d)) documentLimitFix(d); addDocCounts(d); d.getWords().forEach(s -> trie.put(s, d)); d.setLastUseTime(System.nanoTime()); heap.insert(d);if (storage.put(d.getKey(),d) == storage.get(d.getKey())) return true; return false;}));
        }
        commandStack.push(undoSet);
        return output;
    }
        /**
     * set maximum number of documents that may be stored
     * @param limit
     */
    public void setMaxDocumentCount(int limit) {
        this.maxDocumentCount = limit;
        while( documentCount > maxDocumentCount) {
            Document removal = (Document)heap.remove();
                this.deleteDocumentNoRoom(removal);
                removeDocCounts(removal);
        }
    }

    /**
     * set maximum number of bytes of memory that may be used by all the documents in memory combined
     * @param limit
     */
    public void setMaxDocumentBytes(int limit) {
        this.maxDocumentBytes = limit;
        while(documentBytes > maxDocumentBytes) {
            Document removal = (Document)heap.remove();
                this.deleteDocumentNoRoom(removal);
                removeDocCounts(removal);
        }
        this.maxDocumentBytes = limit;
    }
    private void documentLimitFix(Document doc) {
        if (maxDocumentBytes != Integer.MAX_VALUE) {
            if (doc.getDocumentBinaryData() != null && doc.getDocumentBinaryData().length + documentBytes > maxDocumentBytes) {
                while (doc.getDocumentBinaryData().length + documentBytes > maxDocumentBytes) {
                Document removal = (Document)heap.remove();
                this.deleteDocumentNoRoom(removal);   
                removeDocCounts(removal);             
                }
            } else if (doc.getDocumentTxt().getBytes().length + documentBytes > maxDocumentBytes) {
                while (doc.getDocumentTxt().getBytes().length + documentBytes > maxDocumentBytes) {
                    Document removal = (Document)heap.remove();
                    this.deleteDocumentNoRoom(removal);   
                    removeDocCounts(removal);             
                    }
            }
        }
        if (maxDocumentCount != Integer.MAX_VALUE) {
            if (documentCount + 1 > maxDocumentCount) {
                while (documentCount + 1 > maxDocumentCount) {
                    Document removal = (Document)heap.remove();
                    removeDocCounts(removal);
                    this.deleteDocumentNoRoom(removal);    
                }
            }
        }
    }
    private void removeDocCounts(Document doc) {
        if (doc.getDocumentBinaryData() != null) {
            documentBytes -= doc.getDocumentBinaryData().length;
        } else {
            documentBytes -= doc.getDocumentTxt().getBytes().length;
        }
        documentCount--;
    }
    private void addDocCounts(Document doc) {
        if (doc.getDocumentBinaryData() != null) {
            documentBytes += doc.getDocumentBinaryData().length;
        } else {
            documentBytes += doc.getDocumentTxt().getBytes().length;
        }
        documentCount++;
    }
    private boolean documentLimitCheck(Document doc) {
        if (maxDocumentBytes != Integer.MAX_VALUE) {
            if ((doc.getDocumentBinaryData() != null && doc.getDocumentBinaryData().length + documentBytes > maxDocumentBytes ) || doc.getDocumentTxt().getBytes().length + documentBytes > maxDocumentBytes) {
                return true;
            }
        }
        if (maxDocumentCount != Integer.MAX_VALUE &&  documentCount + 1 > maxDocumentCount) {   
            return true;
        }
        return false;    
    }
}