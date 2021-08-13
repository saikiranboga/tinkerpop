/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.tinkergraph;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import io.cucumber.guice.CucumberModules;
import io.cucumber.guice.GuiceFactory;
import io.cucumber.guice.InjectorSource;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.TestHelper;
import org.apache.tinkerpop.gremlin.features.World;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.runner.RunWith;

import java.io.File;

import static org.apache.tinkerpop.gremlin.LoadGraphWith.GraphData;

@RunWith(Cucumber.class)
@CucumberOptions(
        tags = "not @RemoteOnly",
        glue = { "org.apache.tinkerpop.gremlin.features" },
        objectFactory = GuiceFactory.class,
        features = { "../gremlin-test/features" },
        plugin = {"pretty", "junit:target/cucumber.xml"})
public class TinkerGraphFeatureTest {

    public static final class ServiceModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(World.class).to(TinkerGraphWorld.class);
        }
    }

    public static class TinkerGraphWorld implements World {
        private static final TinkerGraph modern = TinkerFactory.createModern();
        private static final TinkerGraph classic = TinkerFactory.createClassic();
        private static final TinkerGraph crew = TinkerFactory.createTheCrew();
        private static final TinkerGraph sink = TinkerFactory.createKitchenSink();
        private static final TinkerGraph grateful = TinkerFactory.createGratefulDead();

        @Override
        public GraphTraversalSource getGraphTraversalSource(final GraphData graphData) {
            if (null == graphData)
                return TinkerGraph.open(getNumberIdManagerConfiguration()).traversal();
            else if (graphData == GraphData.CLASSIC)
                return classic.traversal();
            else if (graphData == GraphData.CREW)
                return crew.traversal();
            else if (graphData == GraphData.MODERN)
                return modern.traversal();
            else if (graphData == GraphData.SINK)
                return sink.traversal();
            else if (graphData == GraphData.GRATEFUL)
                return grateful.traversal();
            else
                throw new UnsupportedOperationException("GraphData not supported: " + graphData.name());
        }

        @Override
        public String changePathToDataFile(final String pathToFileFromGremlin) {
            return ".." + File.separator + pathToFileFromGremlin;
        }
    }

    public static final class WorldInjectorSource implements InjectorSource {
        @Override
        public Injector getInjector() {
            return Guice.createInjector(Stage.PRODUCTION, CucumberModules.createScenarioModule(), new ServiceModule());
        }
    }

    private static Configuration getNumberIdManagerConfiguration() {
        final Configuration conf = new BaseConfiguration();
        conf.setProperty(TinkerGraph.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER, TinkerGraph.DefaultIdManager.INTEGER.name());
        conf.setProperty(TinkerGraph.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER, TinkerGraph.DefaultIdManager.INTEGER.name());
        conf.setProperty(TinkerGraph.GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER, TinkerGraph.DefaultIdManager.LONG.name());
        return conf;
    }
}
