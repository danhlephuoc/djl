/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package ai.djl.integration.tests.ndarray;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.testing.Assertions;
import org.testng.Assert;
import org.testng.annotations.Test;

public class NDArrayOtherOpTest {

    @Test
    public void testGet() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray original = manager.create(new float[] {1f, 2f, 3f, 4f}, new Shape(2, 2));
            Assert.assertEquals(original.get(new NDIndex()), original);

            NDArray getAt = original.get(0);
            NDArray expected = manager.create(new float[] {1f, 2f});
            Assert.assertEquals(getAt, expected);

            Assert.assertEquals(original.get("0,:"), expected);
            Assert.assertEquals(original.get("0,*"), expected);

            NDArray getSlice = original.get("1:");
            expected = manager.create(new float[] {3f, 4f}, new Shape(1, 2));
            Assert.assertEquals(getSlice, expected);

            NDArray getStepSlice = original.get("1::2");
            Assert.assertEquals(getStepSlice, expected);

            // get from boolean array
            original = manager.arange(10).reshape(2, 5);
            NDArray bool = manager.create(new boolean[] {true, false});
            expected = manager.arange(5).reshape(1, 5);
            Assert.assertEquals(original.get(bool), expected);
        }
    }

    @Test
    public void testSetArray() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray original = manager.create(new float[] {1, 2, 3, 4}, new Shape(2, 2));
            NDArray expected = manager.create(new float[] {9, 10, 3, 4}, new Shape(2, 2));
            NDArray value = manager.create(new float[] {9, 10});
            original.set(new NDIndex(0), value);
            Assert.assertEquals(original, expected);
        }
    }

    @Test
    public void testSetArrayBroadcast() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray original = manager.create(new float[] {1, 2, 3, 4}, new Shape(2, 2, 1));
            NDArray expected = manager.create(new float[] {9, 9, 3, 4}, new Shape(2, 2, 1));
            NDArray value = manager.create(new float[] {9});
            original.set(new NDIndex(0), value);
            Assert.assertEquals(original, expected);
        }
    }

    @Test
    public void testSetNumber() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray original = manager.create(new float[] {1, 2, 3, 4}, new Shape(2, 2));
            NDArray expected = manager.create(new float[] {9, 9, 3, 4}, new Shape(2, 2));
            original.set(new NDIndex(0), 9);
            Assert.assertEquals(original, expected);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSetScalar() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray original = manager.create(new float[] {1, 2, 3, 4}, new Shape(2, 2));
            original.setScalar(new NDIndex(0, 0), 0);
            original.setScalar(new NDIndex(0), 1);
        }
    }

    @Test
    public void testSetByFunction() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray original = manager.arange(1, 10).reshape(3, 3);
            NDArray expected = manager.create(new int[] {4, 10, 16});
            NDIndex index = new NDIndex(":, 1");
            original.set(index, nd -> nd.mul(2));
            Assert.assertEquals(original.get(index), expected);
        }
    }

    @Test
    public void testCopyTo() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array1 = manager.create(new float[] {1f, 2f, 3f, 4f});
            NDArray array2 = manager.create(new Shape(4));
            array1.copyTo(array2);
            Assert.assertEquals(array2, array1, "CopyTo NDArray failed");
            // test multi-dim
            array1 = manager.arange(100.0f).reshape(2, 5, 5, 2);
            array2 = manager.create(new Shape(2, 5, 5, 2));
            array1.copyTo(array2);
            Assert.assertEquals(array2, array1, "CopyTo NDArray failed");
            // test scalar
            array1 = manager.create(5f);
            array2 = manager.create(new Shape());
            array1.copyTo(array2);
            Assert.assertEquals(array2, array1, "CopyTo NDArray failed");
            // test zero-dim
            array1 = manager.create(new Shape(4, 2, 1, 0));
            array2 = manager.create(new Shape(4, 2, 1, 0));
            array1.copyTo(array2);
            Assert.assertEquals(array2, array1, "CopyTo NDArray failed");
        }
    }

    @Test
    public void testNonZero() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.ones(new Shape(3, 3));
            NDArray expected =
                    manager.create(
                            new long[] {0, 0, 0, 1, 0, 2, 1, 0, 1, 1, 1, 2, 2, 0, 2, 1, 2, 2},
                            new Shape(9, 2));
            Assert.assertEquals(array.nonzero(), expected);
            // test multi-dim
            array = manager.create(new float[] {0f, 1f, 2f, 0f, 0f, -4f}, new Shape(2, 1, 3));
            expected = manager.create(new long[] {0, 0, 1, 0, 0, 2, 1, 0, 2}, new Shape(3, 3));
            Assert.assertEquals(array.nonzero(), expected);
            // test scalar
            array = manager.create(1f);
            expected = manager.create(new long[] {0}, new Shape(1, 1));
            Assert.assertEquals(array.nonzero(), expected);
            // test zero-dim
            // TODO confirm this behavior is right
            array = manager.create(new Shape(4, 0));
            expected = manager.create(new Shape(0, 2), DataType.INT64);
            Assert.assertEquals(array.nonzero(), expected);
        }
    }

    @Test
    public void testAll() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.arange(10);
            Assert.assertFalse(array.all().getBoolean());
            Assert.assertTrue(array.add(1f).all().getBoolean());
            array = manager.create(new boolean[] {true, false});
            Assert.assertFalse(array.all().getBoolean());

            // test multi-dim
            array = manager.arange(20).reshape(2, 5, 2);
            Assert.assertFalse(array.all().getBoolean());
            Assert.assertTrue(array.add(1f).all().getBoolean());
            array =
                    manager.create(
                            new boolean[] {true, false, true, false, true, false}, new Shape(2, 3));
            Assert.assertFalse(array.all().getBoolean());

            // test scalar
            array = manager.create(1f);
            Assert.assertTrue(array.all().getBoolean());
            array = manager.create(false);
            Assert.assertFalse(array.all().getBoolean());

            // test zero-dim
            array = manager.create(new Shape(0));
            Assert.assertTrue(array.all().getBoolean());
        }
    }

    @Test
    public void testAny() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.arange(10);
            Assert.assertTrue(array.any().getBoolean());
            array = manager.zeros(new Shape(10));
            Assert.assertFalse(array.all().getBoolean());
            array = manager.create(new boolean[] {true, false});
            Assert.assertTrue(array.any().getBoolean());

            // test multi-dim
            array = manager.eye(2);
            Assert.assertTrue(array.any().getBoolean());
            array =
                    manager.create(
                            new boolean[] {true, false, true, false, true, false}, new Shape(2, 3));
            Assert.assertTrue(array.any().getBoolean());

            // test scalar
            array = manager.create(1f);
            Assert.assertTrue(array.any().getBoolean());
            array = manager.create(false);
            Assert.assertFalse(array.any().getBoolean());

            // test zero-dim
            array = manager.create(new Shape(0));
            Assert.assertFalse(array.any().getBoolean());
        }
    }

    @Test
    public void testNone() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.arange(10);
            Assert.assertFalse(array.none().getBoolean());
            array = manager.zeros(new Shape(10));
            Assert.assertTrue(array.none().getBoolean());
            array = manager.create(new boolean[] {false, false});
            Assert.assertTrue(array.none().getBoolean());

            // test multi-dim
            array = manager.eye(2);
            Assert.assertFalse(array.none().getBoolean());
            array =
                    manager.create(
                            new boolean[] {false, false, false, false, false, false},
                            new Shape(2, 3));
            Assert.assertTrue(array.none().getBoolean());

            // test scalar
            array = manager.create(1f);
            Assert.assertFalse(array.none().getBoolean());
            array = manager.create(false);
            Assert.assertTrue(array.none().getBoolean());

            // test zero-dim
            array = manager.create(new Shape(0));
            Assert.assertTrue(array.none().getBoolean());
        }
    }

    @Test
    public void testCountNonzero() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.arange(4);
            Assert.assertEquals(array.countNonzero().getLong(), 3);

            // multi-dim
            array = manager.create(new float[] {-1f, 0f, 2f, 100f, 2340f, -200f}, new Shape(2, 3));
            Assert.assertEquals(array.countNonzero().getLong(), 5);

            // scalar
            array = manager.create(5f);
            Assert.assertEquals(array.countNonzero().getLong(), 1);
            // zero-dim
            array = manager.create(new Shape(2, 0));
            Assert.assertEquals(array.countNonzero().getLong(), 0);
        }
    }

    @Test
    public void testIsNaN() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new float[] {Float.NaN, 0f});
            NDArray expected = manager.create(new boolean[] {true, false});
            Assert.assertEquals(array.isNaN(), expected);
            array = manager.create(new float[] {1f, 2f});
            Assert.assertFalse(array.isNaN().all().getBoolean());

            // test multi-dim
            array =
                    manager.create(
                            new float[] {Float.NaN, Float.NaN, Float.NaN, 0f}, new Shape(2, 2));
            expected = manager.create(new boolean[] {true, true, true, false}, new Shape(2, 2));
            Assert.assertEquals(array.isNaN(), expected);

            // test scalar
            array = manager.create(Float.NaN);
            expected = manager.create(true);
            Assert.assertEquals(array.isNaN(), expected);

            // test zero-dim
            array = manager.create(new Shape(0));
            expected = manager.create(new Shape(0), DataType.BOOLEAN);
            Assert.assertEquals(array.isNaN(), expected);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBooleanMask() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.arange(4.0f);
            NDArray index = manager.create(new boolean[] {true, false, true, false});
            NDArray expected = manager.create(new float[] {0f, 2f});
            Assert.assertEquals(array.booleanMask(index), expected);
            Assert.assertEquals(NDArrays.booleanMask(array, index), expected);

            // test multi-dim
            array = manager.arange(10.0f).reshape(2, 1, 5);
            index = manager.create(new boolean[] {true, false});
            expected = manager.arange(5.0f).reshape(1, 1, 5);
            Assert.assertEquals(array.booleanMask(index), expected);
            Assert.assertEquals(NDArrays.booleanMask(array, index), expected);

            // test scalar
            array = manager.create(5f);
            index = manager.create(true);
            array.booleanMask(index);

            // test zero-dim
            array = manager.create(new Shape(1, 0));
            index = manager.create(new boolean[] {false});
            expected = manager.create(new Shape(0, 0));
            Assert.assertEquals(array.booleanMask(index), expected);
            Assert.assertEquals(NDArrays.booleanMask(array, index), expected);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSequenceMask() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new float[][] {{1, 2, 3}, {4, 5, 6}});
            NDArray sequenceLength = manager.create(new float[] {1, 2});
            NDArray expected = manager.create(new float[][] {{1, 0, 0}, {4, 5, 0}});
            Assert.assertEquals(array.sequenceMask(sequenceLength), expected);
            Assert.assertEquals(NDArrays.sequenceMask(array, sequenceLength), expected);

            // test zero dimension
            array = manager.create(new Shape(1, 0, 0));
            sequenceLength = manager.create(new float[] {1});
            array.sequenceMask(sequenceLength);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSequenceMaskWithScalarInput() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new Shape());
            NDArray sequenceLength = manager.create(new float[] {1});
            array.sequenceMask(sequenceLength);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSequenceMaskWithScalarSequenceLength() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new Shape(1, 1, 1));
            NDArray sequenceLength = manager.create(new float[] {});
            array.sequenceMask(sequenceLength);
        }
    }

    @Test
    public void testArgSort() {
        // TODO switch to numpy argsort
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new float[] {-1f, 2f, 0f, 999f, -998f});
            NDArray expected = manager.create(new long[] {4, 0, 2, 1, 3});
            Assert.assertEquals(array.argSort(), expected);
            // multi-dim
            array =
                    manager.create(
                            new float[] {-1.000f, -0.009f, -0.0001f, 0.0001f, 0.12f, 0.1201f},
                            new Shape(2, 1, 1, 3, 1));
            expected = manager.zeros(new Shape(2, 1, 1, 3, 1), DataType.INT64);
            Assert.assertEquals(array.argSort(), expected);
            // test axis
            array = manager.arange(10).reshape(2, 1, 5);
            expected =
                    manager.create(new long[] {0, 0, 0, 0, 0, 1, 1, 1, 1, 1}, new Shape(2, 1, 5));
            Assert.assertEquals(array.argSort(0), expected);
            expected = manager.zeros(new Shape(2, 1, 5), DataType.INT64);
            Assert.assertEquals(array.argSort(1), expected);
            expected =
                    manager.create(new long[] {0, 1, 2, 3, 4, 0, 1, 2, 3, 4}, new Shape(2, 1, 5));
            Assert.assertEquals(array.argSort(2), expected);
        }
    }

    @Test
    public void testSort() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new float[] {2f, 1f, 4f, 3f});
            NDArray expected = manager.create(new float[] {1f, 2f, 3f, 4f});
            Assert.assertEquals(array.sort(), expected);
            // test multi-dim
            array =
                    manager.create(
                            new float[] {1.01f, 0.00f, 0.01f, -0.05f, 1.0f, 0.9f, 0.99f, 0.999f},
                            new Shape(2, 2, 1, 2));
            expected =
                    manager.create(
                            new float[] {0f, 1.01f, -0.05f, 0.01f, 0.9f, 1f, 0.99f, 0.999f},
                            new Shape(2, 2, 1, 2));
            Assert.assertEquals(array.sort(), expected);
            // test axis
            array =
                    manager.create(
                            new float[] {0f, 2f, 4f, 6f, 7f, 5f, 3f, 1f}, new Shape(2, 1, 2, 2));
            expected =
                    manager.create(
                            new float[] {0f, 2f, 3f, 1f, 7f, 5f, 4f, 6f}, new Shape(2, 1, 2, 2));
            Assert.assertEquals(array.sort(0), expected);
            expected =
                    manager.create(
                            new float[] {0f, 2f, 4f, 6f, 7f, 5f, 3f, 1f}, new Shape(2, 1, 2, 2));
            Assert.assertEquals(array.sort(1), expected);
            expected =
                    manager.create(
                            new float[] {0f, 2f, 4f, 6f, 3f, 1f, 7f, 5f}, new Shape(2, 1, 2, 2));
            Assert.assertEquals(array.sort(2), expected);
            expected =
                    manager.create(
                            new float[] {0f, 2f, 4f, 6f, 5f, 7f, 1f, 3f}, new Shape(2, 1, 2, 2));
            Assert.assertEquals(array.sort(3), expected);

            // scalar
            array = manager.create(5f);
            Assert.assertEquals(array.sort(), array);

            // zero-dim
            array = manager.create(new Shape(1, 0, 1));
            Assert.assertEquals(array.sort(), array);
            Assert.assertEquals(array.sort(0), array);
            Assert.assertEquals(array.sort(1), array);
            Assert.assertEquals(array.sort(2), array);
        }
    }

    @Test
    public void testSoftmax() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.ones(new Shape(10));
            NDArray expected = manager.zeros(new Shape(10)).add(0.1f);
            Assertions.assertAlmostEquals(array.softmax(0), expected);
            // test multi-dim
            array = manager.ones(new Shape(2, 3, 1, 3));
            expected = manager.zeros(new Shape(2, 3, 1, 3)).add(0.5f);
            Assertions.assertAlmostEquals(array.softmax(0), expected);
            expected = manager.zeros(new Shape(2, 3, 1, 3)).add(0.33333334f);
            Assertions.assertAlmostEquals(array.softmax(1), expected);
            expected = manager.ones(new Shape(2, 3, 1, 3));
            Assertions.assertAlmostEquals(array.softmax(2), expected);
            expected = manager.zeros(new Shape(2, 3, 1, 3)).add(0.33333334f);
            Assertions.assertAlmostEquals(array.softmax(3), expected);
            // test scalar
            array = manager.create(1f);
            Assertions.assertAlmostEquals(array.softmax(0), array);
            // test zero
            array = manager.create(new Shape(2, 0, 1));
            Assertions.assertAlmostEquals(array.softmax(0), array);
        }
    }

    @Test
    public void testCumsum() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.arange(10.0f);
            NDArray expected =
                    manager.create(new float[] {0f, 1f, 3f, 6f, 10f, 15f, 21f, 28f, 36f, 45f});
            Assert.assertEquals(array.cumSum(), expected);

            array = manager.create(new float[] {1f, 2f, 3f, 5f, 8f, 13f});
            expected = manager.create(new float[] {1f, 3f, 6f, 11f, 19f, 32f});
            Assert.assertEquals(array.cumSum(0), expected);

            // test multi-dim
            array = manager.arange(10.0f).reshape(2, 1, 5, 1);
            expected =
                    manager.create(
                            new float[] {0f, 1f, 2f, 3f, 4f, 5f, 7f, 9f, 11f, 13f},
                            new Shape(2, 1, 5, 1));
            Assert.assertEquals(array.cumSum(0), expected);

            array = manager.arange(10.0f).reshape(2, 1, 5, 1);
            expected =
                    manager.create(
                            new float[] {0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f},
                            new Shape(2, 1, 5, 1));
            Assert.assertEquals(array.cumSum(1), expected);

            array = manager.arange(10.0f).reshape(2, 1, 5, 1);
            expected =
                    manager.create(
                            new float[] {0f, 1f, 3f, 6f, 10f, 5f, 11f, 18f, 26f, 35f},
                            new Shape(2, 1, 5, 1));
            Assert.assertEquals(array.cumSum(2), expected);

            array = manager.arange(10.0f).reshape(2, 1, 5, 1);
            expected =
                    manager.create(
                            new float[] {0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f},
                            new Shape(2, 1, 5, 1));
            Assert.assertEquals(array.cumSum(3), expected);

            // Note that shape after cumsum op with zero-dim and scalar case change
            // test scalar
            array = manager.create(1f);
            expected = manager.create(new float[] {1f});
            Assert.assertEquals(array.cumSum(), expected);
            // test zero-dim
            array = manager.create(new Shape(2, 0));
            expected = manager.create(new Shape(0));
            Assert.assertEquals(array.cumSum(), expected);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testTile() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new float[] {1f, 2f, 3f, 4f}, new Shape(2, 2));

            NDArray tileAll = array.tile(2);
            NDArray expected =
                    manager.create(
                            new float[] {1, 2, 1, 2, 3, 4, 3, 4, 1, 2, 1, 2, 3, 4, 3, 4},
                            new Shape(4, 4));
            Assert.assertEquals(tileAll, expected, "Incorrect tile all");

            NDArray tileAxis = array.tile(0, 3);
            expected =
                    manager.create(
                            new float[] {1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4}, new Shape(6, 2));
            Assert.assertEquals(tileAxis, expected, "Incorrect tile on axis");

            NDArray tileArray = array.tile(new long[] {3, 1});
            Assert.assertEquals(tileArray, expected, "Incorrect tile array");

            NDArray tileShape = array.tile(new Shape(4));
            expected = manager.create(new float[] {1, 2, 1, 2, 3, 4, 3, 4}, new Shape(2, 4));
            Assert.assertEquals(tileShape, expected, "Incorrect tile shape");

            // scalar
            array = manager.create(5f);

            expected = manager.create(new float[] {5f, 5f, 5f}, new Shape(1, 3));
            Assert.assertEquals(array.tile(1, 3), expected);

            // zero-dim
            array = manager.create(new Shape(2, 0));
            expected = manager.create(new Shape(2, 0));
            Assert.assertEquals(array.tile(5), expected);
            expected = manager.create(new Shape(10, 0));
            Assert.assertEquals(array.tile(0, 5), expected);

            array.tile(new Shape(2, 2, 2));
        }
    }

    @Test
    public void testRepeat() {
        // TODO add scalar and zero-dim test cases after fix the bug in MXNet np.repeat
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new float[] {1f, 2f, 3f, 4f}, new Shape(2, 2));

            NDArray repeatAll = array.repeat(2);
            NDArray expected =
                    manager.create(
                            new float[] {1, 1, 2, 2, 1, 1, 2, 2, 3, 3, 4, 4, 3, 3, 4, 4},
                            new Shape(4, 4));
            Assert.assertEquals(repeatAll, expected, "Incorrect repeat all");

            NDArray repeatAxis = array.repeat(0, 3);
            expected =
                    manager.create(
                            new float[] {1, 2, 1, 2, 1, 2, 3, 4, 3, 4, 3, 4}, new Shape(6, 2));
            Assert.assertEquals(repeatAxis, expected, "Incorrect repeat on axis");

            NDArray repeatArray = array.repeat(new long[] {3, 1});
            Assert.assertEquals(repeatArray, expected, "Incorrect repeat array");

            NDArray repeatShape = array.repeat(new Shape(4));
            expected = manager.create(new float[] {1, 1, 2, 2, 3, 3, 4, 4}, new Shape(2, 4));
            Assert.assertEquals(repeatShape, expected, "Incorrect repeat shape");
        }
    }

    @Test
    public void testClip() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray original = manager.create(new float[] {1f, 2f, 3f, 4f});
            NDArray expected = manager.create(new float[] {2f, 2f, 3f, 3f});
            Assert.assertEquals(original.clip(2.0, 3.0), expected);
            Assert.assertEquals(original.clip(2, 3), expected);
            // multi-dim
            original =
                    manager.create(new float[] {5f, 4f, 2f, 5f, 6f, 7f, 2f, 22f, -23f, -2f})
                            .reshape(2, 1, 5, 1);
            expected =
                    manager.create(
                            new float[] {3f, 3f, 2f, 3f, 3f, 3f, 2f, 3f, 2f, 2f},
                            new Shape(2, 1, 5, 1));
            Assert.assertEquals(original.clip(2.0, 3.0), expected);
            Assert.assertEquals(original.clip(2, 3), expected);

            // scalar
            original = manager.create(5f);
            expected = manager.create(1f);
            Assert.assertEquals(original.clip(0.0, 1.0), expected);
            Assert.assertEquals(original.clip(0, 1), expected);
            // zero-dim
            original = manager.create(new Shape(0, 0));
            Assert.assertEquals(original.clip(0.0, 1.0), original);
            Assert.assertEquals(original.clip(0, 1), original);
        }
    }

    @Test
    public void testSwapAxes() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array =
                    manager.create(new float[] {0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f})
                            .reshape(new Shape(2, 5));
            NDArray expected =
                    manager.create(new float[] {0, 5, 1, 6, 2, 7, 3, 8, 4, 9}, new Shape(5, 2));
            Assert.assertEquals(array.swapAxes(0, 1), expected);

            // TODO MXNet engine crash
            // scalar
            // array = manager.create(5f);
            // array.swapaxes(0, 1);

            // test zero-dim
            array = manager.create(new Shape(2, 0));
            expected = manager.create(new Shape(0, 2));
            Assert.assertEquals(array.swapAxes(0, 1), expected);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testTranspose() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray original = manager.create(new float[] {1f, 2f, 3f, 4f}, new Shape(1, 2, 2));

            NDArray transposeAll = original.transpose();
            NDArray expected = manager.create(new float[] {1, 3, 2, 4}, new Shape(2, 2, 1));
            Assert.assertEquals(transposeAll, expected, "Incorrect transpose all");

            NDArray transpose = original.transpose(1, 0, 2);
            expected = manager.create(new float[] {1, 2, 3, 4}, new Shape(2, 1, 2));
            Assert.assertEquals(transpose, expected, "Incorrect transpose all");
            Assert.assertEquals(original.swapAxes(0, 1), expected, "Incorrect swap axes");

            // scalar
            original = manager.create(5f);
            Assert.assertEquals(original.transpose(), original);
            // throw exception
            original.transpose(0);

            // zero-dim
            original = manager.create(new Shape(2, 0, 1));
            expected = manager.create(new Shape(1, 0, 2));
            Assert.assertEquals(original.transpose(), expected);
            expected = manager.create(new Shape(2, 1, 0));
            Assert.assertEquals(original.transpose(0, 2, 1), expected);
        }
    }

    @Test
    public void testBroadcast() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new float[] {1, 2});
            NDArray broadcasted = array.broadcast(2, 2);
            NDArray expected = manager.create(new float[] {1, 2, 1, 2}, new Shape(2, 2));
            Assert.assertEquals(broadcasted, expected);
            // multi-dim
            array = manager.arange(4).reshape(2, 2);
            broadcasted = array.broadcast(3, 2, 2);
            expected = manager.arange(4).reshape(2, 2);
            expected = NDArrays.stack(new NDList(expected, expected, expected));
            Assert.assertEquals(broadcasted, expected);
            // scalar
            array = manager.create(1f);
            broadcasted = array.broadcast(2, 3, 2);
            expected = manager.ones(new Shape(2, 3, 2));
            Assert.assertEquals(broadcasted, expected);
            // zero-dim
            array = manager.create(new Shape(2, 0, 1));
            broadcasted = array.broadcast(2, 2, 0, 2);
            expected = manager.create(new Shape(2, 2, 0, 2));
            Assert.assertEquals(broadcasted, expected);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testArgMax() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array =
                    manager.create(
                            new float[] {
                                1, 2, 3, 4, 7, 9, 6, 23, 54, 234, 51, 33, 52, 0, 37, 34, 21, 59, 46,
                                32
                            },
                            new Shape(4, 5));
            NDArray argMax = array.argMax();
            NDArray expected = manager.create(9L);
            Assert.assertEquals(argMax, expected, "Argmax: Incorrect value");

            argMax = array.argMax(0);
            expected = manager.create(new long[] {2, 2, 3, 1, 1});
            Assert.assertEquals(argMax, expected, "Argmax: Incorrect value");

            argMax = array.argMax(1);
            expected = manager.create(new long[] {4, 4, 2, 2});
            Assert.assertEquals(argMax, expected, "Argmax: Incorrect value");

            // scalar
            array = manager.create(5f);
            // TODO the dtype should be int instead of float
            // Bug in MXNet to fix
            expected = manager.create(0L);
            Assert.assertEquals(array.argMax(), expected);
            Assert.assertEquals(array.argMax(0), expected);

            // TODO MXNet engine crash
            // zero-dim
            array = manager.create(new Shape(2, 0, 1));
            expected = manager.create(new Shape(0, 1), DataType.INT64);
            Assert.assertEquals(array.argMax(0), expected);
            // throw IllegalArgumentException
            array.argMax();
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testArgMin() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array =
                    manager.create(
                            new float[] {
                                1, 23, 3, 74, 4, 5, 6, -23, -54, 234, 154, 2, 50, 42, -34, 34, 23,
                                -59, 10, 2
                            },
                            new Shape(4, 5));
            NDArray argMin = array.argMin();
            NDArray expected = manager.create(17L);
            Assert.assertEquals(argMin, expected, "ArgMin: Incorrect value");

            argMin = array.argMin(0);
            expected = manager.create(new long[] {0, 2, 3, 1, 2});
            Assert.assertEquals(argMin, expected, "ArgMin: Incorrect value");

            argMin = array.argMin(1);
            expected = manager.create(new long[] {0, 3, 4, 2});
            Assert.assertEquals(argMin, expected, "ArgMin: Incorrect value");

            // scalar
            array = manager.create(1f);
            expected = manager.create(0L);
            Assert.assertEquals(array.argMin(), expected, "ArgMin: Incorrect value");
            Assert.assertEquals(array.argMin(0), expected, "ArgMin: Incorrect value");

            // zero-dim
            array = manager.create(new Shape(0, 1, 0));
            expected = manager.create(new Shape(0, 0), DataType.INT64);
            Assert.assertEquals(array.argMin(1), expected, "ArgMin: Incorrect value");
            array.argMin();
        }
    }

    @Test
    public void testEncodeDecode() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = manager.create(new long[] {0, 3, 4, 2}, new Shape(2, 2));
            byte[] bytes = array.encode();
            NDArray recovered = NDArray.decode(manager, bytes);
            Assert.assertEquals(recovered, array);
        }
    }
}
