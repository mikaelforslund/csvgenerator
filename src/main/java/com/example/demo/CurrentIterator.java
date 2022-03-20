package com.example.demo;

import java.util.Iterator;

import lombok.Getter;
public class CurrentIterator<E> {
    @Getter
    private E current;

    private Iterator<E> iterator;

    public CurrentIterator(Iterator<E> iterator) {
        this.iterator = iterator;
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public E next() { 
        current = iterator.next();
        return current;
    }
}