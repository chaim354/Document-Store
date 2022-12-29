package edu.yu.cs.com1320.project.impl;

import java.io.IOException;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

public class BTreeImpl<Key extends Comparable<Key>,Value> implements BTree<Key,Value> {

       
        private static final int MAX = 4;
        private Node root; //root of the B-tree
        private Node leftMostExternalNode;
        private int height; //height of the B-tree
        private int n; //number of key-value pairs in the B-tree
        private PersistenceManager<Key, Value> pm;

        public BTreeImpl() {

            this.root = new Node(0);
            this.leftMostExternalNode = this.root;
            this.height = 0;
            this.n = 0;
            this.pm = null;
        }

        private static final class Node
        {
            private int entryCount; 
            private Entry[] entries = new Entry[BTreeImpl.MAX];
            private Node next;
            private Node previous;
    

            Node(int k) {
                this.entryCount = k;
                this.entries = new Entry[MAX];
            }
    
            private void setNext(Node next)
            {
                this.next = next;
            }
            private Node getNext()
            {
                return this.next;
            }
            private void setPrevious(Node previous)
            {
                this.previous = previous;
            }
            private Node getPrevious()
            {
                return this.previous;
            }
    
            private Entry[] getEntries()
            {
                return Arrays.copyOf(this.entries, this.entryCount);
            }
    
        }
    

        public static class Entry<Key extends Comparable, Value>
        {
            private Comparable key;
            private Object val;
            private Node child;
            boolean storedOnDisk;
    
            public Entry(Comparable key, Object val, Node child)
            {
                this.key = key;
                this.val = val;
                this.child = child;
            }
            public Object getValue()
            {
                return this.val;
            }
            public Comparable getKey()
            {
                return this.key;
            }
        }
    
    public Value get(Key k) {
        if (k == null) {
            throw new IllegalArgumentException("argument to get() is null");
        }
        Entry entry = this.get(this.root, k, this.height);
        if(entry != null) {
            if (entry.val != null) {
                return (Value) entry.val;
        }else {
            Value val = null;
            try {
                val = this.pm.deserialize(k);
                System.out.println(val + "ds");
            } catch (IOException e) {
                e.printStackTrace();
            }
            entry.val = val;
            entry.storedOnDisk = false;
            return (Value) val;
        }
    }
		return null;

    }

    private Entry get(Node currentNode, Key key, int height)
    {
        Entry[] entries = currentNode.entries;


        if (height == 0)
        {
            for (int j = 0; j < currentNode.entryCount; j++)
            {
                if(isEqual(key, entries[j].key))
                {
                    return entries[j];
                }
            }
            return null;
        }

        else
        {
            for (int j = 0; j < currentNode.entryCount; j++)
            {

                if (j + 1 == currentNode.entryCount || less(key, entries[j + 1].key))
                {
                    return this.get(entries[j].child, key, height - 1);
                }
            }

            return null;
        }
    }


    public Value put(Key k, Value v)
    {
        if (k == null)
        {
            throw new IllegalArgumentException("argument key to put() is null");
        }
        @SuppressWarnings("unchecked") Entry<Key,Value> alreadyThere = (Entry<Key,Value>) this.get(this.root, k, this.height);
        if(alreadyThere != null) {
            try {
                return this.replaceEntryValue(alreadyThere, v);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Node newNode = this.put(this.root, k, v, this.height);
        this.n++;
        if (newNode == null)
        {
            return null;
        }


        Node newRoot = new Node(2);
        newRoot.entries[0] = new Entry(this.root.entries[0].key, null, this.root);
        newRoot.entries[1] = new Entry(newNode.entries[0].key, null, newNode);
        this.root = newRoot;
        this.height++;
        return null;
    }

    /**
     *
     * @param currentNode
     * @param key
     * @param val
     * @param height
     * @return null if no new node was created (i.e. just added a new Entry into an existing node). If a new node was created due to the need to split, returns the new node
     */
    private Node put(Node currentNode, Key key, Value val, int height)
    {
        int j;
        Entry newEntry = new Entry(key, val, null);

        if (height == 0)
        {

            for (j = 0; j < currentNode.entryCount; j++)
            {
                if (less(key, currentNode.entries[j].key))
                {
                    break;
                }
            }
        }

        else
        {
            for (j = 0; j < currentNode.entryCount; j++)
            {
                if ((j + 1 == currentNode.entryCount) || less(key, currentNode.entries[j + 1].key))
                {
                    Node newNode = this.put(currentNode.entries[j++].child, key, val, height - 1);
                    if (newNode == null)
                    {
                        return null;
                    }
                    newEntry.key = newNode.entries[0].key;
                    newEntry.val = null;
                    newEntry.child = newNode;
                    break;
                }
            }
        }
        for (int i = currentNode.entryCount; i > j; i--)
        {
            currentNode.entries[i] = currentNode.entries[i - 1];
        }
        currentNode.entries[j] = newEntry;
        currentNode.entryCount++;
        if (currentNode.entryCount < BTreeImpl.MAX)
        {
            return null;
        }
        else
        {
            return this.split(currentNode, height);
        }
    }

    /**
     * split node in half
     * @param currentNode
     * @return new node
     */
    private Node split(Node currentNode, int height)
    {
        Node newNode = new Node(BTreeImpl.MAX / 2);
        currentNode.entryCount = BTreeImpl.MAX / 2;
        for (int j = 0; j < BTreeImpl.MAX / 2; j++)
        {
            newNode.entries[j] = currentNode.entries[BTreeImpl.MAX / 2 + j];
        }
        if (height == 0)
        {
            newNode.setNext(currentNode.getNext());
            newNode.setPrevious(currentNode);
            currentNode.setNext(newNode);
        }
        return newNode;
    }

    private static boolean less(Comparable k1, Comparable k2)
    {
        return k1.compareTo(k2) < 0;
    }

    private static boolean isEqual(Comparable k1, Comparable k2)
    {
        return k1.compareTo(k2) == 0;
    }

    public void moveToDisk(Key k) throws Exception {
        Value val = get(k);
        pm.serialize(k,val);
        put(k, val);

    }
    public void setPersistenceManager(PersistenceManager<Key,Value> pm) {
        this.pm = pm;
    }
    private Value replaceEntryValue(Entry<Key,Value> entry, Value newValue) throws IOException {
        if (entry == null) {
            throw new NullPointerException();
        }
        if (entry.val != null) {
            Value oldValue = (Value)entry.val;
            entry.val = newValue;
            entry.storedOnDisk = false;
            return oldValue;
        }
        entry.val = newValue;
        entry.storedOnDisk = false;
        return this.pm.deserialize((Key) entry.key);
    }

}
