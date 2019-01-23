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
package com.squareuo.haha.perflib;

import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.RootObj;
import com.squareup.haha.perflib.Snapshot;
import com.squareup.haha.perflib.StackTrace;
import com.squareup.haha.perflib.ThreadObj;

import java.lang.reflect.Field;

public final class HahaSpy {

  public static Instance allocatingThread(Instance instance) {
    Snapshot snapshot = (Snapshot) reflectField(instance.getHeap(),"mSnapshot");
    int threadSerialNumber;
    if (instance instanceof RootObj) {
      threadSerialNumber = (int) reflectField(((RootObj)instance),"mThread");
    } else {
      StackTrace stackTrace = (StackTrace) reflectField(instance,"mStack");
      threadSerialNumber = (int) reflectField(stackTrace, "mThreadSerialNumber");
    }
    ThreadObj thread = snapshot.getThread(threadSerialNumber);
    return snapshot.findInstance((Long) reflectField(thread,"mId"));
  }

  private static Object reflectField(Object object, String fieldName){
    if(object == null){
      return null;
    }
    try {
      Field field = object.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(object);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    return null;
  }

  private HahaSpy() {
    throw new AssertionError();
  }
}
