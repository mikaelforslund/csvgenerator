package com.example.demo.csv;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import org.springframework.util.ClassUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CsvGenerator {
	private Map<String, CurrentIterator<?>> iterators = new HashMap<>();
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
				.collect(Collectors.toMap(v -> (String) v.getValue(), i -> (int) i.getIndex()));

	}

	public Stream<String[]> toCsv(Object o) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<String[]>() {

			@Override
			public boolean hasNext() {
				return started == false || started && iterators.size() != 0;
			}

			@Override
			public String[] next() {
				started = !started ? true : started;
				String[] row = new String[columns.size()];
				Arrays.fill(row, "");
				return reflect(o, row, 0);
			}

		}, Spliterator.IMMUTABLE), false);
	}

	private boolean isCollectionType(Field field) {
		return List.class.isAssignableFrom(field.getType());
	}

	private boolean isPrimitiveOrString(Field field) {
		return ClassUtils.isPrimitiveOrWrapper(field.getType()) || field.getType().equals(String.class);
	}

	private List<String> fieldNames = new ArrayList<>();
	private Map<String, Collection<?>> listMap = new HashMap<>();

	private String[] reflect(Object o, String[] row, int level) {
		try {
			// do next row...
			for (var field : o.getClass().getDeclaredFields()) {
				field.setAccessible(true);

				if (excludeList.contains(field.getName()) || field.get(o) == null) {
					continue;
				}

				if (columnProcessorMap.containsKey(field.getName())) {
					columnProcessorMap.get(field.getName()).process(field.get(o), row, columnIndexMap);

				} else if (isCollectionType(field)) {
					if(!fieldNames.contains(field.getName()))
						fieldNames.add(field.getName());

					listMap.put(field.getName(), (List<?>) field.get(o));
					// collect all collection types....
					reflect(listMap.get(field.getName()).iterator().next(), row, level + 1);
					
				} else if (!isPrimitiveOrString(field)) {
					reflect(field.get(o), row, level + 1);

				} else {
					if (columns.contains(field.getName())) {
						row[columnIndexMap.get(field.getName())] = field.get(o).toString();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (level == 0 && processLists(row, fieldNames, listMap, level) == false) {
			// we are done, remove all remaining iterators to signal to the Spliterator that we are done...
			fieldNames.forEach(name ->  iterators.remove(name) );
		}	

		return row;
	}

	private boolean doLeaf(CurrentIterator<?> i, String[] row, int level, String fieldName) { 
		if (i.hasNext()) {
			i.next();
			log.debug("populating with {}", i.getCurrent());
			reflect(i.getCurrent(), row, level + 1);
			return true;
		} else {
			iterators.remove(fieldName);
			return false;
		}
	}

	/**
	 * Generator approach to processing a nested list of lists to visit all element combinations. Useful when traversing an object 
	 * structure to generate an outcome. 
	 * 
	 * TODO generalize this and put in its own util library
	 *  
	 * @param row the row to populate
	 * @param fieldNames all the field names we need to process in a sequential manner
	 * @param listMap map of all lists we need to generate iterators from 
	 * @param level the nested level with 0==top level and fieldNames.length-1==leaf level
	 * @return true if one of the nested iterators hasNext() == true, false otherwise and we are done
	 */
	public boolean processLists(String[] row, List<String> fieldNames, Map<String, Collection<?>> listMap, int level) {
		String fieldName = fieldNames.get(level);
		CurrentIterator<?> i = iterators.get(fieldName);

		// if new...
		if (i == null) {
			i = new CurrentIterator<>(listMap.get(fieldName).iterator());
			iterators.putIfAbsent(fieldName, i);

			// leaf level
			if (level == fieldNames.size() - 1) {
				return doLeaf(i, row, level, fieldName);
			}

			// the other cases..
			if (i.hasNext()) {
				i.next();
				log.debug("populating with {}", i.getCurrent());
				reflect(i.getCurrent(), row, level + 1);
			}
		} else {
			// leaf level
			if (level == fieldNames.size() - 1) {
				return doLeaf(i, row, level, fieldName);
			}

			log.debug("populating with {}", i.getCurrent());
			reflect(i.getCurrent(), row, level + 1);
		}

		if (processLists(row, fieldNames, listMap, level + 1) == false) {
			if (i.hasNext()) {
				i.next();
				log.debug("populating with {}", i.getCurrent());
				reflect(i.getCurrent(), row, level + 1);
				iterators.remove(fieldNames.get(level + 1));
				return processLists(row, fieldNames, listMap, level + 1);
			} else {
				iterators.remove(fieldName);
				return false;
			}
		}

		return iterators.entrySet().stream().anyMatch(e -> e.getValue().hasNext());
	}
}
