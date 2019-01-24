//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.android.chap04;

import com.squareup.haha.perflib.Instance;
import com.squareup.leakcanary.Exclusion;

final class LeakNode {
    final Exclusion exclusion;
    final Instance instance;
    final LeakNode parent;
    final LeakReference leakReference;

    LeakNode(Exclusion exclusion, Instance instance, LeakNode parent, LeakReference leakReference) {
        this.exclusion = exclusion;
        this.instance = instance;
        this.parent = parent;
        this.leakReference = leakReference;
    }
}
