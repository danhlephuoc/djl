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
package software.amazon.ai.modality.cv;

/**
 * A {@code Rectangle} specifies an area in a coordinate space that is enclosed by the {@code
 * Rectangle} object's upper-left point {@link Point} in the coordinate space, its width, and its
 * height.
 */
public class Rectangle implements BoundingBox {

    Point point;
    int width;
    int height;

    /**
     * Constructs a new {@code Rectangle} whose upper-left corner is specified as {@code (x,y)} and
     * whose width and height are specified by the arguments of the same name.
     *
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     * @param width the width of the {@code Rectangle}
     * @param height the height of the {@code Rectangle}
     */
    public Rectangle(int x, int y, int width, int height) {
        this(new Point(x, y), width, height);
    }

    /**
     * Constructs a new {@code Rectangle} whose upper-left corner is specified as coordinate {@code
     * point} and whose width and height are specified by the arguments of the same name.
     *
     * @param point upper-left corner of the coordinate
     * @param width the width of the {@code Rectangle}
     * @param height the height of the {@code Rectangle}
     */
    public Rectangle(Point point, int width, int height) {
        this.point = point;
        this.width = width;
        this.height = height;
    }

    /** {@inheritDoc} */
    @Override
    public Rectangle getBounds() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PathIterator getPath() {
        return new PathIterator() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < 4;
            }

            @Override
            public void next() {
                if (index > 3) {
                    throw new IllegalStateException("No more path in iterator.");
                }
                ++index;
            }

            @Override
            public Point currentPoint() {
                switch (index) {
                    case 0:
                        return point;
                    case 1:
                        return new Point(point.getX() + width, point.getY());
                    case 2:
                        return new Point(point.getX() + width, point.getY() + height);
                    case 3:
                        return new Point(point.getX(), point.getY() + height);
                    default:
                        throw new AssertionError("Invalid index: " + index);
                }
            }
        };
    }

    public Point getPoint() {
        return point;
    }

    public int getX() {
        return point.getX();
    }

    public int getY() {
        return point.getY();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        double x = point.getX();
        double y = point.getY();
        return "[x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ']';
    }
}