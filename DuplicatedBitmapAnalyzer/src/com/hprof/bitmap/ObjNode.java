package com.hprof.bitmap;

import com.squareup.haha.perflib.Instance;

import java.util.ArrayList;

/**
 * Created by shengmingxu on 2018/12/11.
 */
public class ObjNode {
    private Instance instance;
    private ArrayList<Instance> trace;

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public ArrayList<Instance> getTrace() {
        return trace;
    }

    public void setTrace(ArrayList<Instance> trace) {
        this.trace = trace;
    }

    public String getTraceString(){
        StringBuilder stringBuilder = new StringBuilder();
        if(trace != null && trace.size() > 0){
            for (Instance instance : trace){
                stringBuilder.append(instance.getClassObj().getClassName());
                stringBuilder.append("\n");
            }
        }
        return stringBuilder.toString();
    }
}
