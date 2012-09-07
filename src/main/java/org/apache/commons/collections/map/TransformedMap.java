/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.collections.Transformer;

/**
 * Decorates another <code>Map</code> to transform objects that are added.
 * <p>
 * The Map put methods and Map.Entry setValue method are affected by this class.
 * Thus objects must be removed or searched for using their transformed form.
 * For example, if the transformation converts Strings to Integers, you must
 * use the Integer form to remove objects.
 * <p>
 * <strong>Note that TransformedMap is not synchronized and is not thread-safe.</strong>
 * If you wish to use this map from multiple threads concurrently, you must use
 * appropriate synchronization. The simplest approach is to wrap this map
 * using {@link java.util.Collections#synchronizedMap(Map)}. This class may throw
 * exceptions when accessed by concurrent threads without synchronization.
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 * <p>
 * @see org.apache.commons.collections.splitmap.TransformedMap
 *
 * @since 3.0
 * @version $Id$
 */
public class TransformedMap<K, V>
        extends AbstractInputCheckedMapDecorator<K, V>
        implements Serializable {

    /** Serialization version */
    private static final long serialVersionUID = 7023152376788900464L;

    /** The transformer to use for the key */
    protected final Transformer<? super K, ? extends K> keyTransformer;
    /** The transformer to use for the value */
    protected final Transformer<? super V, ? extends V> valueTransformer;

    /**
     * Factory method to create a transforming map.
     * <p>
     * If there are any elements already in the map being decorated, they
     * are NOT transformed.
     * Contrast this with {@link #transformedMap(Map, Transformer, Transformer)}.
     *
     * @param <K>  the key type
     * @param <V>  the value type
     * @param map  the map to decorate, must not be null
     * @param keyTransformer  the transformer to use for key conversion, null means no transformation
     * @param valueTransformer  the transformer to use for value conversion, null means no transformation
     * @return a new transformed map
     * @throws IllegalArgumentException if map is null
     */
    public static <K, V> TransformedMap<K, V> transformingMap(Map<K, V> map,
            Transformer<? super K, ? extends K> keyTransformer,
            Transformer<? super V, ? extends V> valueTransformer) {
        return new TransformedMap<K, V>(map, keyTransformer, valueTransformer);
    }

    /**
     * Factory method to create a transforming map that will transform
     * existing contents of the specified map.
     * <p>
     * If there are any elements already in the map being decorated, they
     * will be transformed by this method.
     * Contrast this with {@link #transformingMap(Map, Transformer, Transformer)}.
     *
     * @param <K>  the key type
     * @param <V>  the value type
     * @param map  the map to decorate, must not be null
     * @param keyTransformer  the transformer to use for key conversion, null means no transformation
     * @param valueTransformer  the transformer to use for value conversion, null means no transformation
     * @return a new transformed map
     * @throws IllegalArgumentException if map is null
     * @since 3.2
     */
    public static <K, V> TransformedMap<K, V> transformedMap(Map<K, V> map,
            Transformer<? super K, ? extends K> keyTransformer,
            Transformer<? super V, ? extends V> valueTransformer) {
        TransformedMap<K, V> decorated = new TransformedMap<K, V>(map, keyTransformer, valueTransformer);
        if (map.size() > 0) {
            Map<K, V> transformed = decorated.transformMap(map);
            decorated.clear();
            decorated.decorated().putAll(transformed);  // avoids double transformation
        }
        return decorated;
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies).
     * <p>
     * If there are any elements already in the collection being decorated, they
     * are NOT transformed.
     *
     * @param map  the map to decorate, must not be null
     * @param keyTransformer  the transformer to use for key conversion, null means no conversion
     * @param valueTransformer  the transformer to use for value conversion, null means no conversion
     * @throws IllegalArgumentException if map is null
     */
    protected TransformedMap(Map<K, V> map, Transformer<? super K, ? extends K> keyTransformer,
            Transformer<? super V, ? extends V> valueTransformer) {
        super(map);
        this.keyTransformer = keyTransformer;
        this.valueTransformer = valueTransformer;
    }

    //-----------------------------------------------------------------------
    /**
     * Write the map out using a custom routine.
     *
     * @param out  the output stream
     * @throws IOException
     * @since 3.1
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(map);
    }

    /**
     * Read the map in using a custom routine.
     *
     * @param in  the input stream
     * @throws IOException
     * @throws ClassNotFoundException
     * @since 3.1
     */
    @SuppressWarnings("unchecked") // (1) should only fail if input stream is incorrect 
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        map = (Map<K, V>) in.readObject(); // (1)
    }

    //-----------------------------------------------------------------------
    /**
     * Transforms a key.
     * <p>
     * The transformer itself may throw an exception if necessary.
     *
     * @param object  the object to transform
     * @return the transformed object
     */
    protected K transformKey(K object) {
        if (keyTransformer == null) {
            return object;
        }
        return keyTransformer.transform(object);
    }

    /**
     * Transforms a value.
     * <p>
     * The transformer itself may throw an exception if necessary.
     *
     * @param object  the object to transform
     * @return the transformed object
     */
    protected V transformValue(V object) {
        if (valueTransformer == null) {
            return object;
        }
        return valueTransformer.transform(object);
    }

    /**
     * Transforms a map.
     * <p>
     * The transformer itself may throw an exception if necessary.
     *
     * @param map  the map to transform
     * @return the transformed object
     */
    @SuppressWarnings("unchecked")
    protected Map<K, V> transformMap(Map<? extends K, ? extends V> map) {
        if (map.isEmpty()) {
            return (Map<K, V>) map;
        }
        Map<K, V> result = new LinkedMap<K, V>(map.size());

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result.put(transformKey(entry.getKey()), transformValue(entry.getValue()));
        }
        return result;
    }

    /**
     * Override to transform the value when using <code>setValue</code>.
     *
     * @param value  the value to transform
     * @return the transformed value
     * @since 3.1
     */
    @Override
    protected V checkSetValue(V value) {
        return valueTransformer.transform(value);
    }

    /**
     * Override to only return true when there is a value transformer.
     *
     * @return true if a value transformer is in use
     * @since 3.1
     */
    @Override
    protected boolean isSetValueChecking() {
        return (valueTransformer != null);
    }

    //-----------------------------------------------------------------------
    @Override
    public V put(K key, V value) {
        key = transformKey(key);
        value = transformValue(value);
        return decorated().put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> mapToCopy) {
        mapToCopy = transformMap(mapToCopy);
        decorated().putAll(mapToCopy);
    }

}
