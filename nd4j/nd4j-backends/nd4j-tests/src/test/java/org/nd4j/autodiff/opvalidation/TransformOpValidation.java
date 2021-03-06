/*******************************************************************************
 * Copyright (c) 2015-2018 Skymind, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.nd4j.autodiff.opvalidation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.OpValidationSuite;
import org.nd4j.autodiff.functions.DifferentialFunction;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.autodiff.samediff.SameDiffFunctionDefinition;
import org.nd4j.autodiff.validation.OpTestCase;
import org.nd4j.autodiff.validation.OpValidation;
import org.nd4j.autodiff.validation.TestCase;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.linalg.api.ops.Op;
import org.nd4j.linalg.api.ops.impl.layers.convolution.DepthToSpace;
import org.nd4j.linalg.api.ops.impl.layers.convolution.SpaceToDepth;
import org.nd4j.linalg.api.ops.impl.scalar.ScalarFMod;
import org.nd4j.linalg.api.ops.impl.scalar.ScalarMultiplication;
import org.nd4j.linalg.api.ops.impl.shape.Cross;
import org.nd4j.linalg.api.ops.impl.transforms.*;
import org.nd4j.linalg.api.ops.impl.transforms.comparison.GreaterThanOrEqual;
import org.nd4j.linalg.api.ops.impl.transforms.comparison.LessThanOrEqual;
import org.nd4j.linalg.api.ops.impl.transforms.comparison.OldMax;
import org.nd4j.linalg.api.ops.impl.transforms.comparison.OldMin;
import org.nd4j.linalg.api.ops.random.impl.BernoulliDistribution;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.factory.Nd4jBackend;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.indexing.conditions.Condition;
import org.nd4j.linalg.indexing.conditions.Conditions;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.linalg.primitives.Pair;
import org.nd4j.linalg.util.ArrayUtil;
import org.nd4j.nativeblas.NativeOpsHolder;

import java.util.*;

import static org.junit.Assert.*;

@Slf4j
public class TransformOpValidation extends BaseOpValidation {

    private DataBuffer.Type initialType;

    public TransformOpValidation(Nd4jBackend backend) {
        super(backend);
    }

    @Before
    public void before() throws Exception {
        Nd4j.create(1);
        initialType = Nd4j.dataType();

        Nd4j.setDataType(DataBuffer.Type.DOUBLE);
        Nd4j.getRandom().setSeed(123);
    }

    @After
    public void after() throws Exception {
        Nd4j.setDataType(initialType);
    }


    @After
    public void tearDown() throws Exception {
        NativeOpsHolder.getInstance().getDeviceNativeOps().enableDebugMode(false);
        NativeOpsHolder.getInstance().getDeviceNativeOps().enableVerboseMode(false);
    }

    @Test
    public void testScalarOps() {
        int d0 = 2;
        int d1 = 3;
        int d2 = 4;

        int n = d0 * d1 * d2;

        List<String> failed = new ArrayList<>();

        for( int i=0; i<11; i++ ) {
            for (char inOrder : new char[]{'c', 'f'}) {
                SameDiff sd = SameDiff.create();

                INDArray inArr = Nd4j.linspace(1, n, n).reshape('c', d0, d1, d2).dup(inOrder);
                SDVariable in = sd.var("in", inArr);
                TestCase tc = new TestCase(sd).gradientCheck(true);

                SDVariable out;
                String msg;
                switch (i){
                    case 0:
                        out = in.mul(2);
                        tc.expectedOutput(out.getVarName(), inArr.mul(2));
                        msg = "mul - " + inOrder;
                        break;
                    case 1:
                        out = in.div(2);
                        tc.expectedOutput(out.getVarName(), inArr.div(2));
                        msg = "div - " + inOrder;
                        break;
                    case 2:
                        out = in.add(2);
                        tc.expectedOutput(out.getVarName(), inArr.add(2));
                        msg = "add - " + inOrder;
                        break;
                    case 3:
                        out = in.sub(2);
                        tc.expectedOutput(out.getVarName(), inArr.sub(2));
                        msg = "sub - " + inOrder;
                        break;
                    case 4:
                        out = in.rdiv(2);
                        tc.expectedOutput(out.getVarName(), inArr.rdiv(2));
                        msg = "rdiv - " + inOrder;
                        break;
                    case 5:
                        out = in.rsub(2);
                        tc.expectedOutput(out.getVarName(), inArr.rsub(2));
                        msg = "rsub - " + inOrder;
                        break;
                    case 6:
                        out = sd.pow(in,2);
                        tc.expectedOutput(out.getVarName(), Transforms.pow(inArr, 2));
                        msg = "pow - " + inOrder;
                        break;
                    case 7:
                        inArr.assign(Nd4j.rand(inArr.shape()).muli(5).subi(2.5));
                        out = sd.scalarFloorMod(in, 2);
                        tc.expected(out, Nd4j.getExecutioner().execAndReturn(new ScalarFMod(inArr.dup(), 2.0)));
                        msg = "scalarRemainer - " + inOrder;
                        break;
                    case 8:
                        inArr.assign(Nd4j.rand(inArr.shape()));
                        out = sd.scalarMax(in, 0.5);
                        tc.expected(out, Transforms.max(inArr.dup(), 0.5));
                        msg = "scalarMax - " + inOrder;
                        break;
                    case 9:
                        inArr.assign(Nd4j.rand(inArr.shape()));
                        out = sd.scalarMin(in, 0.5);
                        tc.expected(out, Transforms.min(inArr.dup(), 0.5));
                        msg = "scalarMin - " + inOrder;
                        break;
                    case 10:
                        out = in.assign(0.5);
                        tc.expected(out, Nd4j.valueArrayOf(inArr.shape(), 0.5));
                        msg = "scalarSet - " + inOrder;
                        break;
                    default:
                        throw new RuntimeException();
                }

                tc.testName(msg);

                SDVariable loss = sd.standardDeviation(out, true);

                log.info("Starting test: " + msg);
                String err = OpValidation.validate(tc, true);
                if(err != null){
                    failed.add(err);
                }
            }
        }
        assertEquals(failed.toString(), 0, failed.size());
    }

    @Test
    public void testScalarMulCF(){

        INDArray in = Nd4j.linspace(1,12,12).reshape('c',3,4);
        INDArray outC = Nd4j.createUninitialized(3,4);
        INDArray outF = Nd4j.createUninitialized(3, 4);

        Nd4j.getExecutioner().exec(new ScalarMultiplication(in, null, outC, in.length(), 2.0));
        Nd4j.getExecutioner().exec(new ScalarMultiplication(in, null, outF, in.length(), 2.0));

        assertEquals(outC, outF);
    }


    @Test
    public void testScalarMulCF2(){

        INDArray in = Nd4j.linspace(1,12,12).reshape('c',3,4);

        INDArray outC = Nd4j.getExecutioner().execAndReturn(new ScalarMultiplication(in.dup('c'), 2.0));
        INDArray outF = Nd4j.getExecutioner().execAndReturn(new ScalarMultiplication(in.dup('f'), 2.0));

        assertEquals(outC, outF);
    }

    @Test
    public void testCross() {
        INDArray a = Nd4j.create(new float[]{4, 2, 1}, new int[]{1, 3});
        INDArray b = Nd4j.create(new float[]{1, 3, 4}, new int[]{1, 3});

        INDArray expOut = Nd4j.create(1, 3);

        val op = new Cross(a, b, expOut);
        Nd4j.getExecutioner().exec(op);

        SameDiff sd = SameDiff.create();

        SDVariable sdA = sd.var("a", expOut.shape());
        SDVariable sdB = sd.var("b", expOut.shape());


        sd.associateArrayWithVariable(a, sdA);
        sd.associateArrayWithVariable(b, sdB);

        SDVariable t = sd.cross("cross", sdA, sdB);
        SDVariable loss = sd.mean("loss", t);

        String err = OpValidation.validate(new TestCase(sd)
                        .expectedOutput("cross", expOut)
                        .gradientCheck(true));
        assertNull(err, err);
    }

    @Test
    public void testSpaceToDepth() {
        Nd4j.getRandom().setSeed(1337);

        int miniBatch = 128;
        int blockSize = 4;
        String dataFormat = "NHWC";
        int isNHWC = dataFormat.equals("NHWC") ? 1 : 0;
        int[] inputShape = new int[]{miniBatch, 2 * blockSize, 2 * blockSize, 1};

        INDArray input = Nd4j.randn(inputShape);
        SameDiff sd = SameDiff.create();
        SDVariable sdInput = sd.var("in", inputShape);

        INDArray expOut = Nd4j.create(miniBatch, 2, 2, blockSize * blockSize);
        DynamicCustomOp op = new SpaceToDepth(input, expOut, blockSize, dataFormat);
        Nd4j.getExecutioner().exec(op);

        sd.associateArrayWithVariable(input, sdInput);

        SDVariable t = sd.spaceToDepth("std", sdInput, blockSize, dataFormat);
        SDVariable loss = sd.mean("loss", t);

        String err = OpValidation.validate(new TestCase(sd)
                .expectedOutput("std", expOut)
                .gradientCheck(true));
        assertNull(err);
    }

    @Test
    public void testDepthToSpace() {
        Nd4j.getRandom().setSeed(1337);

        int miniBatch = 128;
        int blockSize = 4;
        String dataFormat = "NHWC";
        int isNHWC = dataFormat.equals("NHWC") ? 1 : 0;
        int[] inputShape = new int[]{miniBatch, 2, 2, blockSize * blockSize};

        INDArray input = Nd4j.randn(inputShape);
        SameDiff sd = SameDiff.create();
        SDVariable sdInput = sd.var("in", inputShape);

        INDArray expOut = Nd4j.create(miniBatch, 2 * blockSize, 2 * blockSize, 1);
        DynamicCustomOp op = new DepthToSpace(input, expOut, blockSize, dataFormat);
        Nd4j.getExecutioner().exec(op);

        sd.associateArrayWithVariable(input, sdInput);

        SDVariable t = sd.depthToSpace("dts", sdInput, blockSize, dataFormat);
        SDVariable loss = sd.mean("loss", t);

        String err = OpValidation.validate(new TestCase(sd)
                .expectedOutput("dts", expOut)
                .gradientCheck(true));
        assertNull(err, err);
    }

    @Test
    public void testBatchToSpace() {
        Nd4j.getRandom().setSeed(1337);

        int miniBatch = 4;
        int[] inputShape = new int[]{miniBatch, 1, 1, 1};

        int M = 2;
        int[] blockShape = new int[]{M, 1};
        int[] cropShape = new int[]{M, 2};

        INDArray input = Nd4j.randn(inputShape);
        INDArray blocks = Nd4j.create(new float[]{2, 2}, blockShape);
        INDArray crops = Nd4j.create(new float[]{0, 0, 0, 0}, cropShape);

        SameDiff sd = SameDiff.create();

        SDVariable sdInput = sd.var("in", inputShape);

        INDArray expOut = Nd4j.create(1, 2, 2, 1);
        DynamicCustomOp op = DynamicCustomOp.builder("batch_to_space")
                .addInputs(input, blocks, crops)
                .addOutputs(expOut).build();
        Nd4j.getExecutioner().exec(op);

        sd.associateArrayWithVariable(input, sdInput);

        SDVariable t = sd.batchToSpace("bts", sdInput, new int[]{2, 2}, new int[][]{{0, 0}, {0, 0}});
        SDVariable loss = sd.mean("loss", t);

        String err = OpValidation.validate(new TestCase(sd)
                .expectedOutput("bts", expOut)
                .gradientCheck(true));
        assertNull(err, err);
    }

    @Test
    public void testSpaceToBatch() {
        Nd4j.getRandom().setSeed(7331);

        int miniBatch = 4;
        int[] inputShape = new int[]{1, 2, 2, 1};

        int M = 2;
        int[] blockShape = new int[]{M, 1};
        int[] paddingShape = new int[]{M, 2};

        INDArray input = Nd4j.randn(inputShape);
        INDArray blocks = Nd4j.create(new float[]{2, 2}, blockShape);
        INDArray padding = Nd4j.create(new float[]{0, 0, 0, 0}, paddingShape);

        SameDiff sd = SameDiff.create();

        SDVariable sdInput = sd.var("in", inputShape);

        INDArray expOut = Nd4j.create(miniBatch, 1, 1, 1);
        DynamicCustomOp op = DynamicCustomOp.builder("space_to_batch")
                .addInputs(input, blocks, padding)
                .addOutputs(expOut).build();
        Nd4j.getExecutioner().exec(op);

        sd.associateArrayWithVariable(input, sdInput);

        SDVariable t = sd.spaceToBatch("stb", sdInput, new int[]{2, 2}, new int[][]{{0, 0}, {0, 0}});
        SDVariable loss = sd.mean("loss", t);

        String err = OpValidation.validate(new TestCase(sd)
                .expectedOutput("stb", expOut)
                .gradientCheck(true));
        assertNull(err, err);
    }

    @Test
    public void testDynamicPartition() {
        SameDiff sd = SameDiff.create();

        INDArray ia = Nd4j.trueVector(new float[]{4, 3, 5, 7, 8, 0});
        INDArray partitions = Nd4j.trueVector(new float[]{1, 0, 1, 0, 0, 1});
        int numPartitions = 2;

        SDVariable in = sd.var("in", new long[]{6});
        SDVariable sdPartitions = sd.var("partitions", new long[]{6});

        INDArray expOut1 = Nd4j.create(3L);
        INDArray expOut2 = Nd4j.create(3L);
        INDArray[] expOut = new INDArray[]{expOut1, expOut2};

        DynamicCustomOp dynamicPartition = DynamicCustomOp.builder("dynamic_partition")
                .addInputs(ia, partitions)
                .addIntegerArguments(numPartitions)
                .addOutputs(expOut1, expOut2).build();
        Nd4j.getExecutioner().exec(dynamicPartition);

        SDVariable[] parts = sd.dynamicPartition(new String[]{"dp0", "dp1"}, in, sdPartitions, numPartitions);

        // merge the output partitions together again, to retrieve a single
        // tensor and finally a scalar.
        SDVariable t = sd.mergeAdd(parts);
        SDVariable loss = sd.mean("loss", t);

        sd.associateArrayWithVariable(ia, in);
        sd.associateArrayWithVariable(partitions, sdPartitions);

        String err = OpValidation.validate(new TestCase(sd)
                .gradientCheck(true)
                .gradCheckSkipVariables("partitions")
                .expectedOutput("dp0", expOut[0])
                .expectedOutput("dp1", expOut[1])
                .gradientCheck(true));
        assertNull(err, err);
    }

    @Test
    public void testDynamicStitch() {
        SameDiff sd = SameDiff.create();

        INDArray ia = Nd4j.create(new float[]{5, 1, 3}, new long[]{3});
        INDArray ib = Nd4j.create(new float[]{7, 2, 4}, new long[]{3});
        INDArray indexA = Nd4j.create(new float[]{0, 1, 4}, new long[]{3});
        INDArray indexB = Nd4j.create(new float[]{2, 3, 5}, new long[]{3});

        INDArray expOut = Nd4j.create(new long[]{6});

        DynamicCustomOp dynamicStitch = DynamicCustomOp.builder("dynamic_stitch")
                .addInputs(indexA, indexB, ia, ib)
                .addOutputs(expOut).build();
        Nd4j.getExecutioner().exec(dynamicStitch);

        INDArray expOut2 = Nd4j.create(new double[]{5,1,7,2,3,4});
        assertEquals(expOut2, expOut);

        SDVariable in1 = sd.var("in1", ia);
        SDVariable in2 = sd.var("in2", ib);

        SDVariable index1 = sd.var("index1", indexA);
        SDVariable index2 = sd.var("index2", indexB);

        SDVariable t = sd.dynamicStitch("ds", new SDVariable[]{index1, index2}, new SDVariable[]{in1, in2});
        SDVariable loss = sd.standardDeviation("loss", t, true);

        String err = OpValidation.validate(new TestCase(sd)
                .expectedOutput("ds", expOut)
                .gradientCheck(true)
                .gradCheckSkipVariables("index1", "index2")

        );
        assertNull(err);
    }

    @Test
    public void testDiag() {
        SameDiff sd = SameDiff.create();

        INDArray ia = Nd4j.create(new float[]{4, 2}, new int[] {2});
        SDVariable in = sd.var("in", new long[]{2});
        INDArray expOut = Nd4j.create(new int[]{2, 2});
        DynamicCustomOp diag = DynamicCustomOp.builder("diag").addInputs(ia).addOutputs(expOut).build();
        Nd4j.getExecutioner().exec(diag);
        SDVariable t = sd.diag("diag", in);

        SDVariable loss = sd.standardDeviation("loss", t,false,0, 1);

        sd.associateArrayWithVariable(ia, in);

        String err = OpValidation.validate(new TestCase(sd)
                .expectedOutput("diag", expOut)
                .gradientCheck(true));
        assertNull(err, err);
    }

    @Test
    public void testDiagPart() {
        SameDiff sd = SameDiff.create();

        INDArray input = Nd4j.linspace(1,16,16).reshape(4,4);
        INDArray expOut = Nd4j.create(new float[]{1, 6, 11, 16});

        SDVariable in = sd.var("in", input);
        SDVariable t = sd.diagPart("dp", in);

        // dimension is 0 here, because output of diagPart is vector, not matrix
        SDVariable loss = sd.standardDeviation("loss", t, true, 0);

        String err = OpValidation.validate(new TestCase(sd)
                .expectedOutput("dp", expOut)
                .gradientCheck(true));
        assertNull(err, err);
    }

    @Test
    public void testEye(){
        int[] rows = new int[]{3,3,3,3};
        int[] cols = new int[]{3,2,2,2};
        int[][] batch = new int[][]{null, null, {4}, {3,3}};
        INDArray[] expOut = new INDArray[4];

        expOut[0] = Nd4j.eye(3);
        expOut[1] = Nd4j.create(new double[][]{{1,0},{0,1},{0,0}});
        expOut[2] = Nd4j.create(4,3,2);
        for( int i=0; i<4; i++ ){
            expOut[2].get(NDArrayIndex.point(i), NDArrayIndex.all(), NDArrayIndex.all()).assign(expOut[1]);
        }
        expOut[3] = Nd4j.create(3,3,3,2);
        for( int i=0; i<3; i++ ){
            for( int j=0; j<3; j++ ) {
                expOut[3].get(NDArrayIndex.point(i), NDArrayIndex.point(j), NDArrayIndex.all(), NDArrayIndex.all()).assign(expOut[1]);
            }
        }

        for(int i=0; i<3; i++ ) {
            SameDiff sd = SameDiff.create();
            SDVariable eye = sd.eye("e", rows[i], cols[i], batch[i]);

            SDVariable loss = sd.standardDeviation("loss", eye, true);

            String err = OpValidation.validate(new TestCase(sd)
                    .expectedOutput("e", expOut[i])
                    .gradCheckSkipVariables("e")
                    .gradientCheck(true));
            assertNull(err);
        }
    }

    @Test
    public void testEyeShape(){
        DynamicCustomOp dco = DynamicCustomOp.builder("eye")
                .addIntegerArguments(3,3)
                //.addIntegerArguments(-99,3,3) //Also fails
                .build();

        List<long[]> list = Nd4j.getExecutioner().calculateOutputShape(dco);
        assertEquals(1, list.size());   //Fails here - empty list
        assertArrayEquals(new long[]{3,3}, list.get(0));
    }

    @Test
    public void testTransforms() {
        //Test transforms (non-pairwise)
        Nd4j.getRandom().setSeed(12345);

        List<String> allSkipped = new ArrayList<>();

        List<String> allFailed = new ArrayList<>();
        for (int i = 0; i < 80; i++) {

            SameDiff sd = SameDiff.create();

            int nOut = 4;
            int minibatch = 5;
            SDVariable in = sd.var("in", new int[]{-1, nOut});

            INDArray ia = Nd4j.randn(minibatch, nOut);

            int dim;
            SDVariable t;
            TestCase tc = new TestCase(sd);
            boolean stdevLoss = false;
            switch (i) {
                case 0:
                    t = in.add(5.0);
                    tc.expectedOutput(t.getVarName(), ia.add(5.0));
                    break;
                case 1:
                    t = in.sub(5.0);
                    tc.expectedOutput(t.getVarName(), ia.sub(5.0));
                    break;
                case 2:
                    t = in.mul(2.5);
                    tc.expectedOutput(t.getVarName(), ia.mul(2.5));
                    break;
                case 3:
                    t = in.div(4.0);
                    tc.expectedOutput(t.getVarName(), ia.div(4.0));
                    break;
                case 4:
                    t = in.rsub(5.0);
                    tc.expectedOutput(t.getVarName(), ia.rsub(5.0));
                    break;
                case 5:
                    t = in.rdiv(1.0);
                    tc.expectedOutput(t.getVarName(), ia.rdiv(1.0));
                    break;
                case 6:
                    t = sd.pow(in, 2.5);
                    ia = Nd4j.rand(minibatch, nOut);
                    tc.expectedOutput(t.getVarName(), Transforms.pow(ia, 2.5, true));
                    break;
                case 7:
                    t = sd.sigmoid(in);
                    ia = Nd4j.rand(minibatch, nOut).muli(2).subi(1.0);
                    tc.expectedOutput(t.getVarName(), Transforms.sigmoid(ia, true));
                    break;
                case 8:
                    t = sd.tanh(in);
                    ia = Nd4j.rand(minibatch, nOut).muli(2).subi(1.0);
                    tc.expectedOutput(t.getVarName(), Transforms.tanh(ia, true));
                    break;
                case 9:
                    t = sd.tan(in);
                    ia = Nd4j.rand(minibatch, nOut);
                    tc.expectedOutput(t.getVarName(), Transforms.tan(ia));
                    break;
                case 10:
                    t = sd.cos(in);
                    tc.expectedOutput(t.getVarName(), Transforms.cos(ia, true));
                    break;
                case 11:
                    t = sd.sin(in);
                    tc.expectedOutput(t.getVarName(), Transforms.sin(ia, true));
                    break;
                case 12:
                    t = sd.softplus(in);
                    tc.expectedOutput(t.getVarName(), Transforms.softPlus(ia, true));
                    break;
                case 13:
                    t = sd.log(in);
                    ia = Nd4j.rand(minibatch, nOut);
                    tc.expectedOutput(t.getVarName(), Transforms.log(ia, true));
                    break;
                case 14:
                    t = sd.neg(in);
                    tc.expectedOutput(t.getVarName(), ia.neg());
                    break;
                case 15:
                    t = sd.acos(in);
                    ia = Nd4j.rand(minibatch, nOut).muli(1.8).subi(0.9);
                    tc.expectedOutput(t.getVarName(), Transforms.acos(ia, true));
                    break;
                case 16:
                    t = sd.acosh(in);
                    ia = Nd4j.rand(minibatch, nOut).addi(1.01); //Only defined for x >= 1
                    tc.expectedOutput(t.getVarName(), Nd4j.getExecutioner().execAndReturn(new ACosh(ia.dup())));
                    break;
                case 17:
                    t = sd.asin(in);
                    ia = Nd4j.rand(minibatch, nOut).muli(1.8).subi(0.9);
                    tc.expectedOutput(t.getVarName(), Transforms.asin(ia, true));
                    break;
                case 18:
                    t = sd.atan(in);
                    ia = Nd4j.rand(minibatch, nOut).muli(4).subi(2);
                    tc.expectedOutput(t.getVarName(), Transforms.atan(ia, true));
                    break;
                case 19:
                    t = sd.atanh(in);
                    ia = Nd4j.rand(minibatch, nOut).muli(1.8).subi(0.9);
                    tc.expectedOutput(t.getVarName(), Transforms.atanh(ia, true));
                    break;
                case 20:
                    t = sd.cosh(in);
                    tc.expectedOutput(t.getVarName(), Transforms.cosh(ia, true));
                    break;
                case 21:
                    t = sd.cube(in);
                    tc.expectedOutput(t.getVarName(), Transforms.pow(ia, 3.0, true));
                    break;
                case 22:
                    t = sd.elu(in);
                    tc.expectedOutput(t.getVarName(), Transforms.elu(ia, true));
                    break;
                case 23:
                    //TODO SHOULDN'T THIS HAVE A DIMENSION ARG???
                    t = sd.softmax(in);
                    ia = Nd4j.rand(minibatch, nOut);
                    tc.expectedOutput(t.getVarName(), Nd4j.getExecutioner().execAndReturn(new OldSoftMax(ia.dup())));
                    break;
                case 24:
                    t = sd.sqrt(in);
                    ia = Nd4j.rand(minibatch, nOut);
                    tc.expectedOutput(t.getVarName(), Transforms.sqrt(ia, true));
                    break;
                case 25:
                    t = sd.square(in);
                    tc.expectedOutput(t.getVarName(), Transforms.pow(ia, 2.0, true));
                    break;
                case 26:
                    t = sd.transpose(in);
                    tc.expectedOutput(t.getVarName(), ia.transpose().dup());
                    break;
                case 27:
                    t = sd.abs(in);
                    tc.expectedOutput(t.getVarName(), Transforms.abs(ia, true));
                    break;
                case 28:
                    t = sd.sinh(in);
                    tc.expectedOutput(t.getVarName(), Transforms.sinh(ia, true));
                    break;
                case 29:
                    t = sd.asinh(in);
                    tc.expectedOutput(t.getVarName(), Nd4j.getExecutioner().execAndReturn(new ASinh(ia.dup())));
                    break;
                case 30:
                    t = sd.exp(in);
                    tc.expectedOutput(t.getVarName(), Transforms.exp(ia, true));
                    break;
                case 31:
                    t = sd.floor(in);
                    tc.expectedOutput(t.getVarName(), Transforms.floor(ia, true));
                    break;
                case 32:
                    t = sd.relu(in, 0.0);
                    ia = Nd4j.rand(minibatch, nOut);
                    tc.expectedOutput(t.getVarName(), Transforms.relu(ia, true));
                    break;
                case 33:
                    t = sd.hardTanh(in);
                    ia = Nd4j.rand(minibatch, nOut).muli(2).subi(1.0);
                    tc.expectedOutput(t.getVarName(), Transforms.hardTanh(ia, true));
                    break;
                case 34:
                    t = sd.logSigmoid(in);
                    tc.expectedOutput(t.getVarName(), Nd4j.getExecutioner().execAndReturn(new LogSigmoid(ia.dup())));
                    break;
                case 35:
                    t = sd.swish(in);
                    tc.expectedOutput(t.getVarName(), Nd4j.getExecutioner().execAndReturn(new Swish(ia.dup())));
                    break;
                case 36:
                    t = sd.sign(in);
                    tc.expectedOutput(t.getVarName(), Transforms.sign(ia, true));
                    break;
                case 37:
                    t = sd.softsign(in);
                    tc.expectedOutput(t.getVarName(), Transforms.softsign(ia, true));
                    break;
                case 38:
                    t = sd.leakyRelu(in, 0.0);
                    ia = Nd4j.rand(minibatch, nOut);
                    tc.expectedOutput(t.getVarName(), Transforms.leakyRelu(ia, true));
                    break;
                case 39:
                    t = sd.logSoftmax(in);
                    ia = Nd4j.rand(minibatch, nOut).muli(10).subi(5);
                    tc.expectedOutput(t.getVarName(), Transforms.log(Transforms.softmax(ia, true)));
                    stdevLoss = true;
                    break;
                case 40:
                    t = sd.selu(in);
                    tc.expectedOutput(t.getVarName(), Nd4j.getExecutioner().execAndReturn(new SELU(ia.dup())));
                    break;
                case 41:
                    t = sd.gt(in, 1.0);
                    tc.expectedOutput(t.getVarName(), ia.gt(1.0));
                    break;
                case 42:
                    t = sd.gte(in, 1.0);
                    tc.expectedOutput(t.getVarName(), ia.gte(1.0));
                    break;
                case 43:
                    t = sd.lt(in, 1.0);
                    tc.expectedOutput(t.getVarName(), ia.lt(1.0));
                    break;
                case 44:
                    t = sd.lte(in, 1.0);
                    tc.expectedOutput(t.getVarName(), ia.lte(1.0));
                    break;
                case 45:
                    t = sd.eq(in, 2.0);
                    ia = Nd4j.linspace(1, minibatch * nOut, minibatch * nOut).reshape('c', minibatch, nOut);
                    tc.expectedOutput(t.getVarName(), ia.eq(2.0));
                    break;
                case 46:
                    t = sd.neq(in, 2.0);
                    ia = Nd4j.linspace(1, minibatch * nOut, minibatch * nOut).reshape('c', minibatch, nOut);
                    tc.expectedOutput(t.getVarName(), ia.neq(2.0));
                    break;
                case 47:
                    t = sd.ceil(in);
                    tc.expectedOutput(t.getVarName(), Transforms.ceil(ia, true));
                    break;
                case 48:
                    ia = Nd4j.randn(ia.shape()).muli(2);
                    t = sd.clipByValue(in, -3, 2);
                    INDArray expOut48 = ia.dup();
                    BooleanIndexing.replaceWhere(expOut48, -3, Conditions.lessThan(-3));
                    BooleanIndexing.replaceWhere(expOut48, 2, Conditions.greaterThan(2));
                    tc.expectedOutput(t.getVarName(), expOut48);
                    break;
                case 49:
                    //Clip by norm, dimension 0, some below threshold, some above
                    double clip = 2.0;
                    t = sd.clipByNorm(in, clip, 0);
                    ia = Nd4j.rand(ia.shape());
                    ia.diviRowVector(ia.norm2(0)).muli(clip);  //Norm2 is now 'clip' (i.e., exactly at threshold
                    //System.out.println(ia.norm2(0));
                    ia.muliColumnVector(Nd4j.linspace(0.9, 1.1, ia.size(0)).transpose());
                    //System.out.println(ia.norm2(0));

                    INDArray expOut49 = Nd4j.create(ia.shape());
                    for (int j = 0; j < ia.columns(); j++) {
                        INDArray origCol = ia.getColumn(j);
                        if (origCol.norm2Number().doubleValue() < clip) {
                            expOut49.putColumn(j, origCol);
                        } else {
                            expOut49.putColumn(j, origCol.mul(clip / origCol.norm2Number().doubleValue()));
                        }
                    }
                    tc.expectedOutput(t.getVarName(), expOut49);
                    //System.out.println(expOut.norm2(0));
                    break;
                //TODO clip by norm along other dimensions
                case 50:
                    dim = 1;
                    t = sd.reverse(in, dim);
                    INDArray expOut50 = Nd4j.create(ia.shape());
                    DynamicCustomOp reverse = DynamicCustomOp.builder("reverse")
                            .addIntegerArguments(dim)
                            .addInputs(ia).addOutputs(expOut50).build();
                    Nd4j.getExecutioner().exec(reverse);
                    tc.expectedOutput(t.getVarName(), expOut50);
                    break;
                case 51:
                    dim = 0;
                    boolean exclusive = false;
                    boolean reverseBool = false;

                    t = sd.cumsum(in, exclusive, reverseBool, dim);
                    INDArray expOut51 = Nd4j.create(ia.shape());
                    DynamicCustomOp cumsum = DynamicCustomOp.builder("cumsum")
                            .addIntegerArguments((exclusive) ? 1 : 0, (reverseBool) ? 1 : 0, dim)
                            .addInputs(ia).addOutputs(expOut51).build();
                    Nd4j.getExecutioner().exec(cumsum);
                    tc.expectedOutput(t.getVarName(), expOut51);
                    break;
                case 52:
                    if(OpValidationSuite.IGNORE_FAILING){
                        continue;
                    }
                    boolean ex = false;
                    boolean revBool = false;
                    t = sd.cumprod(in, ex, revBool, 0);
                    INDArray expOut52 = Nd4j.create(ia.shape());
                    for( int s0=0; s0<ia.size(0); s0++){
                        for( int s1=0; s1<ia.size(1); s1++ ){
                            double prod = 1.0;
                            for(int x=0; x<=s0; x++ ){
                                prod *= ia.getDouble(x, s1);
                            }
                            expOut52.putScalar(s0, s1, prod);
                        }
                    }
                    tc.expectedOutput(t.getVarName(), expOut52);
                    break;
                case 53:
                    if(OpValidationSuite.IGNORE_FAILING){
                        continue;
                    }
                    t = sd.diag(in);
                    ia = Nd4j.create(new float[]{4, 2});
                    in = sd.var("in", new int[]{1, 2});
                    INDArray expOut53 = Nd4j.create(new int[]{2, 2});
                    DynamicCustomOp op = DynamicCustomOp.builder("diag").addInputs(ia).addOutputs(expOut53).build();
                    Nd4j.getExecutioner().exec(op);
                    tc.expectedOutput(t.getVarName(), expOut53);
                    break;
                case 54:
                    t = sd.erf(in);
                    INDArray expOut54 = Nd4j.createUninitialized(ia.shape(), ia.ordering());
                    Nd4j.getExecutioner().exec(new Erf(ia, expOut54));
                    tc.expectedOutput(t.getVarName(), expOut54);
                    break;
                case 55:
                    t = sd.erfc(in);
                    tc.expectedOutput(t.getVarName(), Nd4j.getExecutioner().execAndReturn(new Erfc(ia, Nd4j.createUninitialized(ia.shape(), ia.ordering()))));
                    break;
                case 56:
                    t = sd.expm1(in);
                    tc.expectedOutput(t.getVarName(),Transforms.expm1(ia, true));
                    break;
                case 57:
                    t = sd.log1p(in);
                    ia = Nd4j.rand(minibatch, nOut);
                    tc.expectedOutput(t.getVarName(), Transforms.log1p(ia, true));
                    break;
                case 58:
                    t = sd.round(in);
                    tc.expectedOutput(t.getVarName(), Transforms.round(ia, true));
                    break;
                case 59:
                    ia = Nd4j.create(new float[]{4, 2});
                    in = sd.var("in", new int[]{1, 2});
                    t = sd.rsqrt(in);
                    tc.expectedOutput(t.getVarName(),Nd4j.getExecutioner().execAndReturn(new RSqrt(ia, Nd4j.create(ia.shape(), ia.ordering()))));
                    break;
                case 60:
                    t = sd.relu6(in, 0);
                    ia = Nd4j.rand(minibatch, nOut);
                    tc.expectedOutput(t.getVarName(),Transforms.relu6(ia, true));
                    break;
                case 61:
                    ia = Nd4j.create(new float[] {2, 2});
                    in = sd.var("in", new int[]{1, 2});
                    sd.associateArrayWithVariable(ia, in);
                    double value = 42;
                    t = sd.fill(in, value);
                    tc.expectedOutput(t.getVarName(), Nd4j.valueArrayOf(new int[]{2,2}, 42));
                    break;
                case 62:
                    t = sd.hardSigmoid(in);
                    tc.expectedOutput(t.getVarName(), Nd4j.getExecutioner().execAndReturn(new HardSigmoid(ia, ia.dup())));
                    break;
                case 63:
                    t = sd.scalarMax(in, 0.5);
                    tc.expectedOutput(t.getVarName(), Transforms.max(ia, 0.5, true));
                    break;
                case 64:
                    t = sd.scalarMin(in, 0.5);
                    tc.expectedOutput(t.getVarName(), Transforms.min(ia, 0.5, true));
                    break;
                case 65:
                    t = sd.assign(in, 0.5);
                    tc.expectedOutput(t.getVarName(), ia.dup().assign(0.5));
                    break;
                case 66:
                    t = sd.scalarFloorMod(in, 0.5);
                    tc.expectedOutput(t.getVarName(), Nd4j.getExecutioner().execAndReturn(new ScalarFMod(ia.dup(), 0.5)));
                    break;
                case 67:
                    t = sd.reciprocal(in);
                    tc.expectedOutput(t.getVarName(), ia.rdiv(1.0));
                    break;
                case 68:
                    t = sd.shape(in);
                    tc.expectedOutput(t.getVarName(), Nd4j.create(ArrayUtil.toDouble(ia.shape())));
                    break;
                case 69:
                    t = sd.rank(in);
                    tc.expectedOutput(t.getVarName(), Nd4j.create(new double[]{ia.rank()}));
                    break;
                case 70:
                    t = sd.onesLike(in);
                    tc.expectedOutput(t.getVarName(), Nd4j.ones(ia.shape()));
                    break;
                case 71:
                    ia = Nd4j.randn(nOut, nOut);
                    t = sd.diagPart(in);
                    tc.expectedOutput(t.getVarName(), Nd4j.trueVector(new double[]{ia.getDouble(0,0), ia.getDouble(1,1), ia.getDouble(2,2), ia.getDouble(3,3)}));
                    break;
                case 72:
                    t = sd.identity(in);
                    tc.expected(t, ia.dup());
                    break;
                case 73:
                    t = sd.step(in, 1.0);
                    tc.expected(t, ia.gte(1.0));
                    break;
                case 74:
                    if(OpValidationSuite.IGNORE_FAILING){
                        continue;
                    }
                    t = sd.f().noop(in);
                    tc.expected(t, ia.dup());
                    break;
                case 75:
                    ia = Nd4j.rand(ia.shape());
                    t = sd.log(in, 2);
                    tc.expected(t, Transforms.log(ia, 2, true));
                    break;
                case 76:
                    ia = Nd4j.rand(ia.shape());
                    t = sd.log(in, 10);
                    tc.expected(t, Transforms.log(ia, 10, true));
                    break;
                case 77:
                    ia = Nd4j.rand(ia.shape());
                    t = sd.matchCondition(in, Conditions.lessThan(0.5));
                    INDArray exp = ia.dup().lt(0.5);
                    tc.expected(t, exp);
                    break;
                case 78:
                    ia = Nd4j.rand(ia.shape()).muli(2).subi(1);
                    t = sd.f().tanhRational(in);
                    tc.expected(t, Nd4j.getExecutioner().execAndReturn(new RationalTanh(ia.dup())));
                    break;
                case 79:
                    ia = Nd4j.rand(ia.shape()).muli(2).subi(1);
                    t = sd.f().tanhRectified(in);
                    tc.expected(t, Nd4j.getExecutioner().execAndReturn(new RectifiedTanh(ia.dup())));
                    break;
                default:
                    throw new RuntimeException();
            }


            DifferentialFunction[] funcs = sd.functions();
            String name = funcs[0].opName();


            String msg = "test: " + i + " - " + name;
            log.info("*** Starting test: " + msg);

            SDVariable loss;
            if(stdevLoss){
                loss = sd.standardDeviation("loss", t, false, Integer.MAX_VALUE);   //.standardDeviation("loss", t, true, Integer.MAX_VALUE);
            } else {
                loss = sd.mean("loss", t);
            }

            sd.associateArrayWithVariable(ia, in);

            tc.testName(name);
            String error = OpValidation.validate(tc, true);
            if(error != null){
                allFailed.add(name);
            }
        }

        if (allSkipped.size() > 0) {
            log.info("All backward skipped transforms: " + allSkipped);
            log.info(allSkipped.size() + " backward passes were skipped.");
        }

        if (allFailed.size() > 0) {
            log.error("All failed transforms: " + allFailed);
            fail(allFailed.size() + " transforms failed");
        }
    }


    @Test
    public void testPairwiseTransforms() {
        /*
        add, sub, mul, div, rsub, rdiv
        eq, neq, gt, lt, gte, lte, or, and, xor
        min, max
        mmul
        tensormmul
         */
        //Test transforms (pairwise)
        Nd4j.getRandom().setSeed(12345);

        List<String> allFailed = new ArrayList<>();
        for (int i = 0; i < 23; i++) {

            SameDiff sd = SameDiff.create();

            int nOut = 4;
            int minibatch = 5;
            SDVariable in1 = sd.var("in1", new int[]{-1, nOut});
            SDVariable in2 = sd.var("in2", new int[]{-1, nOut});

            INDArray ia = Nd4j.randn(minibatch, nOut);
            INDArray ib = Nd4j.randn(minibatch, nOut);

            SDVariable t;
            TestCase tc = new TestCase(sd);
            switch (i) {
                case 0:
                    t = in1.add(in2);
                    tc.expectedOutput(t.getVarName(), ia.add(ib));
                    break;
                case 1:
                    t = in1.sub(in2);
                    tc.expectedOutput(t.getVarName(),ia.sub(ib));
                    break;
                case 2:
                    t = in1.mul(in2);
                    tc.expectedOutput(t.getVarName(), ia.mul(ib));
                    break;
                case 3:
                    t = in1.div(in2);
                    tc.expectedOutput(t.getVarName(), ia.div(ib));
                    break;
                case 4:
                    t = in1.rsub(in2);
                    tc.expectedOutput(t.getVarName(), ia.rsub(ib));
                    break;
                case 5:
                    t = in1.rdiv(in2);
                    tc.expectedOutput(t.getVarName(), ia.rdiv(ib));
                    break;
                case 6:
                    t = sd.eq(in1, in2);
                    tc.expectedOutput(t.getVarName(), ia.eq(ib));
                    break;
                case 7:
                    t = sd.neq(in1, in2);
                    tc.expectedOutput(t.getVarName(), ia.neq(ib));
                    break;
                case 8:
                    t = sd.gt(in1, in2);
                    tc.expectedOutput(t.getVarName(), ia.gt(ib));
                    break;
                case 9:
                    t = sd.lt(in1, in2);
                    tc.expectedOutput(t.getVarName(), ia.lt(ib));
                    break;
                case 10:
                    t = sd.gte(in1, in2);
                    INDArray expOut10 = ia.dup();
                    Nd4j.getExecutioner().exec(new GreaterThanOrEqual(new INDArray[]{ia, ib}, new INDArray[]{expOut10}));
                    tc.expectedOutput(t.getVarName(), expOut10);
                    break;
                case 11:
                    t = sd.lte(in1, in2);
                    INDArray expOut11 = ia.dup();
                    Nd4j.getExecutioner().exec(new LessThanOrEqual(new INDArray[]{ia, ib}, new INDArray[]{expOut11}));
                    tc.expectedOutput(t.getVarName(), expOut11);
                    break;
                case 12:
                    ia = Nd4j.getExecutioner().exec(new BernoulliDistribution(ia, 0.5));
                    ib = Nd4j.getExecutioner().exec(new BernoulliDistribution(ib, 0.5));
                    t = sd.or(in1, in2);
                    tc.expectedOutput(t.getVarName(), Transforms.or(ia, ib));
                    break;
                case 13:
                    ib = Nd4j.randn(nOut, nOut);
                    t = sd.mmul(in1, in2);
                    tc.expectedOutput(t.getVarName(), ia.mmul(ib));
                    break;
                case 14:
                    t = sd.max(in1, in2);
                    tc.expectedOutput(t.getVarName(), Nd4j.getExecutioner().execAndReturn(new OldMax(ia, ib, ia.dup(), ia.length())));
                    break;
                case 15:
                    t = sd.min(in1, in2);
                    tc.expectedOutput(t.getVarName(), Nd4j.getExecutioner().execAndReturn(new OldMin(ia, ib, ia.dup(), ia.length())));
                    break;
                case 16:
                    ia = Nd4j.getExecutioner().exec(new BernoulliDistribution(ia, 0.5));
                    ib = Nd4j.getExecutioner().exec(new BernoulliDistribution(ib, 0.5));
                    t = sd.and(in1, in2);
                    tc.expectedOutput(t.getVarName(), Transforms.and(ia, ib));
                    break;
                case 17:
                    ia = Nd4j.getExecutioner().exec(new BernoulliDistribution(ia, 0.5));
                    ib = Nd4j.getExecutioner().exec(new BernoulliDistribution(ib, 0.5));
                    t = sd.xor(in1, in2);
                    tc.expectedOutput(t.getVarName(), Transforms.xor(ia, ib));
                    break;
                case 18:
                    t = sd.assign(in1, in2);
                    tc.expectedOutput(t.getVarName(), ib);
                    break;
                case 19:
                    t = sd.atan2(in1, in2);
                    tc.expectedOutput(t.getVarName(), Transforms.atan2(ib, ia));    //Note: y,x order for samediff; x,y order for transforms
                    break;
                case 20:
                    t = sd.mergeAdd(in1, in2, in2);
                    tc.expectedOutput(t.getVarName(), ia.add(ib).add(ib));
                    break;
                case 21:
                    t = in1.squaredDifference(in2);
                    INDArray expOut21 = Nd4j.create(ia.shape(), ia.ordering());
                    DynamicCustomOp squareDiff = DynamicCustomOp.builder("squaredsubtract")
                            .addInputs(ia, ib)
                            .addOutputs(expOut21)
                            .build();
                    Nd4j.getExecutioner().exec(squareDiff);
                    tc.expectedOutput(t.getVarName(), expOut21);
                    break;
                case 22:
                    //set diag
                    ia = Nd4j.randn(nOut, nOut);
                    ib = Nd4j.randn(1, nOut).reshape(nOut);
                    INDArray expOut22 = ia.dup();
                    for( int j=0; j<nOut; j++ ){
                        expOut22.putScalar(j,j, ib.getDouble(j));
                    }
                    t = sd.setDiag(in1, in2);
                    tc.expectedOutput(t.getVarName(), expOut22);
                    break;
                default:
                    throw new RuntimeException();
            }


            DifferentialFunction[] funcs = sd.functions();
            String name = funcs[0].opName();

            String msg = "test: " + i + " - " + name;
            log.info("*** Starting test: " + msg);

            SDVariable loss = sd.mean("loss", t);

            sd.associateArrayWithVariable(ia, in1);
            sd.associateArrayWithVariable(ib, in2);

            tc.testName(name);
            String error = OpValidation.validate(tc, true);
            if(error != null){
                allFailed.add(name);
            }
        }

        if (allFailed.size() > 0) {
            log.error("All failed transforms: " + allFailed);
            fail(allFailed.size() + " transforms failed");
        }
    }

    @Test
    public void testIsX(){
        List<String> failed = new ArrayList<>();

        for( int i=0; i<4; i++ ){

            SameDiff sd = SameDiff.create();
            SDVariable in = sd.var("in", 4);

            boolean doGrad = true;
            SDVariable out;
            INDArray exp;
            INDArray inArr;
            switch (i){
                case 0:
                    inArr = Nd4j.trueVector(new double[]{10,Double.POSITIVE_INFINITY, 0, Double.NEGATIVE_INFINITY});
                    exp = Nd4j.trueVector(new double[]{1,0,1,0});
                    out = sd.isFinite(in);
                    break;
                case 1:
                    inArr = Nd4j.trueVector(new double[]{10,Double.POSITIVE_INFINITY, 0, Double.NEGATIVE_INFINITY});
                    exp = Nd4j.trueVector(new double[]{0,1,0,1});
                    out = sd.isInfinite(in);
                    break;
                case 2:
                    inArr = Nd4j.trueVector(new double[]{-3,5,0,2});
                    exp = Nd4j.trueVector(new double[]{0,1,0,0});
                    out = sd.isMax(in);
                    break;
                case 3:
                    inArr = Nd4j.trueVector(new double[]{0,Double.NaN,10,Double.NaN});
                    exp = Nd4j.trueVector(new double[]{0,1,0,1});
                    out = sd.isNaN(in);
                    doGrad = false; //Can't grad check due to NaNs
                    break;
                default:
                    throw new RuntimeException();
            }

            SDVariable loss = out.mean();
            TestCase tc = new TestCase(sd)
                    .gradientCheck(doGrad)
                    .expected(out, exp);

            in.setArray(inArr);

            String err = OpValidation.validate(tc, true);
            if(err != null){
                failed.add(err);
            }
        }
        assertEquals(failed.toString(), 0, failed.size());
    }

    @Test
    public void testReplaceWhereScalar(){
        for(Condition c : new Condition[]{Conditions.lessThan(0.5), Conditions.greaterThan(0.5), Conditions.equals(0.5)}){

            log.info("Testing condition: " + c.getClass().getName());
            INDArray inArr = Nd4j.rand(3,4);
            SameDiff sd = SameDiff.create();
            SDVariable in = sd.var("in", inArr);
            SDVariable where = sd.replaceWhere(in, 10, c);

            INDArray exp = inArr.dup();
            BooleanIndexing.replaceWhere(exp, 10, c);

            SDVariable loss = where.std(true);

            TestCase tc = new TestCase(sd);

            String err = OpValidation.validate(tc);
            assertNull(err);
        }
    }

    @Test
    public void testReplaceWhereArray(){
        for(Condition c : new Condition[]{Conditions.lessThan(0.5), Conditions.greaterThan(0.5), Conditions.equals(0.5)}){

            INDArray inArr = Nd4j.rand(3,4);
            INDArray inArr2 = Nd4j.valueArrayOf(3, 4, 10);
            SameDiff sd = SameDiff.create();
            SDVariable in = sd.var("in", inArr);
            SDVariable in2 = sd.var("in2", inArr2);
            SDVariable where = sd.replaceWhere(in, in2, c);

            INDArray exp = inArr.dup();
            BooleanIndexing.replaceWhere(exp, inArr2, c);

            SDVariable loss = where.std(true);

            TestCase tc = new TestCase(sd);

            String err = OpValidation.validate(tc);
            assertNull(err);
        }
    }

    //TODO UPDATE TO OP VALIDATION OR DELETE
    @Test
    public void testLogGrad() {
        SameDiff sameDiff = SameDiff.create();
        SDVariable input = sameDiff.var("x", Nd4j.linspace(1, 4, 4));
        SDVariable log = sameDiff.log(input);
        SDVariable sum = sameDiff.sum(log, Integer.MAX_VALUE);
        INDArray result = null;
        Pair<Map<SDVariable, DifferentialFunction>, List<DifferentialFunction>> execBackwards = sameDiff.execBackwards();
        System.out.println(execBackwards);
        //INDArray assertion = Nd4j.create(new double[]{1, 0.5, 0.33, 0.25});
        // assertTrue(assertion.equalsWithEps(result, 1e-2));
    }


    @Test
    public void testSigmoidBackwards() {
        SameDiff sameDiff = SameDiff.create();
        INDArray sumInput = Nd4j.linspace(1, 4, 4).reshape(2, 2);
        Map<String, INDArray> inputs = new HashMap<>();
        inputs.put("x", sumInput);
        SDVariable input = sameDiff.var("x", inputs.get("x"));
        SDVariable sigmoid = sameDiff.sigmoid(input);
        SDVariable sum = sameDiff.sum(sigmoid, Integer.MAX_VALUE);
        List<DifferentialFunction> backwardsOps = sameDiff.execBackwards().getRight();
        Op finalOp = (Op) backwardsOps.get(backwardsOps.size() - 1);
        assertTrue(Nd4j.create(new double[][]{
                {0.1966, 0.1050},
                {0.0452, 0.0177}
        }).equalsWithEps(
                finalOp.z(), 1e-2));
        System.out.println(backwardsOps);
    }

    @Test
    public void testExpGradient() {
        SameDiff sameDiff = SameDiff.create();
        INDArray sumInput = Nd4j.linspace(1, 4, 4).reshape(2, 2);
        Map<String, INDArray> inputs = new HashMap<>();
        inputs.put("x", sumInput);
        sameDiff.defineFunction("expGradient", new SameDiffFunctionDefinition() {
            @Override
            public SDVariable[] define(SameDiff sameDiff, Map<String, INDArray> inputs, SDVariable[] variableInputs) {
                SDVariable input = sameDiff.var("x", inputs.get("x"));
                SDVariable exp = sameDiff.exp(input);
                SDVariable sum = sameDiff.sum(exp, Integer.MAX_VALUE);
                return new SDVariable[]{sum};
            }
        }, inputs);


        List<DifferentialFunction> ops = sameDiff.getFunction("expGradient").execBackwards().getRight();

        INDArray executions = ops.get(ops.size() - 1).outputVariables()[0].getArr();
        INDArray assertion = Nd4j.create(new double[][]{
                {2.7183, 7.3891},
                {20.0855, 54.5981}
        });
        assertArrayEquals(sumInput.shape(), executions.shape());
        assertEquals(assertion, executions);
        System.out.println(executions);
        //assertEquals(Nd4j.ones(2,2),executions);
    }


