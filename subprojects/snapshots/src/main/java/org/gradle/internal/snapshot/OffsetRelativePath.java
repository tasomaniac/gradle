/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.snapshot;

import java.util.function.IntUnaryOperator;

import static org.gradle.internal.snapshot.CaseSensitivity.CASE_SENSITIVE;
import static org.gradle.internal.snapshot.PathUtil.compareChars;
import static org.gradle.internal.snapshot.PathUtil.compareCharsIgnoringCase;
import static org.gradle.internal.snapshot.PathUtil.equalChars;
import static org.gradle.internal.snapshot.PathUtil.isFileSeparator;

public class OffsetRelativePath {
    private final String absolutePath;
    private final int offset;

    public static OffsetRelativePath of(String absolutePath, int offset) {
        return new OffsetRelativePath(absolutePath, offset);
    }

    public OffsetRelativePath(String absolutePath, int offset) {
        this.absolutePath = absolutePath;
        this.offset = offset;
    }

    public OffsetRelativePath withNewOffset(int addTo) {
        return new OffsetRelativePath(absolutePath, offset + addTo);
    }

    public OffsetRelativePath withThisOffset() {
        return new OffsetRelativePath(absolutePath, absolutePath.length() + 1);
    }

    public int length() {
        return absolutePath.length() - offset;
    }

    public String getAsString() {
        return absolutePath.substring(offset);
    }

