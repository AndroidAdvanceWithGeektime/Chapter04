//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.android.chap04;

import com.android.chap04.LeakTraceElement.Type;
import com.squareup.haha.perflib.ArrayInstance;
import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Field;
import com.squareup.haha.perflib.HahaSpy;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.RootObj;
import com.squareup.haha.perflib.RootType;
import com.squareup.haha.perflib.Snapshot;
import com.squareup.haha.perflib.ClassInstance.FieldValue;
import com.squareup.leakcanary.ExcludedRefs;
import com.squareup.leakcanary.Exclusion;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;

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
        this.toVisitQueue = new ArrayDeque();
        this.toVisitIfNoPathQueue = new ArrayDeque();
        this.toVisitSet = new LinkedHashSet();
        this.toVisitIfNoPathSet = new LinkedHashSet();
        this.visitedSet = new LinkedHashSet();
    }

    ShortestPathFinder.Result findPath(Snapshot snapshot, Instance leakingRef) {
        this.clearState();
        this.canIgnoreStrings = !this.isString(leakingRef);
        this.enqueueGcRoots(snapshot);
        boolean excludingKnownLeaks = false;
        LeakNode leakingNode = null;

        while(!this.toVisitQueue.isEmpty() || !this.toVisitIfNoPathQueue.isEmpty()) {
            LeakNode node;
            if (!this.toVisitQueue.isEmpty()) {
                node = (LeakNode)this.toVisitQueue.poll();
            } else {
                node = (LeakNode)this.toVisitIfNoPathQueue.poll();
                if (node.exclusion == null) {
                    throw new IllegalStateException("Expected node to have an exclusion " + node);
                }

                excludingKnownLeaks = true;
            }

            if (node.instance == leakingRef) {
                leakingNode = node;
                break;
            }

            if (!this.checkSeen(node)) {
                if (node.instance instanceof RootObj) {
                    this.visitRootObj(node);
                } else if (node.instance instanceof ClassObj) {
                    this.visitClassObj(node);
                } else if (node.instance instanceof ClassInstance) {
                    this.visitClassInstance(node);
                } else {
                    if (!(node.instance instanceof ArrayInstance)) {
                        throw new IllegalStateException("Unexpected type for " + node.instance);
                    }

                    this.visitArrayInstance(node);
                }
            }
        }

        return new ShortestPathFinder.Result(leakingNode, excludingKnownLeaks);
    }

    private void clearState() {
        this.toVisitQueue.clear();
        this.toVisitIfNoPathQueue.clear();
        this.toVisitSet.clear();
        this.toVisitIfNoPathSet.clear();
        this.visitedSet.clear();
    }

    private void enqueueGcRoots(Snapshot snapshot) {
        Iterator var2 = HahaSpy.allGcRoots(snapshot).iterator();

        while(var2.hasNext()) {
            RootObj rootObj = (RootObj)var2.next();
            switch(rootObj.getRootType()) {
                case JAVA_LOCAL:
                    Instance thread = HahaSpy.allocatingThread(rootObj);
                    String threadName = HahaHelper.threadName(thread);
                    Exclusion params = (Exclusion)this.excludedRefs.threadNames.get(threadName);
                    if (params == null || !params.alwaysExclude) {
                        this.enqueue(params, (LeakNode)null, rootObj, (LeakReference)null);
                    }
                case INTERNED_STRING:
                case DEBUGGER:
                case INVALID_TYPE:
                case UNREACHABLE:
                case UNKNOWN:
                case FINALIZING:
                    break;
                case SYSTEM_CLASS:
                case VM_INTERNAL:
                case NATIVE_LOCAL:
                case NATIVE_STATIC:
                case THREAD_BLOCK:
                case BUSY_MONITOR:
                case NATIVE_MONITOR:
                case REFERENCE_CLEANUP:
                case NATIVE_STACK:
                case JAVA_STATIC:
                    this.enqueue((Exclusion)null, (LeakNode)null, rootObj, (LeakReference)null);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown root type:" + rootObj.getRootType());
            }
        }

    }

    private boolean checkSeen(LeakNode node) {
        return !this.visitedSet.add(node.instance);
    }

    private void visitRootObj(LeakNode node) {
        RootObj rootObj = (RootObj)node.instance;
        Instance child = rootObj.getReferredInstance();
        if (rootObj.getRootType() == RootType.JAVA_LOCAL) {
            Instance holder = HahaSpy.allocatingThread(rootObj);
            Exclusion exclusion = null;
            if (node.exclusion != null) {
                exclusion = node.exclusion;
            }

            LeakNode parent = new LeakNode((Exclusion)null, holder, (LeakNode)null, (LeakReference)null);
            this.enqueue(exclusion, parent, child, new LeakReference(Type.LOCAL, (String)null, (String)null));
        } else {
            this.enqueue((Exclusion)null, node, child, (LeakReference)null);
        }

    }

    private void visitClassObj(LeakNode node) {
        ClassObj classObj = (ClassObj)node.instance;
        Map<String, Exclusion> ignoredStaticFields = (Map)this.excludedRefs.staticFieldNameByClassName.get(classObj.getClassName());
        Iterator var4 = classObj.getStaticFieldValues().entrySet().iterator();

        while(var4.hasNext()) {
            Entry<Field, Object> entry = (Entry)var4.next();
            Field field = (Field)entry.getKey();
            if (field.getType() == com.squareup.haha.perflib.Type.OBJECT) {
                String fieldName = field.getName();
                if (!fieldName.equals("$staticOverhead")) {
                    Instance child = (Instance)entry.getValue();
                    boolean visit = true;
                    String fieldValue = entry.getValue() == null ? "null" : entry.getValue().toString();
                    LeakReference leakReference = new LeakReference(Type.STATIC_FIELD, fieldName, fieldValue);
                    if (ignoredStaticFields != null) {
                        Exclusion params = (Exclusion)ignoredStaticFields.get(fieldName);
                        if (params != null) {
                            visit = false;
                            if (!params.alwaysExclude) {
                                this.enqueue(params, node, child, leakReference);
                            }
                        }
                    }

                    if (visit) {
                        this.enqueue((Exclusion)null, node, child, leakReference);
                    }
                }
            }
        }

    }

    private void visitClassInstance(LeakNode node) {
        ClassInstance classInstance = (ClassInstance)node.instance;
        Map<String, Exclusion> ignoredFields = new LinkedHashMap();
        ClassObj superClassObj = classInstance.getClassObj();

        Exclusion classExclusion;
        for(classExclusion = null; superClassObj != null; superClassObj = superClassObj.getSuperClassObj()) {
            Exclusion params = (Exclusion)this.excludedRefs.classNames.get(superClassObj.getClassName());
            if (params != null && (classExclusion == null || !classExclusion.alwaysExclude)) {
                classExclusion = params;
            }

            Map<String, Exclusion> classIgnoredFields = (Map)this.excludedRefs.fieldNameByClassName.get(superClassObj.getClassName());
            if (classIgnoredFields != null) {
                ignoredFields.putAll(classIgnoredFields);
            }
        }

        if (classExclusion == null || !classExclusion.alwaysExclude) {
            Iterator var14 = classInstance.getValues().iterator();

            while(true) {
                Exclusion fieldExclusion;
                Field field;
                FieldValue fieldValue;
                do {
                    if (!var14.hasNext()) {
                        return;
                    }

                    fieldValue = (FieldValue)var14.next();
                    fieldExclusion = classExclusion;
                    field = fieldValue.getField();
                } while(field.getType() != com.squareup.haha.perflib.Type.OBJECT);

                Instance child = (Instance)fieldValue.getValue();
                String fieldName = field.getName();
                Exclusion params = (Exclusion)ignoredFields.get(fieldName);
                if (params != null && (classExclusion == null || params.alwaysExclude && !classExclusion.alwaysExclude)) {
                    fieldExclusion = params;
                }

                String value = fieldValue.getValue() == null ? "null" : fieldValue.getValue().toString();
                this.enqueue(fieldExclusion, node, child, new LeakReference(Type.INSTANCE_FIELD, fieldName, value));
            }
        }
    }

    private void visitArrayInstance(LeakNode node) {
        ArrayInstance arrayInstance = (ArrayInstance)node.instance;
        com.squareup.haha.perflib.Type arrayType = arrayInstance.getArrayType();
        if (arrayType == com.squareup.haha.perflib.Type.OBJECT) {
            Object[] values = arrayInstance.getValues();

            for(int i = 0; i < values.length; ++i) {
                Instance child = (Instance)values[i];
                String name = Integer.toString(i);
                String value = child == null ? "null" : child.toString();
                this.enqueue((Exclusion)null, node, child, new LeakReference(Type.ARRAY_ENTRY, name, value));
            }
        }

    }

    private void enqueue(Exclusion exclusion, LeakNode parent, Instance child, LeakReference leakReference) {
        if (child != null) {
            if (!HahaHelper.isPrimitiveOrWrapperArray(child) && !HahaHelper.isPrimitiveWrapper(child)) {
                if (!this.toVisitSet.contains(child)) {
                    boolean visitNow = exclusion == null;
                    if (visitNow || !this.toVisitIfNoPathSet.contains(child)) {
                        if (!this.canIgnoreStrings || !this.isString(child)) {
                            if (!this.visitedSet.contains(child)) {
                                LeakNode childNode = new LeakNode(exclusion, child, parent, leakReference);
                                if (visitNow) {
                                    this.toVisitSet.add(child);
                                    this.toVisitQueue.add(childNode);
                                } else {
                                    this.toVisitIfNoPathSet.add(child);
                                    this.toVisitIfNoPathQueue.add(childNode);
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isString(Instance instance) {
        return instance.getClassObj() != null && instance.getClassObj().getClassName().equals(String.class.getName());
    }

    static final class Result {
        final LeakNode leakingNode;
        final boolean excludingKnownLeaks;

        Result(LeakNode leakingNode, boolean excludingKnownLeaks) {
            this.leakingNode = leakingNode;
            this.excludingKnownLeaks = excludingKnownLeaks;
        }
    }
}
