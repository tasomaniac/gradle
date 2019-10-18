/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.api.artifacts.dsl;

import org.gradle.api.Incubating;

/**
 * A {@code DependencyLockingHandler} manages the behaviour and configuration of dependency locking.
 *
 * @since 4.8
 */
public interface DependencyLockingHandler {

    /**
     * The supported lock modes:
     * <ul>
     *     <li>DEFAULT will load the lock state and verify resolution matches it</li>
     *     <li>STRICT will fail resolution if a locked configuration does not have a lock state</li>
     *     <li>LENIENT will load the lock state but not perform verification after resolution</li>
     * </ul>
     *
     * @since 6.1
     */
    @Incubating
    enum LockMode {
        STRICT,
        DEFAULT,
        LENIENT
    }

    /**
     * Convenience method for doing:
     *
     * configurations.all {
     *     resolutionStrategy.activateDependencyLocking()
     * }
     *
     */
    void lockAllConfigurations();

    /**
     * Convenience method for doing:
     *
     * configurations.all {
     *     resolutionStrategy.deactivateDependencyLocking()
     * }
     *
     * @since 6.0
     */
    @Incubating
    void unlockAllConfigurations();

    /**
     * Allows to query the lock mode currently configured
     *
     * @since 6.1
     */
    @Incubating
    LockMode getLockMode();

    /**
     * Sets the lock mode
     *
     * @since 6.1
     */
    @Incubating
    void setLockMode(LockMode mode);
}
