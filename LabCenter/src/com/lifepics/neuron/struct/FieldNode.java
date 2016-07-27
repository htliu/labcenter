/*
 * FieldNode.java
 */

package com.lifepics.neuron.struct;

import com.lifepics.neuron.meta.Accessor;

import java.util.Collection;

/**
 * A public, data-driven version of the field tree.  To see the difference,
 * look at QueueList.queues as an example.  In the private, data-free version,
 * all we know is that it has some number of children that are all stored
 * in XML under the name Queue.  In the public, data-driven version, we want
 * to look at some actual config file and say that it has this many children
 * with these actual queue names.
 *
 * To do that, FieldNodes are either static or dynamic.  Static ones are not
 * data-driven, dynamic ones are.  In the dynamic case, the children will be
 * some new subclass of FieldNode that can be compared for equality and thus
 * merged for comparison across multiple config files.
 */

public interface FieldNode extends Accessor {

   // about the implementation of Accessor, there are two cases.  in one,
   // getFieldClass returns FieldNode.class and get returns an object
   // that the caller can only do two things with.  one is, check for nulls.
   // null means this FieldNode is not defined in this config file.
   // the other is, when it's not null, pass it to the get functions of the
   // node's children.  you always need to check for nulls!

   // it's not just dynamic nodes that can return null; NullableStructureField
   // can do it too.

   // in the other case, getFieldClass returns some subclass of AtomicField.
   // in that case, the object is some atomic type (Integer, String, File, etc.)
   // that the caller can display and edit.  getFieldClass does not return
   // things like Integer.class because we need to distinguish between fields
   // that are and aren't nullable.  (this wasn't an issue in EditSKUDialog
   // because there null is always allowed -- all nulls means no entry defined.)

   // an important difference between get and getChildren: if you think of
   // a pair (fn,o), you call fn.getChildren(o) to get children fn_i,
   // then you call fn_i.get(o) to get o_i.  so, in that case the fn and object
   // don't match up.

   // to put it all another way, here's the right way to iterate over FieldNode.
   // you start with a pair (fn,o), and you apply this three-way case statement
   // to that and to all children.
   //
   // if (fn.getFieldClass() != FieldNode.class) | atomic field, no children
   // else if (o == null)                        | can't iterate further
   // else                                       | can and should iterate further

   // note on who implements what, because it's a bit confusing.  AbstractField
   // implements getName, on the AtomicField side, the superclass implements
   // isDynamic and getChildren, the subclasses implement getFieldClass and get.
   // on the CompositeField side it's exactly reversed.  I'm not sure how the
   // dynamic children will fit into this scheme.

   // random notes about this whole system that don't fit anywhere else:
   //
   // * getFieldClass can't just return getClass because there are lots of
   // unnamed subclasses in the structure definitions.
   //
   // * there are some heavyweight subclasses outside the struct directory,
   // currently User.RecordField and the unnamed one of Queue.formatConfig.
   // don't forget these when you're making changes to the hierarchy!
   //
   // * in tget and tset, the "t" stands for "typed"; I had to rename them
   // to avoid collision with Accessor.get.
   //
   // * Java's Field.get and set have some typecasting slack that I don't like.
   // that's why I reimplemented get in terms of tget in each class instead of
   // just having an AtomicField implementation in terms of Field.get.
   //
   // * I decided it wasn't worth implementing get in terms of tget on the
   // CompositeField side.  thus, CompositeField.get is redundant, oh well.
   //
   // * we now have two sets of accessors, these struct ones and also the ones
   // in the classes like RollUtil.  the reason I won't go back and remove all
   // the old ones is, I think they have to be slightly faster since they don't
   // use reflection.

   /**
    * @return The node name, same as the XML name except for dynamic nodes.
    */
   String getName();

   /**
    * @return True if getChildren needs to be called for every config file.
    * So, technically areChildrenDynamic would be the better name for this.
    */
   boolean isDynamic();

   /**
    * @param o The config object, used only in the dynamic case but should
    * always be filled in.
    *
    * @return The children (of type FieldNode) as a collection.  Null is allowed
    * in place of an empty collection.
    */
   Collection getChildren(Object o);

}

