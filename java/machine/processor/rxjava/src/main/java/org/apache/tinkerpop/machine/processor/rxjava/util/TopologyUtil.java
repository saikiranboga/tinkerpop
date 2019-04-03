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
package org.apache.tinkerpop.machine.processor.rxjava.util;

import io.reactivex.Flowable;
import org.apache.tinkerpop.machine.bytecode.compiler.Compilation;
import org.apache.tinkerpop.machine.function.BranchFunction;
import org.apache.tinkerpop.machine.function.CFunction;
import org.apache.tinkerpop.machine.function.FilterFunction;
import org.apache.tinkerpop.machine.function.FlatMapFunction;
import org.apache.tinkerpop.machine.function.InitialFunction;
import org.apache.tinkerpop.machine.function.MapFunction;
import org.apache.tinkerpop.machine.function.ReduceFunction;
import org.apache.tinkerpop.machine.function.branch.RepeatBranch;
import org.apache.tinkerpop.machine.processor.rxjava.BranchFlow;
import org.apache.tinkerpop.machine.processor.rxjava.FilterFlow;
import org.apache.tinkerpop.machine.processor.rxjava.FlatMapFlow;
import org.apache.tinkerpop.machine.processor.rxjava.MapFlow;
import org.apache.tinkerpop.machine.processor.rxjava.ReduceFlow;
import org.apache.tinkerpop.machine.traverser.Traverser;
import org.apache.tinkerpop.machine.traverser.TraverserFactory;
import org.apache.tinkerpop.machine.util.IteratorUtils;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class TopologyUtil {

    private static final int MAX_ITERATIONS = 20;

    public static <C, S, E> Flowable<Traverser<C, E>> compile(final Flowable<Traverser<C, S>> source, final Compilation<C, S, E> compilation) {
        final TraverserFactory<C> traverserFactory = compilation.getTraverserFactory();
        Flowable<Traverser<C, E>> sink = (Flowable) source;
        for (final CFunction<C> function : compilation.getFunctions()) {
            sink = TopologyUtil.extend(sink, function, traverserFactory);
        }
        return sink;
    }

    private static <C, S, E, B> Flowable<Traverser<C, E>> extend(Flowable<Traverser<C, S>> flow, final CFunction<C> function, final TraverserFactory<C> traverserFactory) {
        if (function instanceof MapFunction)
            return flow.map(new MapFlow<>((MapFunction<C, S, E>) function));
        else if (function instanceof FilterFunction) {
            return (Flowable) flow.filter(new FilterFlow<>((FilterFunction<C, S>) function));
        } else if (function instanceof FlatMapFunction) {
            return flow.flatMapIterable(new FlatMapFlow<>((FlatMapFunction<C, S, E>) function));
        } else if (function instanceof InitialFunction) {
            return Flowable.fromIterable(() -> IteratorUtils.map(((InitialFunction<C, E>) function).get(), s -> traverserFactory.create(function, s)));
        } else if (function instanceof ReduceFunction) {
            final ReduceFunction<C, S, E> reduceFunction = (ReduceFunction<C, S, E>) function;
            return flow.reduce(traverserFactory.create(reduceFunction, reduceFunction.getInitialValue()), new ReduceFlow<>(reduceFunction)).toFlowable();
        } else if (function instanceof BranchFunction) {
            final Flowable<List> selectorFlow = flow.map(new BranchFlow<>((BranchFunction<C, S, B>) function));
            final List<Publisher<Traverser<C, E>>> branchFlows = new ArrayList<>();
            for (final Map.Entry<Compilation<C, S, ?>, List<Compilation<C, S, E>>> branches : ((BranchFunction<C, S, E>) function).getBranches().entrySet()) {
                for (int i = 0; i < branches.getValue().size(); i++) {
                    final Compilation<C, S, E> branch = branches.getValue().get(i);
                    final int id = i;
                    branchFlows.add(compile(selectorFlow.
                                    filter(list -> list.get(0).equals(null == branches.getKey() ? -1 : id)).
                                    map(list -> (Traverser<C, S>) list.get(1)),
                            branch));
                }
            }
            Flowable<Traverser<C, E>> sink = (Flowable) flow.filter(t -> false); // branches are the only outputs
            for (final Publisher<Traverser<C, E>> branchFlow : branchFlows) {
                sink = sink.mergeWith(branchFlow);
            }
            return sink;
        } else if (function instanceof RepeatBranch) {
            final RepeatBranch<C, S> repeatBranch = (RepeatBranch<C, S>) function;
            final List<Publisher<Traverser<C, S>>> outputs = new ArrayList<>();
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                Flowable<List> selectorFlow = flow.flatMapIterable(t -> {
                    final List<List> list = new ArrayList<>();
                    if (repeatBranch.hasStartPredicates()) {
                        if (1 == repeatBranch.getUntilLocation()) {
                            if (repeatBranch.getUntil().filterTraverser(t)) {
                                list.add(List.of(0, t.repeatDone(repeatBranch)));
                            } else if (2 == repeatBranch.getEmitLocation() && repeatBranch.getEmit().filterTraverser(t)) {
                                list.add(List.of(1, t));
                                list.add(List.of(0, t.repeatDone(repeatBranch)));
                            } else
                                list.add(List.of(1, t));
                        } else if (1 == repeatBranch.getEmitLocation()) {
                            if (repeatBranch.getEmit().filterTraverser(t))
                                list.add(List.of(0, t.repeatDone(repeatBranch)));
                            if (2 == repeatBranch.getUntilLocation() && repeatBranch.getUntil().filterTraverser(t)) {
                                list.add(List.of(0, t.repeatDone(repeatBranch)));
                            } else
                                list.add(List.of(1, t));
                        }
                    } else
                        list.add(List.of(1, t));
                    return list;
                });
                outputs.add(selectorFlow.filter(list -> list.get(0).equals(0)).map(list -> (Traverser<C, S>) list.get(1)));
                flow = compile(selectorFlow.filter(list -> list.get(0).equals(1)).map(list -> (Traverser<C, S>) list.get(1)), repeatBranch.getRepeat());
                selectorFlow = flow.flatMapIterable(t -> {
                    final List<List> list = new ArrayList<>();
                    if (repeatBranch.hasEndPredicates()) {
                        if (3 == repeatBranch.getUntilLocation()) {
                            if (repeatBranch.getUntil().filterTraverser(t)) {
                                list.add(List.of(0, t.repeatDone(repeatBranch)));
                            } else if (4 == repeatBranch.getEmitLocation() && repeatBranch.getEmit().filterTraverser(t)) {
                                list.add(List.of(0, t.repeatDone(repeatBranch)));
                                list.add(List.of(1, t.repeatLoop(repeatBranch)));
                            } else
                                list.add(List.of(1, t.repeatLoop(repeatBranch)));
                        } else if (3 == repeatBranch.getEmitLocation()) {
                            if (repeatBranch.getEmit().filterTraverser(t))
                                list.add(List.of(0, t.repeatDone(repeatBranch)));
                            if (4 == repeatBranch.getUntilLocation() && repeatBranch.getUntil().filterTraverser(t))
                                list.add(List.of(0, t.repeatDone(repeatBranch)));
                            else
                                list.add(List.of(1, t.repeatLoop(repeatBranch)));
                        }
                    } else
                        list.add(List.of(1, t.repeatLoop(repeatBranch)));
                    return list;
                });
                outputs.add(selectorFlow.filter(list -> list.get(0).equals(0)).map(list -> (Traverser<C, S>) list.get(1)));
                flow = selectorFlow.filter(list -> list.get(0).equals(1)).map(list -> (Traverser<C, S>) list.get(1));
            }
            Flowable<Traverser<C, S>> sink = flow.filter(t -> false); // branches are the only outputs
            for (final Publisher<Traverser<C, S>> output : outputs) {
                sink = sink.mergeWith(output);
            }
            return (Flowable) sink;
        }
        throw new RuntimeException("Need a new execution plan step: " + function);
    }
}