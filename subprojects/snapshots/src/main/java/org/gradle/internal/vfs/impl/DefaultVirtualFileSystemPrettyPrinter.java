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

package org.gradle.internal.vfs.impl;

import org.gradle.internal.snapshot.AbstractIncompleteSnapshotWithChildren;
import org.gradle.internal.snapshot.FileSystemNode;
import org.gradle.internal.snapshot.MetadataSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DefaultVirtualFileSystemPrettyPrinter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultVirtualFileSystemPrettyPrinter.class);

    public static void prettyPrint(DefaultFileHierarchySet set) {
        if (LOGGER.isInfoEnabled()) {
            print(set.rootNode, 0);
        }
    }


    private static void print(FileSystemNode node, int depth) {
        if (depth == 0) {
            LOGGER.info((File.separatorChar == '/' ? '/' : "") + node.getPathToParent());
        } else {
            LOGGER.info(String.format("%" + depth * 2 + "c{}{}", ' '), node.getPathToParent().replace(File.separatorChar, '/'), node instanceof MetadataSnapshot ? " | " + ((MetadataSnapshot) node).getType() : "");
        }
        if (node instanceof AbstractIncompleteSnapshotWithChildren) {
            ((AbstractIncompleteSnapshotWithChildren) node).children.forEach(child -> print(child, depth + 1));
        }
    }
}
