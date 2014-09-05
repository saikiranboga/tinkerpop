package com.tinkerpop.gremlin.process.graph;

import com.tinkerpop.gremlin.structure.MetaProperty;
import com.tinkerpop.gremlin.structure.Property;

import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface MetaPropertyTraversal extends ElementTraversal<MetaProperty> {

    public default <E2> GraphTraversal<MetaProperty, Property<E2>> properties(final String... propertyKeys) {
        return (GraphTraversal) this.start().properties(propertyKeys);
    }

    public default <E2> GraphTraversal<MetaProperty, Map<String, Property<E2>>> propertyMap(final String... propertyKeys) {
        return this.start().propertyMap(propertyKeys);
    }

    public default <E2> GraphTraversal<MetaProperty, Property<E2>> hiddens(final String... propertyKeys) {
        return (GraphTraversal) this.start().hiddens(propertyKeys);
    }

    public default <E2> GraphTraversal<MetaProperty, Map<String, Property<E2>>> hiddenMap(final String... propertyKeys) {
        return (GraphTraversal) this.start().hiddenMap(propertyKeys);
    }

    public default <E2> GraphTraversal<MetaProperty, Map<String, E2>> valueMap(final String... propertyKeys) {
        return this.start().valueMap(propertyKeys);
    }

    public default <E2> GraphTraversal<MetaProperty, Map<String, E2>> hiddenValueMap(final String... propertyKeys) {
        return this.start().hiddenValueMap(propertyKeys);
    }
}
