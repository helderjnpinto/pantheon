/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.util;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class FutureUtils {

  /**
   * Creates a {@link CompletableFuture} that is exceptionally completed by <code>error</code>.
   *
   * @param error the error to exceptionally complete the future with
   * @param <T> the type of CompletableFuture
   * @return a CompletableFuture exceptionally completed by <code>error</code>.
   */
  public static <T> CompletableFuture<T> completedExceptionally(final Throwable error) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(error);
    return future;
  }

  /**
   * Returns a new CompletionStage that, when the provided stage completes exceptionally, is
   * executed with the provided stage's exception as the argument to the supplied function.
   * Otherwise the returned stage completes successfully with the same value as the provided stage.
   *
   * <p>This is the exceptional equivalent to {@link CompletionStage#thenCompose(Function)}
   *
   * @param future the future to handle results or exceptions from
   * @param errorHandler the function returning a new CompletionStage
   * @param <T> the type of the CompletionStage's result
   * @return the CompletionStage
   */
  public static <T> CompletableFuture<T> exceptionallyCompose(
      final CompletableFuture<T> future,
      final Function<Throwable, CompletionStage<T>> errorHandler) {
    final CompletableFuture<T> result = new CompletableFuture<>();
    future.whenComplete(
        (value, error) -> {
          try {
            final CompletionStage<T> nextStep =
                error != null ? errorHandler.apply(error) : completedFuture(value);
            propagateResult(nextStep, result);
          } catch (final Throwable t) {
            result.completeExceptionally(t);
          }
        });
    return result;
  }

  /**
   * Propagates the result of one {@link CompletionStage} to a different {@link CompletableFuture}.
   *
   * <p>When <code>from</code> completes successfully, <code>to</code> will be completed
   * successfully with the same value. When <code>from</code> completes exceptionally, <code>to
   * </code> will be completed exceptionally with the same exception.
   *
   * @param from the CompletionStage to take results and exceptions from
   * @param to the CompletableFuture to propagate results and exceptions to
   * @param <T> the type of the success value
   */
  public static <T> void propagateResult(
      final CompletionStage<T> from, final CompletableFuture<T> to) {
    from.whenComplete(
        (value, error) -> {
          if (error != null) {
            to.completeExceptionally(error);
          } else {
            to.complete(value);
          }
        });
  }
}