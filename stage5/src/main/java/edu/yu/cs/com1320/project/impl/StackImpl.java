package edu.yu.cs.com1320.project.impl;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import edu.yu.cs.com1320.project.Stack;

/**
 * @param <T>
 */
public class StackImpl<T> implements Stack<T>{
    private LinkedList<T> stack;
    public StackImpl(){
        this.stack = new LinkedList<>();
    }
    /**
     * @param element object to add to the Stack
     */
    public void push(T element) {
        stack.addFirst(element);
    }

    /**
     * removes and returns element at the top of the stack
     * @return element at the top of the stack, null if the stack is empty
     */
    public T pop() {
        try {
            return stack.removeFirst();
            } catch (NoSuchElementException e) {
                return null;
            }
    }

    /**
     *
     * @return the element at the top of the stack without removing it
     */
    public T peek() {
        try {
        return stack.getFirst();
        } catch (NoSuchElementException e) {
            return null;
        }
    } 

    /**
     *
     * @return how many elements are currently in the stack
     */
    public int size() {
        return stack.size();
    }
}