    public boolean isThis() {
        return absolutePath.length() + 1 == offset;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    /**
     * Returns the size of the common prefix of a path and a sub-path of another path starting at on offset.
     *
     * The size of the common prefix does not include the last line separator.
     */
    public int sizeOfCommonPrefix(String relativePath, CaseSensitivity caseSensitivity) {
        int pos = 0;
        int lastSeparator = 0;
        int maxPos = Math.min(relativePath.length(), absolutePath.length() - offset);
        for (; pos < maxPos; pos++) {
            char charInPath1 = relativePath.charAt(pos);
            char charInPath2 = absolutePath.charAt(pos + offset);
            if (!equalChars(charInPath1, charInPath2, caseSensitivity)) {
                break;
            }
            if (isFileSeparator(charInPath1)) {
                lastSeparator = pos;
            }
        }
        if (pos == maxPos) {
            if (relativePath.length() == absolutePath.length() - offset) {
                return pos;
            }
            if (pos < relativePath.length() && isFileSeparator(relativePath.charAt(pos))) {
                return pos;
            }
            if (pos < absolutePath.length() - offset && isFileSeparator(absolutePath.charAt(pos + offset))) {
                return pos;
            }
        }
        return lastSeparator;
    }

    /**
     * Compares based on the first segment of two paths.
     *
     * Similar to {@link #sizeOfCommonPrefix(String, CaseSensitivity)},
     * only that this methods compares the first segment of the paths if there is no common prefix.
     *
     * The paths must not start with a separator, taking into account the offset
     *
     * For example, this method returns:
     *     some/path == some/other
     *     some1/path < some2/other
     *     some/path > some1/other
     *     some/same == some/same/more
     *
     * @return 0 if the two paths have a common prefix, and the comparison of the first segment of each path if not.
     */
    public int compareWithCommonPrefix(String relativePath, CaseSensitivity caseSensitivity) {
        int maxPos = Math.min(relativePath.length(), absolutePath.length() - offset);
        int accumulatedValue = 0;
        for (int pos = 0; pos < maxPos; pos++) {
            char charInPath1 = relativePath.charAt(pos);
            char charInPath2 = absolutePath.charAt(pos + offset);
            int comparedChars = compareCharsIgnoringCase(charInPath1, charInPath2);
            if (comparedChars != 0) {
                return comparedChars;
            }
            accumulatedValue = computeCombinedCompare(accumulatedValue, charInPath1, charInPath2, caseSensitivity == CASE_SENSITIVE);
            if (isFileSeparator(charInPath1)) {
                if (pos > 0) {
                    return accumulatedValue;
                }
            }
        }
        if (relativePath.length() == absolutePath.length() - offset) {
            return accumulatedValue;
        }
        if (relativePath.length() > absolutePath.length() - offset) {
            return isFileSeparator(relativePath.charAt(maxPos)) ? accumulatedValue : 1;
        }
        return isFileSeparator(absolutePath.charAt(maxPos + offset)) ? accumulatedValue : -1;
    }

    /**
     * Checks whether the relative path given by the absolute path and the offset has the given prefix.
     */
    public boolean isPrefix(String prefix, CaseSensitivity caseSensitivity) {
        int prefixLength = prefix.length();
        if (prefixLength == 0) {
            return true;
        }
        int pathLength = absolutePath.length();
        int endOfThisSegment = prefixLength + offset;
        if (pathLength < endOfThisSegment) {
            return false;
        }
        for (int i = prefixLength - 1, j = endOfThisSegment - 1; i >= 0; i--, j--) {
            if (!equalChars(prefix.charAt(i), absolutePath.charAt(j), caseSensitivity)) {
                return false;
            }
        }
        return endOfThisSegment == pathLength || isFileSeparator(absolutePath.charAt(endOfThisSegment));
    }

    /**
     * Determines whether a relative path has the given prefix,
     * and returns the comparison of prefix and the path if it has not.
     *
     * Similar to {@link #isPrefix}, only that it returns whether the path is bigger or smaller than the prefix.
     *
     * Examples:
     *   some/start == some/start/subpath
     *   some/start == some/start
     *   some/a     < some/start
     *   some/b     > some/a
     *
     * @param prefix prefix to compare to
     */
    public int compareToPrefix(String prefix, CaseSensitivity caseSensitivity) {
        int pathLength = absolutePath.length();
        int prefixLength = prefix.length();
        int endOfThisSegment = prefixLength + offset;
        if (pathLength < endOfThisSegment) {
            return comparePaths(prefix, caseSensitivity);
        }
        return comparePathRegions(prefix, prefixLength, caseSensitivity == CASE_SENSITIVE,
            accumulatedValue -> endOfThisSegment == pathLength || isFileSeparator(absolutePath.charAt(endOfThisSegment))
                ? accumulatedValue
                : -1);
    }

    private int comparePaths(String relativePath, CaseSensitivity caseSensitivity) {
        int maxPos = Math.min(relativePath.length(), absolutePath.length() - offset);
        return comparePathRegions(relativePath, maxPos, caseSensitivity == CASE_SENSITIVE,
            accumulatedValue -> {
                int lengthCompare = Integer.compare(relativePath.length(), absolutePath.length() - offset);
                return lengthCompare != 0
                    ? lengthCompare
                    : accumulatedValue;
            }
        );
    }

    private int comparePathRegions(String relativePath, int maxPos, boolean caseSensitive, IntUnaryOperator andThenCompare) {
        int accumulatedValue = 0;
        for (int pos = 0; pos < maxPos; pos++) {
            char charInPath1 = relativePath.charAt(pos);
            char charInPath2 = absolutePath.charAt(pos + offset);
            int comparedChars = compareCharsIgnoringCase(charInPath1, charInPath2);
            if (comparedChars != 0) {
                return comparedChars;
            }
            accumulatedValue = computeCombinedCompare(accumulatedValue, charInPath1, charInPath2, caseSensitive);
            if (accumulatedValue != 0 && isFileSeparator(charInPath1)) {
                return accumulatedValue;
            }
        }
        return andThenCompare.applyAsInt(accumulatedValue);
    }

    private static int computeCombinedCompare(int previousCombinedValue, char charInPath1, char charInPath2, boolean caseSensitive) {
        if (!caseSensitive) {
            return 0;
        }
        return previousCombinedValue == 0
            ? compareChars(charInPath1, charInPath2)
            : previousCombinedValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OffsetRelativePath that = (OffsetRelativePath) o;

        if (offset != that.offset) {
            return false;
        }
        return absolutePath.equals(that.absolutePath);
    }

    @Override
    public int hashCode() {
        int result = absolutePath.hashCode();
        result = 31 * result + offset;
        return result;
    }
}
