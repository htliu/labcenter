/*
 * FormatDLSStub.java
 */

package com.lifepics.neuron.dendron;

import com.lifepics.neuron.core.Text;

/**
 * A stub class that lets us do everything except actually format
 * print jobs without loading the DLS libraries.  For consistency
 * with other external jar situations, this should be FormatDLS
 * and the other FormatDLSImpl, but it's not worth a full renaming.
 *
 * The class {@link StaticDLS} does sort of the same thing, but it
 * doesn't make sense to me to merge it in here.
 */

public class FormatDLSStub extends Format {

// --- subclass hooks ---

   public String getShortName() { return Text.get(FormatDLSStub.class,"s1"); }

   public int[] getAllowedCompletionModes() { return new int[] { COMPLETION_MODE_MANUAL, COMPLETION_MODE_AUTO }; }
   public int   getCompletionMode(Object formatConfig) { return ((DLSConfig) formatConfig).completeImmediately ? COMPLETION_MODE_AUTO : COMPLETION_MODE_MANUAL; }
   public void  setCompletionMode(Object formatConfig, int mode) { ((DLSConfig) formatConfig).completeImmediately = (mode == COMPLETION_MODE_AUTO); }

// --- format function ---

   public void format(Job job, Order order, Object formatConfig) throws Exception {

      try {
         new FormatDLS().format(job,order,formatConfig);
         // FYI, the FormatDLS class has no construction overhead,
         // so it's not a big deal to create a new one every time.
      } catch (NoClassDefFoundError e) {
         throw new Exception(Text.get(FormatDLSStub.class,"e1")); // "this" would work too
      }

      // the way the process works is, when FormatDLSStub is linked,
      // its symbolic references to other classes are resolved to check
      // that they're correct.  but, the references from those other
      // classes are *not* checked, so FormatDLSStub can finish linking
      // even though it refers to FormatDLS and FormatDLS refers to
      // things that might not be there.  FormatDLS itself won't link
      // until we actively use it, here by constructing an instance.
      //
      // the whole thing works just as well if we declare a FormatDLS
      // member variable as a cache ... that's still just a reference.
      // (of course we still have to construct it inside the try-catch block.)
      // the only reason I'm not doing that is, FormatDLS inherits
      // from FormatDLSStub, so it would contain an unused pointer to a copy
      // of itself ... creepy!
      //
      // the above is how the process *does* work, but it's important
      // to note that that's not part of the language specification.
      // the VM may choose not to resolve references (in which case we
      // wouldn't have this problem), or it may choose to resolve them
      // recursively, in which case the whole app might fail to load,
      // or we just have an uncatchable random-point-in-time failure.
      // (it's not totally random, but who knows where it might happen.)
      //
      // anyway, the point is, if the VM chooses not to resolve right
      // away, it's possible that FormatDLS itself could load even if
      // the KIAS jar is missing, and we wouldn't see the NoClassDef
      // error until we actually ran the format function.  thus, it's
      // important that both the FormatDLS construction and the call
      // to the format function are inside the try-catch block.
      //
      // another reason that's important is that there are other jars
      // besides the KIAS one.  if those are missing, we might load
      // the KIAS classes successfully and still get a NoClassDef from
      // inside the format function.
   }

// --- completion ---

   public boolean isCompleteWithoutDirectory(String property, Special special) throws Exception {
      return (property != null);
      // not null means job should complete immediately
   }

}

