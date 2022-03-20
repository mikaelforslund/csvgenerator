package com.example.demo.csv;

import java.util.Map;

@FunctionalInterface
public interface ColumnProcessor  {
    String[] process(Object o, String[] r, Map<String, Integer> columnIndexMap);
}
