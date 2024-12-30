/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
