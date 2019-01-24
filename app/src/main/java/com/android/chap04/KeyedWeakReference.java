//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.android.chap04;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

final class KeyedWeakReference extends WeakReference<Object> {
    public final String key;
    public final String name;

    KeyedWeakReference(Object referent, String key, String name, ReferenceQueue<Object> referenceQueue) {
        super(Preconditions.checkNotNull(referent, "referent"), (ReferenceQueue)Preconditions.checkNotNull(referenceQueue, "referenceQueue"));
        this.key = (String)Preconditions.checkNotNull(key, "key");
        this.name = (String)Preconditions.checkNotNull(name, "name");
    }
}
