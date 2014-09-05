package com.tinkerpop.gremlin.structure;

import com.tinkerpop.gremlin.process.Path;
import com.tinkerpop.gremlin.process.T;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.Traverser;
import com.tinkerpop.gremlin.process.computer.GraphComputer;
import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.process.graph.step.sideEffect.StartStep;
import com.tinkerpop.gremlin.util.StreamFactory;
import com.tinkerpop.gremlin.util.function.SBiConsumer;
import com.tinkerpop.gremlin.util.function.SBiFunction;
import com.tinkerpop.gremlin.util.function.SBiPredicate;
import com.tinkerpop.gremlin.util.function.SConsumer;
import com.tinkerpop.gremlin.util.function.SFunction;
import com.tinkerpop.gremlin.util.function.SPredicate;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;

/**
 * An {@link Element} is the base class for both {@link Vertex} and {@link Edge}. An {@link Element} has an identifier
 * that must be unique to its inheriting classes ({@link Vertex} or {@link Edge}). An {@link Element} can maintain a
 * collection of {@link Property} objects.  Typically, objects are Java primitives (e.g. String, long, int, boolean,
 * etc.)
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract interface Element {

    public static final String ID = "id";
    public static final String LABEL = "label";

    /**
     * Gets the unique identifier for the graph {@code Element}.
     *
     * @return The id of the element
     */
    public Object id();

    /**
     * Gets the label for the graph {@code Element} which helps categorize it.
     *
     * @return The label of the element
     */
    public String label();


    /**
     * Get the keys from non-hidden properties.
     *
     * @return The non-hidden key set
     */
    public default Set<String> keys() {
        final Set<String> keys = new HashSet<>();
        this.iterators().properties().forEachRemaining(property -> keys.add(property.key()));
        return keys;
    }

    /**
     * Get the keys of hidden properties.
     *
     * @return The hidden key set
     */
    public default Set<String> hiddenKeys() {
        final Set<String> hiddenKeys = new HashSet<>();
        this.iterators().hiddens().forEachRemaining(property -> hiddenKeys.add(Graph.Key.unHide(property.key())));
        return hiddenKeys;
    }

    /**
     * Get a {@link Property} for the {@code Element} given its key.  Hidden properties can be retrieved by specifying
     * the key as {@link com.tinkerpop.gremlin.structure.Graph.Key#hide}.
     */
    public default <V> Property<V> property(final String key) {
        final Iterator<? extends Property<V>> iterator = this.iterators().properties(key);
        return iterator.hasNext() ? iterator.next() : Property.<V>empty();
    }

    /**
     * Add or set a property value for the {@code Element} given its key.  Hidden properties can be set by specifying
     * the key as {@link com.tinkerpop.gremlin.structure.Graph.Key#hide}.
     */
    public <V> Property<V> property(final String key, final V value);

    /**
     * Get the value of a {@link Property} given it's key.
     *
     * @throws NoSuchElementException if the property does not exist on the {@code Element}.
     */
    public default <V> V value(final String key) throws NoSuchElementException {
        final Property<V> property = this.property(key);
        return property.orElseThrow(() -> Property.Exceptions.propertyDoesNotExist(key));
    }

    public default <V> V value(final String key, final V orElse) {
        final Property<V> property = this.property(key);
        return property.orElse(orElse);
    }

    /**
     * Removes the {@code Element} from the graph.
     */
    public void remove();

    public Element.Iterators iterators();

    public interface Iterators {

        /**
         * Get the values of non-hidden properties as a {@link Map} of keys and values.
         */
        public default <V> Iterator<V> values(final String... propertyKeys) {
            return StreamFactory.stream(this.properties(propertyKeys)).map(property -> (V) property.value()).iterator();
        }

        /**
         * Get the values of hidden properties as a {@link Map} of keys and values.
         */
        public default <V> Iterator<V> hiddenValues(final String... propertyKeys) {
            return StreamFactory.stream(this.hiddens(propertyKeys)).map(property -> (V) property.value()).iterator();
        }

        /**
         * Get an {@link Iterator} of non-hidden properties.
         */
        public <V> Iterator<? extends Property<V>> properties(final String... propertyKeys);

        /**
         * Get an {@link Iterator} of hidden properties.
         */
        public <V> Iterator<? extends Property<V>> hiddens(final String... propertyKeys);
    }

    /**
     * Common exceptions to use with an element.
     */
    public static class Exceptions {

        public static IllegalArgumentException providedKeyValuesMustBeAMultipleOfTwo() {
            return new IllegalArgumentException("The provided key/value array must be a multiple of two");
        }

        public static IllegalArgumentException providedKeyValuesMustHaveALegalKeyOnEvenIndices() {
            return new IllegalArgumentException("The provided key/value array must have a String key on even array indices");
        }

        public static IllegalStateException propertyAdditionNotSupported() {
            return new IllegalStateException("Property addition is not supported");
        }

        public static IllegalStateException propertyRemovalNotSupported() {
            return new IllegalStateException("Property removal is not supported");
        }
    }
}
