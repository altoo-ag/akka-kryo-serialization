package io.altoo.akka.serialization.kryo.serializer.scala

import java.util.Collections

import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.util.DefaultClassResolver

class SubclassResolver extends DefaultClassResolver {

  /**
   * We don't want to do subclass resolution during the Kryo.register() call, and unfortunately it
   * hits this a lot. So this doesn't get turned on until the KryoSerializer explicitly enables it,
   * at the end of Kryo setup.
   */
  private var enabled = false

  def enable(): Unit = enabled = true

  /**
   * Keep track of the Types we've tried to look up and failed, to reduce wasted effort.
   */
  private val unregisteredTypes = Collections.newSetFromMap[Class[_]](new java.util.WeakHashMap())

  /**
   * Given Class clazz, this recursively walks up the reflection tree and collects all of its
   * ancestors, so we can check whether any of them are registered.
   */
  def findRegistered(clazz: Class[_]): Option[Registration] = {
    if (clazz == null || unregisteredTypes.contains(clazz))
      // Hit the top, so give up
      None
    else {
      val reg = classToRegistration.get(clazz)
      if (reg == null) {
        val result =
          findRegistered(clazz.getSuperclass) orElse
          clazz.getInterfaces.foldLeft(Option.empty[Registration]) { (res, interf) =>
            res orElse findRegistered(interf)
          }
        if (result.isEmpty) {
          unregisteredTypes.add(clazz)
        }
        result
      } else {
        Some(reg)
      }
    }
  }

  override def getRegistration(tpe: Class[_]): Registration = {
    val found = super.getRegistration(tpe)
    if (enabled && found == null) {
      findRegistered(tpe) match {
        case Some(reg) =>
          // Okay, we've found an ancestor registration. Add that registration for the current type, so
          // it'll be efficient later. (This isn't threadsafe, but a given Kryo instance isn't anyway.)
          classToRegistration.put(tpe, reg)
          reg

        case None => null
      }
    } else
      found
  }
}
