package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TrieImpl<Value> implements Trie<Value>{
   
   
    public TrieImpl () {
        this.root = new Node();
    }
    private  final int alphabetSize = 256; // extended ASCII
    private Node root; // root of trie

    class Node<Value>
    {
        protected List<Value> val;
        protected Node[] links = new Node[alphabetSize];
        public Node() { 
            this.val= new LinkedList<Value>(); 
        } 
    }
    /**
     * add the given value at the given key
     * @param key
     * @param val
     */
    public void put(String key, Value val) {
                //deleteAll the value from this key
                if (val == null)
                {
                    this.deleteAll(key);
                }
                else
                {
                    key = key.toLowerCase();
                    this.root = put(this.root, key, val, 0);
                }
    }

    private Node put(Node x, String key, Value val, int d)
    {
        //create a new node
        if (x == null)
        {
            x = new Node();
        }
        //we've reached the last node in the key,
        //set the value for the key and return the node
        if (d == key.length())
        {
            x.val.add(val);
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char c = key.charAt(d);
        x.links[c] = this.put(x.links[c], key, val, d + 1);
        return x;
    }






    /**
     * get all exact matches for the given key, sorted in descending order.
     * Search is CASE INSENSITIVE.
     * @param key
     * @param comparator used to sort  values
     * @return a List of matching Values, in descending order
     */
    public List<Value> getAllSorted(String key, Comparator<Value> comparator) {
        key = key.toLowerCase();
        Node x = this.get(this.root, key, 0);
        if (x == null)
        {
            return null;
        }
        List list = x.val;
        Collections.sort(list,comparator);
        return list;
    }

    private Node get(Node x, String key, int d) {
        //link was null - return null, indicating a miss
        key = key.toLowerCase();
        if (x == null)
        {
            return null;
        }
        //we've reached the last node in the key,
        //return the node
        if (d == key.length())
        {
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char c = key.charAt(d);
        return this.get(x.links[c], key, d + 1);
    }

    /**
     * get all matches which contain a String with the given prefix, sorted in descending order.
     * For example, if the key is "Too", you would return any value that contains "Tool", "Too", "Tooth", "Toodle", etc.
     * Search is CASE INSENSITIVE.
     * @param prefix
     * @param comparator used to sort values
     * @return a List of all matching Values containing the given prefix, in descending order
     */
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        prefix = prefix.toLowerCase();
        Node x = this.get(this.root, prefix, 0);
        if (x == null) {
            return new LinkedList<>();
        }
        List<Value> list = getAllWithPrefixSortedNew(x,prefix, new LinkedList());
        Collections.sort(list,comparator);
        return list;
    }
    private List<Value> getAllWithPrefixSortedNew(Node x, String prefix, LinkedList linkedList) {
        if (x.val != null) {
            linkedList.addAll(x.val);
        }
        for (char c = 0; c < this.alphabetSize; c++) {
            if(x.links[c]!=null){
            //add child's char to the string
            prefix = prefix + c;
            this.getAllWithPrefixSortedNew(x.links[c], prefix, linkedList);
            //remove the child's char to prepare for next iteration
            prefix = prefix.substring(0, prefix.length() - 1);
            }
        }
        return linkedList;
    }

    /**
     * Delete the subtree rooted at the last character of the prefix.
     * Search is CASE INSENSITIVE.
     * @param prefix
     * @return a Set of all Values that were deleted.
     */
    public Set<Value> deleteAllWithPrefix(String prefix) {
        prefix = prefix.toLowerCase();
        Node x = this.get(this.root, prefix, 0);
        if (x == null) {
            return new HashSet<>();
        }
        List<Value> list = getAllWithPrefixSortedNew(x,prefix, new LinkedList());
        Set<Value> set = new HashSet<>(list);
        x.links = new Node[alphabetSize];
        x.val = new LinkedList<Value>();
        return set;

    }

    /**
     * Delete all values from the node of the given key (do not remove the values from other nodes in the Trie)
     * @param key
     * @return a Set of all Values that were deleted.
     */
    public Set<Value> deleteAll(String key) {
        Node x = this.get(this.root, key, 0);
        if (x == null) {
            return new HashSet<>();
        }
        Set<Value> set = new HashSet<>(x.val);
        x.val = new LinkedList<>();
        return set;
       // this.root = deleteAll(this.root, key, 0);
    }
    private Node deleteAll(Node x, String key, int d)
    {
        if (x == null)
        {
            return null;
        }
        //we're at the node to del - set the val to null
        if (d == key.length())
        {
            x.val = null;
        }
        //continue down the trie to the target node
        else
        {
            char c = key.charAt(d);
            x.links[c] = this.deleteAll(x.links[c], key, d + 1);
        }
        //this node has a val – do nothing, return the node
        if (x.val != null)
        {
            return x;
        }
        //remove subtrie rooted at x if it is completely empty	
        for (int c = 0; c <alphabetSize; c++)
        {
            if (x.links[c] != null)
            {
                return x; //not empty
            }
        }
        //empty - set this link to null in the parent
        return null;
    }

    /**
     * Remove the given value from the node of the given key (do not remove the value from other nodes in the Trie)
     * @param key
     * @param val
     * @return the value which was deleted. If the key did not contain the given value, return null.
     */
    public Value delete(String key, Value val) {
        Node hi = this.get(this.root, key, 0);
        if (hi.val.remove(val)) {
            return val;
        }
        return null;
    }
 
}
