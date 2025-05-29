/**
 * Copyright [2025 - 2025] Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jcustenborder.kafka.connect.spooldir;

import org.slf4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class RegexTimeoutUtils {

  private RegexTimeoutUtils() {
  }

  /**
   * Executes a callable task with a specified timeout.
   * If the task completes within the timeout, its result is returned.
   * If the task times out, is interrupted, or throws an exception,
   * a default value is returned and appropriate messages are logged.
   *
   * @param task                The callable task to execute (e.g., regex matching operation).
   * @param timeoutMillis       The maximum time to wait for the task to complete, in milliseconds.
   * @param defaultValueOnTimeout The value to return if the task times out, is interrupted, or fails.
   * @param log                 The SLF4J Logger instance to use for logging warnings or errors.
   * @param <V>                 The type of the result returned by the callable and the default value.
   * @return The result of the task if completed within the timeout, otherwise the default value.
   */
  public static <V> V executeWithTimeout(Callable<V> task, long timeoutMillis, V defaultValueOnTimeout, Logger log) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<V> future = executor.submit(task);

    try {
      return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      if (log != null) {
        log.warn("Operation timed out after {} ms. Potential ReDoS attack or very complex pattern/input.", timeoutMillis, e);
      }
      return defaultValueOnTimeout;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      if (log != null) {
        log.warn("Operation was interrupted.", e);
      }
      return defaultValueOnTimeout;
    } catch (Exception e) {
      Throwable cause = e.getCause();
      if (log != null) {
        log.error("Operation failed with an exception.", cause != null ? cause : e);
      }
      return defaultValueOnTimeout;
    } finally {
      executor.shutdownNow();
    }
  }
}