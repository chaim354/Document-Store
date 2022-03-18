package edu.yu.cs.com1320.project.impl;
import java.util.NoSuchElementException;

import edu.yu.cs.com1320.project.MinHeap;
public class MinHeapImpl<E extends Comparable<E>> extends MinHeap<E> {
    
    public MinHeapImpl () {
        super.elements = (E[])new Comparable[8];

    }
    public void reHeapify(E element) {
        int arrayIndex;
            arrayIndex = getArrayIndex(element);
        super.upHeap(arrayIndex);
        super.downHeap(arrayIndex);


    }

    protected int getArrayIndex(E element) {
        for(int i = 1; i < super.elements.length;i++) {
            if (elements[i] != null && element.equals(super.elements[i])) {
                return i;
            }
        }
        throw new NoSuchElementException();
    }

    protected void doubleArraySize() {
        E[] newArray = (E[])new Object[super.elements.length * 2];
        for (int i = 0; i < super.elements.length;i++) {
            newArray[i] = super.elements[i];
        }
        elements = newArray;
    }

    
}
