package com.hprof.bitmap;/*
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

import com.squareup.haha.perflib.*;
import com.squareup.leakcanary.ExcludedRefs;
import com.squareup.leakcanary.Exclusion;
import com.squareup.leakcanary.LeakReference;

import java.util.*;

import static com.squareup.leakcanary.LeakTraceElement.Type.*;

/**
 * Not thread safe.
 *
 * Finds the shortest path from a leaking reference to a gc root, ignoring excluded
 * refs first and then including the ones that are not "always ignorable" as needed if no path is
 * found.
 */
final class ShortestPathFinder {

    private final ExcludedRefs excludedRefs;
    private final Deque<LeakNode> toVisitQueue;
    private final Deque<LeakNode> toVisitIfNoPathQueue;
    private final LinkedHashSet<Instance> toVisitSet;
    private final LinkedHashSet<Instance> toVisitIfNoPathSet;
    private final LinkedHashSet<Instance> visitedSet;
    private boolean canIgnoreStrings;

    ShortestPathFinder(ExcludedRefs excludedRefs) {
        this.excludedRefs = excludedRefs;
        toVisitQueue = new ArrayDeque<>();
        toVisitIfNoPathQueue = new ArrayDeque<>();
        toVisitSet = new LinkedHashSet<>();
        toVisitIfNoPathSet = new LinkedHashSet<>();
        visitedSet = new LinkedHashSet<>();
    }

    static final class Result {
        final LeakNode leakingNode;
        final boolean excludingKnownLeaks;

        Result(LeakNode leakingNode, boolean excludingKnownLeaks) {
            this.leakingNode = leakingNode;
            this.excludingKnownLeaks = excludingKnownLeaks;
        }
    }

    Result findPath(Snapshot snapshot, Instance leakingRef) {
        clearState();
        canIgnoreStrings = !isString(leakingRef);

        enqueueGcRoots(snapshot);

        boolean excludingKnownLeaks = false;
        LeakNode leakingNode = null;
        while (!toVisitQueue.isEmpty() || !toVisitIfNoPathQueue.isEmpty()) {
            LeakNode node;
            if (!toVisitQueue.isEmpty()) {
                node = toVisitQueue.poll();
            } else {
                node = toVisitIfNoPathQueue.poll();
                if (node.exclusion == null) {
                    throw new IllegalStateException("Expected node to have an exclusion " + node);
                }
                excludingKnownLeaks = true;
            }

            // Termination
            if (node.instance == leakingRef) {
                leakingNode = node;
                break;
            }

            if (checkSeen(node)) {
                continue;
            }

            if (node.instance instanceof RootObj) {
                visitRootObj(node);
            } else if (node.instance instanceof ClassObj) {
                visitClassObj(node);
            } else if (node.instance instanceof ClassInstance) {
                visitClassInstance(node);
            } else if (node.instance instanceof ArrayInstance) {
                visitArrayInstance(node);
            } else {
                throw new IllegalStateException("Unexpected type for " + node.instance);
            }
        }
        return new Result(leakingNode, excludingKnownLeaks);
    }

    private void clearState() {
        toVisitQueue.clear();
        toVisitIfNoPathQueue.clear();
        toVisitSet.clear();
        toVisitIfNoPathSet.clear();
        visitedSet.clear();
    }

    private void enqueueGcRoots(Snapshot snapshot) {
        for (RootObj rootObj : snapshot.getGCRoots()) {
            switch (rootObj.getRootType()) {
                case JAVA_LOCAL:
                case INTERNED_STRING:
                case DEBUGGER:
                case INVALID_TYPE:
                    // An object that is unreachable from any other root, but not a root itself.
                case UNREACHABLE:
                case UNKNOWN:
                    // An object that is in a queue, waiting for a finalizer to run.
                case FINALIZING:
                    break;
                case SYSTEM_CLASS:
                case VM_INTERNAL:
                    // A local variable in native code.
                case NATIVE_LOCAL:
                    // A global variable in native code.
                case NATIVE_STATIC:
                    // An object that was referenced from an active thread block.
                case THREAD_BLOCK:
                    // Everything that called the wait() or notify() methods, or that is synchronized.
                case BUSY_MONITOR:
                case NATIVE_MONITOR:
                case REFERENCE_CLEANUP:
                    // Input or output parameters in native code.
                case NATIVE_STACK:
                case JAVA_STATIC:
                    enqueue(null, null, rootObj, null);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown root type:" + rootObj.getRootType());
            }
        }
    }

