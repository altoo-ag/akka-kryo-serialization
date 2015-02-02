/**
 * *****************************************************************************
 * Copyright 2013 Roman Levenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package com.romix.scala.serialization.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.serializers.FieldSerializer
import com.esotericsoftware.kryo.ClassResolver
import com.esotericsoftware.kryo.ReferenceResolver
import com.esotericsoftware.kryo.StreamFactory

class ScalaKryo(classResolver: ClassResolver, referenceResolver: ReferenceResolver, streamFactory: StreamFactory)
  extends Kryo(classResolver, referenceResolver, streamFactory) {

  lazy val objSer = new ObjectSerializer[AnyRef]

  override def getDefaultSerializer(`type`: Class[_]): Serializer[_] = {
    if(isSingleton(`type`)) {
      objSer
    } else {
      super.getDefaultSerializer(`type`)
    }
  }

  override def newDefaultSerializer(klass: Class[_]): Serializer[_] = {
    if (isSingleton(klass)) {
      objSer
    } else {
      super.newDefaultSerializer(klass) match {
        case fs: FieldSerializer[_] =>
          //Scala has a lot of synthetic fields that must be serialized:
          //We also enable it by default in java since not wanting these fields
          //serialized looks like the exception rather than the rule.
          fs.setIgnoreSyntheticFields(false)
          fs
        case x: Serializer[_] => x
      }
    }
  }

  /**
   * return true if this class is a scala "object"
   */
  def isSingleton(klass: Class[_]): Boolean =
    klass.getName.last == '$' && objSer.accepts(klass)
}