/*    @Test
    public void testDepth() {
        SameDiff sameDiff = SameDiff.create();
        SDVariable x = sameDiff.one("one",new long[]{2,2});
        assertEquals(0,x.depth());
        SDVariable sigmoid = sameDiff.sigmoid("sigmoid",x);
        assertEquals(1,sigmoid.depth());
    }*/


    @Test
    public void testTanhGradient() {
        SameDiff sameDiff = SameDiff.create();
        INDArray sumInput = Nd4j.linspace(1, 4, 4).reshape(2, 2);
        Map<String, INDArray> inputs = new HashMap<>();
        inputs.put("x", sumInput);
        sameDiff.defineFunction("tanhGradient", new SameDiffFunctionDefinition() {
            @Override
            public SDVariable[] define(SameDiff sameDiff, Map<String, INDArray> inputs, SDVariable[] variableInputs) {
                SDVariable input = sameDiff.var("x", inputs.get("x"));
                SDVariable tanh = sameDiff.tanh(input);
                SDVariable sum = sameDiff.sum(tanh, Integer.MAX_VALUE);
                return new SDVariable[]{tanh};
            }
        }, inputs);

        INDArray executions = sameDiff.getFunction("tanhGradient").execBackwardAndEndResult();
        //[0.41997434161402614,0.07065082485316443,0.009866037165440211,0.0013409506830258655]
        INDArray assertion = Nd4j.create(new double[][]{
                {0.41997434161402614, 0.07065082485316443},
                {0.009866037165440211, 0.0013409506830258655}
        });

        assertTrue(assertion.equalsWithEps(
                executions, 1e-3));

        assertArrayEquals(sumInput.shape(), executions.shape());
        assertEquals(assertion, executions);
        System.out.println(executions);
        //assertEquals(Nd4j.ones(2,2),executions);
    }

    @Test
    public void testRank0EdgeCase(){
        SameDiff sd = SameDiff.create();
        SDVariable v1 = sd.sum(sd.var(Nd4j.create(new double[]{4, 4})));
        double d0 = sd.execAndEndResult().getDouble(0);
        assertEquals(8, d0, 0);

        SDVariable v2 = sd.sum(sd.var(Nd4j.create(new double[]{4, 4}))).div(2.0);
        double d1 = sd.execAndEndResult().getDouble(0);
        assertEquals(4, d1, 0);
    }

    @Test
    public void testAtan2BroadcastShape(){
        INDArray arr1 = Nd4j.create(new long[]{3,1,4});
        INDArray arr2 = Nd4j.create(new long[]{1,2,4});

        DynamicCustomOp op = DynamicCustomOp.builder("tf_atan2")
                .addInputs(arr1, arr2)
                .build();

        List<long[]> outShapes = Nd4j.getExecutioner().calculateOutputShape(op);
        assertEquals(1, outShapes.size());

        assertArrayEquals(Arrays.toString(outShapes.get(0)), new long[]{3,2,4}, outShapes.get(0));
    }

    @Test
    public void testBooleanAnd(){
        Nd4j.setDataType(DataBuffer.Type.FLOAT);
        INDArray arr1 = Nd4j.create(new long[]{3,4});
        INDArray arr2 = Nd4j.create(new long[]{3,4});
        INDArray out = Nd4j.create(new long[]{3,4});

        DynamicCustomOp op = DynamicCustomOp.builder("boolean_and")
                .addInputs(arr1, arr2)
                .addOutputs(out)
                .build();
        Nd4j.getExecutioner().exec(op);
    }

    @Test
    public void testLogicalNot(){
        Nd4j.setDataType(DataBuffer.Type.FLOAT);
        INDArray x = Nd4j.create(new long[]{3,4});
        INDArray z = Nd4j.create(new long[]{3,4});

        Op op = new Not(x, z);
        Nd4j.getExecutioner().exec(op);
    }


    @Test
    public void testScatterOpsScalar(){
        for(String s : new String[]{"add", "sub", "mul", "div"}) {
            INDArray ref = Nd4j.linspace(1, 30, 30).reshape(10, 3);
            INDArray indices = Nd4j.trueScalar(5);
            INDArray upd = Nd4j.trueVector(new double[]{10, 20, 30});

            //The non-scalar case works:
//            INDArray indices = Nd4j.trueVector(new float[]{5});
//            INDArray upd = Nd4j.create(new double[]{10, 20, 30}, new int[]{1, 3});

            INDArray exp = ref.dup();
            switch (s){
                case "add":
                    exp.getRow(5).addi(upd);
                    break;
                case "sub":
                    exp.getRow(5).subi(upd);
                    break;
                case "mul":
                    exp.getRow(5).muli(upd);
                    break;
                case "div":
                    exp.getRow(5).divi(upd);
                    break;
                default:
                    throw new RuntimeException();
            }


            INDArray out = Nd4j.create(10, 3);

            DynamicCustomOp op = DynamicCustomOp.builder("scatter_" + s)
                    .addInputs(ref, indices, upd)
                    .addOutputs(out)
                    .build();

            Nd4j.getExecutioner().exec(op);

            assertEquals(s, exp, out);
        }
    }


    @Test
    public void testPad(){
        INDArray in = Nd4j.valueArrayOf(new long[]{5}, 1.0);
        INDArray pad = Nd4j.create(new double[]{1,1}, new long[]{1,2});
        INDArray value = Nd4j.trueScalar(10.0);

        INDArray out = Nd4j.create(new long[]{7});

        DynamicCustomOp op = DynamicCustomOp.builder("pad")
                .addInputs(in, pad, value)
                //.addInputs(in, pad) //Also doesn't work
                .addOutputs(out)
                .addIntegerArguments(0) //0 = CONSTANT
                .build();

        OpValidation.validate(new OpTestCase(op)
                .expectedOutput(0, Nd4j.trueVector(new double[]{10, 1, 1, 1, 1, 1, 10})));
    }


    @Test
    public void testMirrorPad(){
//        OpValidationSuite.ignoreFailing();

        INDArray in = Nd4j.linspace(1, 6, 6).reshape(2,3);
        INDArray pad = Nd4j.create(new double[][]{{1,1},{2,2}});

        INDArray out = Nd4j.create(new long[]{4,7});

        DynamicCustomOp op = DynamicCustomOp.builder("mirror_pad")
                .addInputs(in, pad)
                .addOutputs(out)
                .addIntegerArguments(0) //0=reflect, 1=symmetric
                .build();

        Nd4j.getExecutioner().exec(op);

        INDArray exp = Nd4j.create(new double[][]{
                {6, 5, 4, 5, 6, 5, 4},
                {3, 2, 1, 2, 3, 2, 1},
                {6, 5, 4, 5, 6, 5, 4},
                {3, 2, 1, 2, 3, 2, 1}});
        String err = OpValidation.validate(new OpTestCase(op)
            .expectedOutput(0, exp));

        assertNull(err);
    }

    @Test
    public void testMirrorPad2(){
//        OpValidationSuite.ignoreFailing();

        INDArray in = Nd4j.linspace(1, 6, 6).reshape(2,3);
        INDArray pad = Nd4j.create(new double[][]{{1,1},{2,2}});

        INDArray out = Nd4j.create(new long[]{4,7});

        DynamicCustomOp op = DynamicCustomOp.builder("mirror_pad")
                .addInputs(in, pad)
                .addOutputs(out)
                .addIntegerArguments(1) //0=reflect, 1=symmetric
                .build();

        Nd4j.getExecutioner().exec(op);

        INDArray exp = Nd4j.create(new double[][]{
                {2, 1, 1, 2, 3, 3, 2},
                {2, 1, 1, 2, 3, 3, 2},
                {5, 4, 4, 5, 6, 6, 5},
                {5, 4, 4, 5, 6, 6, 5}});
        String err = OpValidation.validate(new OpTestCase(op)
                .expectedOutput(0, exp));

        assertNull(err);
    }

    @Test
    public void testMirrorPadSymmetric(){
        INDArray in = Nd4j.linspace(1, 12, 12).reshape(3,4);
        INDArray pad = Nd4j.create(new double[][]{{1,1},{1,1}});

        INDArray out = Nd4j.create(new long[]{5,6});

        DynamicCustomOp op = DynamicCustomOp.builder("mirror_pad")
                .addInputs(in, pad)
                .addOutputs(out)
                .addIntegerArguments(1) //0=reflect, 1=symmetric
                .build();

        Nd4j.getExecutioner().exec(op);

        INDArray exp = Nd4j.create(new double[][]{
                { 1,  1,  2,  3,  4,  4},
                { 1,  1,  2,  3,  4,  4},
                { 5,  5,  6,  7,  8,  8},
                { 9,  9, 10, 11, 12, 12},
                { 9,  9, 10, 11, 12, 12}});
        String err = OpValidation.validate(new OpTestCase(op)
                .expectedOutput(0, exp));

        assertNull(err);
    }

    @Test
    public void testUnique(){
        INDArray in = Nd4j.trueVector(new double[]{3, 4, 3, 1, 3, 0, 2, 4, 2, 4});

        INDArray expUnique = Nd4j.trueVector(new double[]{3, 4, 1, 0, 2});
        INDArray expUniqueIdxs = Nd4j.trueVector(new double[]{0, 1, 0, 2, 0, 3, 4, 1, 4, 1});

        INDArray outUnique = Nd4j.create(expUnique.shape());
        INDArray outUniqueIdxs = Nd4j.create(expUniqueIdxs.shape());

        DynamicCustomOp op = DynamicCustomOp.builder("unique")
                .addInputs(in)
                .addOutputs(outUnique, outUniqueIdxs)
                .build();

        String err = OpValidation.validate(new OpTestCase(op)
                .expectedOutput(0, expUnique)
                .expectedOutput(1, expUniqueIdxs));

        assertNull(err);
    }

    @Test
    public void testTopK(){
        OpValidationSuite.ignoreFailing();  //Can't assume sorted here
        INDArray in = Nd4j.trueVector(new double[]{7, 3, 1, 2, 5, 0, 4, 6, 9, 8});

        INDArray expTopK = Nd4j.trueVector(new double[]{7, 5, 6, 9, 8});
        INDArray expIndices = Nd4j.trueVector(new double[]{0, 4, 7, 8, 9});

        INDArray expTopK_sorted = Nd4j.trueVector(new double[]{9, 8, 7, 6, 5});
        INDArray expIndices_sorted = Nd4j.trueVector(new double[]{8, 9, 0, 7, 4});

        for(boolean sort : new boolean[]{false, true}) {
            INDArray outUnique = Nd4j.create(expTopK.shape());
            INDArray outUniqueIdxs = Nd4j.create(expIndices.shape());

            DynamicCustomOp op = DynamicCustomOp.builder("top_k")
                    .addInputs(in)
                    .addOutputs(outUnique, outUniqueIdxs)
                    .addIntegerArguments(5, sort ? 1 : 0)  //k=5, sort
                    .build();

            String err = OpValidation.validate(new OpTestCase(op)
                    .expectedOutput(0, sort ? expTopK_sorted : expTopK)
                    .expectedOutput(1, sort ? expIndices_sorted : expIndices));

            assertNull(err);
        }
    }

    @Test
    public void testInTopK() {
        for( int k=4; k>= 1; k--){
            log.info("Testing: k=" + k);
            INDArray in = Nd4j.linspace(1, 20, 20).reshape(4, 5);
            INDArray idxs = Nd4j.trueVector(new double[]{1, 2, 3, 4});

            INDArray expOut;
            switch (k){
                case 4:
                    expOut = Nd4j.trueVector(new double[]{1, 1, 1, 1});
                    break;
                case 3:
                    expOut = Nd4j.trueVector(new double[]{0, 1, 1, 1});
                    break;
                case 2:
                    expOut = Nd4j.trueVector(new double[]{0, 0, 1, 1});
                    break;
                case 1:
                    expOut = Nd4j.trueVector(new double[]{0, 0, 0, 1});
                    break;
                default:
                    throw new RuntimeException();
            }



            INDArray out = Nd4j.create(expOut.shape());

            DynamicCustomOp op = DynamicCustomOp.builder("in_top_k")
                    .addInputs(in, idxs)
                    .addOutputs(out)
                    .addIntegerArguments(k)  //k=1
                    .build();

            String err = OpValidation.validate(new OpTestCase(op)
                    .expectedOutput(0, expOut));

            assertNull(err);
        }
    }

    @Test
    public void testZeta(){
        OpValidationSuite.ignoreFailing();  //https://github.com/deeplearning4j/deeplearning4j/issues/6182
        INDArray x = Nd4j.rand(3,4).addi(1.0);
        INDArray q = Nd4j.rand(3,4);

        INDArray out = Nd4j.create(3,4);
        DynamicCustomOp op = DynamicCustomOp.builder("zeta")
                .addInputs(x,q)
                .addOutputs(out)
                .build();

        Nd4j.getExecutioner().exec(op);

        assertNotEquals(Nd4j.create(out.shape()), out);
    }
}