    private boolean checkSeen(LeakNode node) {
        return !visitedSet.add(node.instance);
    }

    private void visitRootObj(LeakNode node) {
        RootObj rootObj = (RootObj) node.instance;
        Instance child = rootObj.getReferredInstance();

        if (rootObj.getRootType() == RootType.JAVA_LOCAL) {
            Instance holder = HahaSpy.allocatingThread(rootObj);
            // We switch the parent node with the thread instance that holds
            // the local reference.
            Exclusion exclusion = null;
            if (node.exclusion != null) {
                exclusion = node.exclusion;
            }
            LeakNode parent = new LeakNode(null, holder, null, null);
            enqueue(exclusion, parent, child, new LeakReference(LOCAL, null, null));
        } else {
            enqueue(null, node, child, null);
        }
    }

    private void visitClassObj(LeakNode node) {
        ClassObj classObj = (ClassObj) node.instance;
        for (Map.Entry<Field, Object> entry : classObj.getStaticFieldValues().entrySet()) {
            Field field = entry.getKey();
            if (field.getType() != Type.OBJECT) {
                continue;
            }
            String fieldName = field.getName();
            if (fieldName.equals("$staticOverhead")) {
                continue;
            }
            Instance child = (Instance) entry.getValue();
            String fieldValue = entry.getValue() == null ? "null" : entry.getValue().toString();
            LeakReference leakReference = new LeakReference(STATIC_FIELD, fieldName, fieldValue);
            enqueue(null, node, child, leakReference);
        }
    }

    private void visitClassInstance(LeakNode node) {
        ClassInstance classInstance = (ClassInstance) node.instance;
        ClassObj superClassObj = classInstance.getClassObj();
        while (superClassObj != null) {

            superClassObj = superClassObj.getSuperClassObj();
        }

        for (ClassInstance.FieldValue fieldValue : classInstance.getValues()) {
            Field field = fieldValue.getField();
            if (field.getType() != Type.OBJECT) {
                continue;
            }
            Instance child = (Instance) fieldValue.getValue();
            String fieldName = field.getName();
            // If we found a field exclusion and it's stronger than a class exclusion
            String value = fieldValue.getValue() == null ? "null" : fieldValue.getValue().toString();
            enqueue(null, node, child, new LeakReference(INSTANCE_FIELD, fieldName, value));
        }
    }

    private void visitArrayInstance(LeakNode node) {
        ArrayInstance arrayInstance = (ArrayInstance) node.instance;
        Type arrayType = arrayInstance.getArrayType();
        if (arrayType == Type.OBJECT) {
            Object[] values = arrayInstance.getValues();
            for (int i = 0; i < values.length; i++) {
                Instance child = (Instance) values[i];
                String name = Integer.toString(i);
                String value = child == null ? "null" : child.toString();
                enqueue(null, node, child, new LeakReference(ARRAY_ENTRY, name, value));
            }
        }
    }

    private void enqueue(Exclusion exclusion, LeakNode parent, Instance child,
                         LeakReference leakReference) {
        if (child == null) {
            return;
        }
        if (HahaHelper.isPrimitiveOrWrapperArray(child) || HahaHelper.isPrimitiveWrapper(child)) {
            return;
        }
        // Whether we want to visit now or later, we should skip if this is already to visit.
        if (toVisitSet.contains(child)) {
            return;
        }
        boolean visitNow = exclusion == null;
        if (!visitNow && toVisitIfNoPathSet.contains(child)) {
            return;
        }
        if (canIgnoreStrings && isString(child)) {
            return;
        }
        if (visitedSet.contains(child)) {
            return;
        }
        LeakNode childNode = new LeakNode(exclusion, child, parent, leakReference);
        if (visitNow) {
            toVisitSet.add(child);
            toVisitQueue.add(childNode);
        } else {
            toVisitIfNoPathSet.add(child);
            toVisitIfNoPathQueue.add(childNode);
        }
    }

    private boolean isString(Instance instance) {
        return instance.getClassObj() != null && instance.getClassObj()
                .getClassName()
                .equals(String.class.getName());
    }
}
