/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockColors {
    private static Map<Integer, String> colors = new ConcurrentHashMap<>();
    private static int count = 0;

    static {
        add("White");
        add("Orange");
        add("Magenta");
        add("Light Blue");
        add("Yellow");
        add("Lime");
        add("Pink");
        add("Gray");
        add("Light Gray");
        add("Cyan");
        add("Purple");
        add("Blue");
        add("Brown");
        add("Green");
        add("Red");
        add("Black");
    }

    private static void add(String value) {
        colors.put(count++, value);
    }

    public static boolean has(Integer key) {
        return colors.containsKey(key);
    }

    public static String get(Integer key) {
        return colors.getOrDefault(key, "Unknown color");
    }
}
