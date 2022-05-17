/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.bench.java.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Param;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(1)
public class DoubleClassCheck {

    RandomGenerator rng;
    static final int BUFFER_SIZE = 1024;
    double[] inputs;
    boolean[] outputs;

    @Setup
    public void setup() {
        outputs = new boolean[BUFFER_SIZE];
        inputs = new double[BUFFER_SIZE];
        RandomGenerator rng = RandomGeneratorFactory.getDefault().create(0);
        double input;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            if (i % 5 == 0) {
                input = (i%2 == 0) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            }
            else if (i % 3 == 0) input = Double.NaN;
            else input = rng.nextDouble();
            inputs[i] = input;
        }
    }

    @Benchmark
    @OperationsPerInvocation(BUFFER_SIZE)
    public void testIsFinite() {
        for (int i = 0; i < BUFFER_SIZE; i++) {
            outputs[i] = Double.isFinite(inputs[i]) ? false : true;
        }
    }

    @Benchmark
    @OperationsPerInvocation(BUFFER_SIZE)
    public void testIsInfinite() {
        for (int i = 0; i < BUFFER_SIZE; i++) {
            outputs[i] = Double.isInfinite(inputs[i]) ? false : true;
        }
    }

    @Benchmark
    @OperationsPerInvocation(BUFFER_SIZE)
    public void testIsNaN() {
        for (int i = 0; i < BUFFER_SIZE; i++) {
            outputs[i] = Double.isNaN(inputs[i]) ? false : true;
        }
    }
}
