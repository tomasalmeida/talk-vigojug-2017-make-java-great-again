/**
 * GCTest.java
 * Testing garbage collection behavior.
 * Suggested JVM options: -Xms2m -Xmx8m
 * Copyright (c) 2003 by Dr. Herong Yang, herongyang.com
 time  java -Xms2m -Xmx16m -XX:-UseConcMarkSweepGC GCTest
 time  java -Xms2m -Xmx16m -XX:-UseParallelOldGC GCTest #(crash)
 time  java -Xms2m -Xmx16m -XX:+UseG1GC GCTest
 time  java -Xms2m -Xmx16m -XX:-UseParallelGC GCTest
 time  java -Xms2m -Xmx16m -XX:-UseSerialGC GCTest
 */
import java.util.*;
class GCTest {
   static MyList objList = null;
   static int wait = 50; // milliseconds
   static int initSteps = 32; // 2 MB
   static int testSteps = 1;
   static int objSize = 256; // 1/8 MB

   public static void main(String[] arg) {
      if (arg.length>0) initSteps = Integer.parseInt(arg[0]);
      if (arg.length>1) testSteps = Integer.parseInt(arg[1]);
      objList = new MyList();
      Monitor m = new Monitor();
      m.setDaemon(true);
      m.start();
      myTest();
   }
   public static void myTest() {
      for (int m=0; m<initSteps; m++) {
         mySleep(wait);
         objList.add(new MyObject());
      }
      for (int n=0; n<10*8*8/testSteps; n++) {
         for (int m=0; m<testSteps; m++) {
            mySleep(wait);
            objList.add(new MyObject());
         }
         for (int m=0; m<testSteps; m++) {
            mySleep(wait);
            objList.removeTail();
            // objList.removeHead();
         }
      }
   }
   static void mySleep(int t) {
      try {
         Thread.sleep(t);
      } catch (InterruptedException e) {
         System.out.println("Interreupted...");
      }
   }
   static class MyObject {
      private static long count = 0;
      private long[] obj = null;
      public MyObject next = null;
      public MyObject prev = null;
      public MyObject() {
         count++;
         obj = new long[objSize*128];
      }
      protected final void finalize() {
         count--;
      }
      static long getCount() {
         return count;
      }
   }
   static class MyList {
      static long count = 0;
      MyObject head = null;
      MyObject tail = null;
      static long getCount() {
         return count;
      }
      void add(MyObject o) {
      	 // add the new object to the head;
         if (head==null) {
            head = o;
            tail = o;
         } else {
            o.prev = head;
            head.next = o;
            head = o;
         }
         count++;
      }
      void removeTail() {
      	 if (tail!=null) {
      	    if (tail.next==null) {
      	       tail = null;
      	       head = null;
      	    } else {
      	       tail = tail.next;
      	       tail.prev = null;
      	    }
      	    count--;
      	 }
      }
      void removeHead() {
      	 if (head!=null) {
      	    if (head.prev==null) {
      	       tail = null;
      	       head = null;
      	    } else {
      	       head = head.prev;
      	       head.next = null;
      	    }
      	    count--;
      	 }
      }
   }
   static class Monitor extends Thread {
      public void run() {
         Runtime rt = Runtime.getRuntime();
         System.out.println(
            "Time\tTotal\tFree\tFree\tTotal\tAct.\tDead\tOver");
         System.out.println(
            "sec.\t Mem.\tMem.\tPer.\t Obj.\tObj.\tObj.\tHead");
         long dt0 = System.currentTimeMillis()/1000;
         while (true) {
            long tm = rt.totalMemory()/1024;
            long fm = rt.freeMemory()/1024;
            long ratio = (100*fm)/tm;
            long dt = System.currentTimeMillis()/1000 - dt0;
            long to = MyObject.getCount()*objSize;
            long ao = MyList.getCount()*objSize;
            System.out.println(dt
               + "\t" + tm + "\t" + fm + "\t" + ratio +"%"
               + "\t" + to + "\t" + ao + "\t" + (to-ao)
               + "\t" + (tm-fm-to));
            mySleep(wait);
      	 }
      }
   }
}
