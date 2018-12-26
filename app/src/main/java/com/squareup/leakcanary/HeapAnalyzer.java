/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.leakcanary;

import android.support.annotation.NonNull;

import com.squareup.haha.perflib.ArrayInstance;
import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Field;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.RootObj;
import com.squareup.haha.perflib.Snapshot;
import com.squareup.haha.perflib.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.squareup.leakcanary.HahaHelper.extendsThread;
import static com.squareup.leakcanary.HahaHelper.threadName;
import static com.squareup.leakcanary.HahaHelper.valueAsString;
import static com.squareup.leakcanary.LeakTraceElement.Holder.ARRAY;
import static com.squareup.leakcanary.LeakTraceElement.Holder.CLASS;
import static com.squareup.leakcanary.LeakTraceElement.Holder.OBJECT;
import static com.squareup.leakcanary.LeakTraceElement.Holder.THREAD;
import static com.squareup.leakcanary.LeakTraceElement.Type.ARRAY_ENTRY;
import static com.squareup.leakcanary.LeakTraceElement.Type.INSTANCE_FIELD;
import static com.squareup.leakcanary.LeakTraceElement.Type.STATIC_FIELD;
import static com.squareup.leakcanary.Reachability.REACHABLE;
import static com.squareup.leakcanary.Reachability.UNKNOWN;
import static com.squareup.leakcanary.Reachability.UNREACHABLE;

public final class HeapAnalyzer {

	private static final String ANONYMOUS_CLASS_NAME_PATTERN = "^.+\\$\\d+$";

	private final ExcludedRefs excludedRefs;

	public HeapAnalyzer(@NonNull ExcludedRefs excludedRefs) {
		this.excludedRefs = excludedRefs;
	}

	public HeapAnalyzer() {
		excludedRefs = ExcludedRefs.builder().build();
	}

	/**
	 * 查询引用链
	 *
	 * @param snapshot snapshot
	 * @param instance 实例
	 */
	public LeakTrace findLeakTrace(Snapshot snapshot, Instance instance) {
		ShortestPathFinder pathFinder = new ShortestPathFinder(excludedRefs);
		ShortestPathFinder.Result result = pathFinder.findPath(snapshot, instance);
		return buildLeakTrace(result.leakingNode);
	}

	private LeakTrace buildLeakTrace(LeakNode leakingNode) {
		List<LeakTraceElement> elements = new ArrayList<>();
		// We iterate from the leak to the GC root
		LeakNode node = new LeakNode(null, null, leakingNode, null);
		while (node != null) {
			LeakTraceElement element = buildLeakElement(node);
			if (element != null) {
				elements.add(0, element);
			}
			node = node.parent;
		}

		List<Reachability> expectedReachability = computeExpectedReachability(elements);

		return new LeakTrace(elements, expectedReachability);
	}

	private List<Reachability> computeExpectedReachability(
			List<LeakTraceElement> elements) {
		int lastReachableElement = 0;
		int lastElementIndex = elements.size() - 1;
		int firstUnreachableElement = lastElementIndex;

		List<Reachability> expectedReachability = new ArrayList<>();
		for (int i = 0; i < elements.size(); i++) {
			Reachability status;
			if (i <= lastReachableElement) {
				status = REACHABLE;
			} else if (i >= firstUnreachableElement) {
				status = UNREACHABLE;
			} else {
				status = UNKNOWN;
			}
			expectedReachability.add(status);
		}
		return expectedReachability;
	}

