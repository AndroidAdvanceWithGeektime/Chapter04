package com.squareup.leakcanary;

import android.support.annotation.NonNull;

public interface AnalyzerProgressListener {

  @NonNull AnalyzerProgressListener NONE = new AnalyzerProgressListener() {
  };

}