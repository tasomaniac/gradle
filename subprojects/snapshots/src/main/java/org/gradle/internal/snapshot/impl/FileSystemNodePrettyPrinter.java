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

package org.gradle.internal.snapshot.impl;

import org.gradle.internal.snapshot.AbstractIncompleteSnapshotWithChildren;
import org.gradle.internal.snapshot.CompleteDirectorySnapshot;
import org.gradle.internal.snapshot.FileSystemNode;
import org.gradle.internal.snapshot.MetadataSnapshot;
import org.gradle.internal.snapshot.PathCompressingSnapshotWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FileSystemNodePrettyPrinter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemNodePrettyPrinter.class);

    public static void prettyPrint(FileSystemNode node, int depth) {
        if (!LOGGER.isInfoEnabled()) {
            return;
        }
        if (depth == 0) {
            LOGGER.info((File.separatorChar == '/' ? '/' : "") + node.getPathToParent());
        } else {
            LOGGER.info(String.format("%" + depth * 2 + "c{}{}", ' '), node.getPathToParent().replace(File.separatorChar, '/'), node instanceof MetadataSnapshot ? " | " + determineNodeType(node) : "");
        }
        if (node instanceof AbstractIncompleteSnapshotWithChildren) {
            ((AbstractIncompleteSnapshotWithChildren) node).children.forEach(child -> prettyPrint(child, depth + 1));
        } else if (node instanceof CompleteDirectorySnapshot) {
            ((CompleteDirectorySnapshot) node).getChildren().forEach(child -> prettyPrint(child, depth + 1));
        } else if (node instanceof PathCompressingSnapshotWrapper) {
            MetadataSnapshot snapshot = ((PathCompressingSnapshotWrapper) node).getMetadata().get();
            if (snapshot instanceof CompleteDirectorySnapshot) {
                ((CompleteDirectorySnapshot) snapshot).getChildren().forEach(child -> prettyPrint(child, depth + 1));
            }
        }
    }

    private static String determineNodeType(FileSystemNode node) {
        if (node instanceof PathCompressingSnapshotWrapper) {
            PathCompressingSnapshotWrapper pathCompressingNode = (PathCompressingSnapshotWrapper) node;
            return pathCompressingNode.getType().toString() + " | " + pathCompressingNode.getMetadata().get().getClass().getSimpleName();
        }
        return node.getClass().getSimpleName();
    }
}