	private LeakTraceElement buildLeakElement(LeakNode node) {
		if (node.parent == null) {
			// Ignore any root node.
			return null;
		}
		Instance holder = node.parent.instance;

		if (holder instanceof RootObj) {
			return null;
		}
		LeakTraceElement.Holder holderType;
		String className;
		String extra = null;
		List<LeakReference> leakReferences = describeFields(holder);

		className = getClassName(holder);

		List<String> classHierarchy = new ArrayList<>();
		classHierarchy.add(className);
		String rootClassName = Object.class.getName();
		if (holder instanceof ClassInstance) {
			ClassObj classObj = holder.getClassObj();
			while (!(classObj = classObj.getSuperClassObj()).getClassName().equals(rootClassName)) {
				classHierarchy.add(classObj.getClassName());
			}
		}

		if (holder instanceof ClassObj) {
			holderType = CLASS;
		} else if (holder instanceof ArrayInstance) {
			holderType = ARRAY;
		} else {
			ClassObj classObj = holder.getClassObj();
			if (extendsThread(classObj)) {
				holderType = THREAD;
				String threadName = threadName(holder);
				extra = "(named '" + threadName + "')";
			} else if (className.matches(ANONYMOUS_CLASS_NAME_PATTERN)) {
				String parentClassName = classObj.getSuperClassObj().getClassName();
				if (rootClassName.equals(parentClassName)) {
					holderType = OBJECT;
					try {
						// This is an anonymous class implementing an interface. The API does not give access
						// to the interfaces implemented by the class. We check if it's in the class path and
						// use that instead.
						Class<?> actualClass = Class.forName(classObj.getClassName());
						Class<?>[] interfaces = actualClass.getInterfaces();
						if (interfaces.length > 0) {
							Class<?> implementedInterface = interfaces[0];
							extra = "(anonymous implementation of " + implementedInterface.getName() + ")";
						} else {
							extra = "(anonymous subclass of java.lang.Object)";
						}
					} catch (ClassNotFoundException ignored) {
					}
				} else {
					holderType = OBJECT;
					// Makes it easier to figure out which anonymous class we're looking at.
					extra = "(anonymous subclass of " + parentClassName + ")";
				}
			} else {
				holderType = OBJECT;
			}
		}
		return new LeakTraceElement(node.leakReference, holderType, classHierarchy, extra,
				node.exclusion, leakReferences);
	}

	private List<LeakReference> describeFields(Instance instance) {
		List<LeakReference> leakReferences = new ArrayList<>();
		if (instance instanceof ClassObj) {
			ClassObj classObj = (ClassObj) instance;
			for (Map.Entry<Field, Object> entry : classObj.getStaticFieldValues().entrySet()) {
				String name = entry.getKey().getName();
				String stringValue = valueAsString(entry.getValue());
				leakReferences.add(new LeakReference(STATIC_FIELD, name, stringValue));
			}
		} else if (instance instanceof ArrayInstance) {
			ArrayInstance arrayInstance = (ArrayInstance) instance;
			if (arrayInstance.getArrayType() == Type.OBJECT) {
				Object[] values = arrayInstance.getValues();
				for (int i = 0; i < values.length; i++) {
					String name = Integer.toString(i);
					String stringValue = valueAsString(values[i]);
					leakReferences.add(new LeakReference(ARRAY_ENTRY, name, stringValue));
				}
			}
		} else {
			ClassObj classObj = instance.getClassObj();
			for (Map.Entry<Field, Object> entry : classObj.getStaticFieldValues().entrySet()) {
				String name = entry.getKey().getName();
				String stringValue = valueAsString(entry.getValue());
				leakReferences.add(new LeakReference(STATIC_FIELD, name, stringValue));
			}
			ClassInstance classInstance = (ClassInstance) instance;
			for (ClassInstance.FieldValue field : classInstance.getValues()) {
				String name = field.getField().getName();
				String stringValue = valueAsString(field.getValue());
				leakReferences.add(new LeakReference(INSTANCE_FIELD, name, stringValue));
			}
		}
		return leakReferences;
	}

	private String getClassName(Instance instance) {
		String className;
		if (instance instanceof ClassObj) {
			ClassObj classObj = (ClassObj) instance;
			className = classObj.getClassName();
		} else if (instance instanceof ArrayInstance) {
			ArrayInstance arrayInstance = (ArrayInstance) instance;
			className = arrayInstance.getClassObj().getClassName();
		} else {
			ClassObj classObj = instance.getClassObj();
			className = classObj.getClassName();
		}
		return className;
	}

}
