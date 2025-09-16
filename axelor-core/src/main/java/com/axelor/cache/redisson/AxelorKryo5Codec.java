/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer.FieldSerializerConfig;
import org.apache.shiro.session.mgt.SimpleSession;
import org.redisson.codec.Kryo5Codec;

/** Kryo5Codec implementation with any required custom configuration. */
public class AxelorKryo5Codec extends Kryo5Codec {

  @Override
  protected Kryo createKryo(ClassLoader classLoader, boolean useReferences)
      throws ClassNotFoundException {
    var kryo = super.createKryo(classLoader, useReferences);
    var sessionSerializerConfig = new FieldSerializerConfig();

    // Required because org.apache.shiro.session.mgt.SimpleSession uses custom
    // serialization of transient fields.
    sessionSerializerConfig.setSerializeTransient(true);

    var sessionSerializer =
        new FieldSerializer<SimpleSession>(kryo, SimpleSession.class, sessionSerializerConfig);
    kryo.addDefaultSerializer(SimpleSession.class, sessionSerializer);

    return kryo;
  }
}
