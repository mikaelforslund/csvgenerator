package com.example.demo.csv;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.codepoetics.protonpack.StreamUtils;
import com.example.demo.CurrentIterator;
import com.example.domain.DataItem;

import org.springframework.util.ClassUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CsvGenerator {
    private Map<String, CurrentIterator<?>> iterators = new HashMap<>();
	private Deque<CurrentIterator<?>> stack = new ArrayDeque<>();
	private boolean started;
    private List<String> columns;

	private Map<String, Integer> columnIndexMap;

	@Getter
	private String header;	

	@Getter
    private String columnDelimiter;
	private Map<String, ColumnProcessor> columnProcessorMap;
	private List<String> excludeList;

    public CsvGenerator(List<String> columns, String columnDelimiter, List<String> excludeList, 
		Map<String, ColumnProcessor> columnProcessorMap) {

		this.excludeList = excludeList;
		this.columnProcessorMap = columnProcessorMap;
		
        this.columnDelimiter = columnDelimiter;
		this.columns = columns;

		header = String.join(columnDelimiter, columns);

		columnIndexMap = StreamUtils.zipWithIndex(columns.stream())
							.collect(Collectors.toList()).stream()
							.collect(Collectors.toMap(v -> (String)v.getValue(), i -> (int)i.getIndex()));

    }

	public Stream<String[]> toCsv(Object o) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<String[]>() {

			@Override
			public boolean hasNext() {
				return started == false || started && iterators.size() != 0;
			}

			@Override
			public String[] next() {
				started = !started? true : started; 	
				return reflect(o, new String[columns.size()], "");	// to preserve the column order
			}

		}, Spliterator.IMMUTABLE), false);
	}

	private void processList(Object o, String[] row, Field field, String path)
			throws IllegalArgumentException, IllegalAccessException {
		
		//log.info("path = {}", path);

		CurrentIterator<?> iterator = iterators.get(field.getName());
		if (iterator == null) {
			iterator = new CurrentIterator<>( ((List<?>) field.get(o)).iterator() );
			iterators.put(field.getName(), iterator);
			stack.push(iterator);
		}
		
		// are we at the correct level to iterate through this current collection....
		if (stack.peek() == iterator) {		
			if (iterator.hasNext()) {
				reflect(iterator.next(), row, path);	
			}

		// ... or do we need to iterate thorugh the childeren first?
		} else if (iterator.getCurrent() != null) {
			reflect(iterator.getCurrent(), row, path);
		}

		// pop/remove expired iterator... 
		if (stack.peek() == iterator && !iterator.hasNext()) {
			iterators.remove(field.getName());
			stack.pop();
		}
	}

	private boolean isCollectionType(Field field) { 
		return List.class.isAssignableFrom(field.getType());
	}

	private boolean isPrimitiveOrString(Field field) { 
		return ClassUtils.isPrimitiveOrWrapper(field.getType()) || field.getType().equals(String.class); 
	}

	private String[] reflect(Object o, String[] row, String path) {
		try {
			// do next row...
			for (var field : o.getClass().getDeclaredFields()) {
				//log.info(field.getName());
				field.setAccessible(true);
				
				if (excludeList.contains(field.getName()) || field.get(o) == null) {
					continue;
				}

				if (columnProcessorMap.containsKey(field.getName())) {
					columnProcessorMap.get(field.getName()).process(field.get(o), row, columnIndexMap);

				} else if (isCollectionType(field)) {
					processList(o, row, field, path + "-" + field.getName());

				} else if (!isPrimitiveOrString(field)) {
					reflect(field.get(o), row, path + "-" + field.getName()); 

				} else {
					if (columns.contains(field.getName())) {
						row[columnIndexMap.get(field.getName())] = field.get(o).toString();
					}
				}				
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}

		return row;
	}
}
