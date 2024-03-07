package ua.railian.coroutines.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletionHandlerException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newCoroutineContext
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Launches a new coroutine without blocking the current thread and returns a reference to the coroutine as a [Job].
 * The coroutine is cancelled when the resulting job is [cancelled][Job.cancel].
 *
 * The coroutine context is inherited from a [CoroutineScope]. Additional context elements can be specified with [context] argument.
 * If the context does not have any dispatcher nor any other [ContinuationInterceptor], then [Dispatchers.Default] is used.
 * The parent job is inherited from a [CoroutineScope] as well, but it can also be overridden
 * with a corresponding [context] element.
 *
 * By default, the coroutine is immediately scheduled for execution.
 * Other start options can be specified via `start` parameter. See [CoroutineStart] for details.
 * An optional [start] parameter can be set to [CoroutineStart.LAZY] to start coroutine _lazily_. In this case,
 * the coroutine [Job] is created in _new_ state. It can be explicitly started with [start][Job.start] function
 * and will be started implicitly on the first invocation of [join][Job.join].
 *
 * Uncaught exceptions in this coroutine will not cancel the parent job in the context.
 *
 * See [newCoroutineContext] for a description of debugging facilities that are available for a newly created coroutine.
 *
 * @param onFailure a function which handles exception thrown by a coroutine.
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block the coroutine code which will be invoked in the context of the provided scope.
 */
public fun CoroutineScope.launchCatching(
    onFailure: (Throwable) -> Unit = {},
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch(
    context = context + CoroutineExceptionHandler { _, throwable -> onFailure(throwable) },
    start = start,
    block = block,
)

/**
 * Registers handler that is **synchronously** invoked once on completion of this job.
 * When the job is already complete, then the handler is immediately invoked
 * with the job's exception or cancellation cause or `null`. Otherwise, the handler will be invoked once when this
 * job is complete.
 *
 * The meaning of `cause` that is passed to the handler:
 * - Cause is `null` when the job has completed normally.
 * - Cause is an instance of [CancellationException] when the job was cancelled _normally_.
 *   **It should not be treated as an error**. In particular, it should not be reported to error logs.
 * - Otherwise, the job had _failed_.
 *
 * Installed [handler] should not throw any exceptions. If it does, they will get caught,
 * wrapped into [CompletionHandlerException], and rethrown, potentially causing crash of unrelated code.
 *
 * **Note**: Implementation of `handler` must be fast, non-blocking, and thread-safe.
 * This handler can be invoked concurrently with the surrounding code.
 * There is no guarantee on the execution context in which the [handler] is invoked.
 */
public fun <T : Job> T.onCompletion(
    handler: (Throwable?) -> Unit,
): T = apply { invokeOnCompletion(handler) }

/**
 * Registers handlers, one of which is **synchronously** invoked once on completion of this job.
 * When the job is already complete, then the handler is immediately invoked.
 *
 * Installed `handlers` should not throw any exceptions. If they does, they will get caught,
 * wrapped into [CompletionHandlerException], and rethrown, potentially causing crash of unrelated code.
 *
 * **Note**: Implementation of `handlers` must be fast, non-blocking, and thread-safe.
 * There is no guarantee on the execution context in which the `handler` is invoked.
 *
 * @param onSuccess is invoked when the job has completed normally.
 * @param onFailure is invoked when the job had _failed_.
 * @param onCancel is invoked when the job was cancelled _normally_ with [CancellationException].
 */
public inline fun <T : Job> T.onCompletion(
    crossinline onSuccess: () -> Unit = {},
    crossinline onFailure: (Throwable) -> Unit = {},
    crossinline onCancel: (CancellationException) -> Unit = {},
): T = onCompletion { throwable ->
    when (throwable) {
        null -> onSuccess()
        is CancellationException -> onCancel(throwable)
        else -> onFailure(throwable)
    }
}

/**
 * Register [handler], that is **synchronously** invoked once on completion of this job.
 * When the job is already complete, then the handler is immediately invoked.
 *
 * Installed [handler] should not throw any exceptions. If it does, they will get caught,
 * wrapped into [CompletionHandlerException], and rethrown, potentially causing crash of unrelated code.
 *
 * **Note**: Implementation of [handler] must be fast, non-blocking, and thread-safe.
 * There is no guarantee on the execution context in which the `handler` is invoked.
 *
 * @param handler is invoked when the job has completed normally.
 */
public inline fun <T : Job> T.onSuccess(
    crossinline handler: () -> Unit,
): T = onCompletion(onSuccess = handler)

/**
 * Register [handler], that is **synchronously** invoked once on completion of this job.
 * When the job is already complete, then the handler is immediately invoked.
 *
 * Installed [handler] should not throw any exceptions. If it does, they will get caught,
 * wrapped into [CompletionHandlerException], and rethrown, potentially causing crash of unrelated code.
 *
 * **Note**: Implementation of [handler] must be fast, non-blocking, and thread-safe.
 * There is no guarantee on the execution context in which the `handler` is invoked.
 *
 * @param handler is invoked when the job had _failed_.
 */
public inline fun <T : Job> T.onFailure(
    crossinline handler: (Throwable) -> Unit,
): T = onCompletion(onFailure = handler)

/**
 * Register [handler], that is **synchronously** invoked once on completion of this job.
 * When the job is already complete, then the handler is immediately invoked.
 *
 * Installed [handler] should not throw any exceptions. If it does, they will get caught,
 * wrapped into [CompletionHandlerException], and rethrown, potentially causing crash of unrelated code.
 *
 * **Note**: Implementation of [handler] must be fast, non-blocking, and thread-safe.
 * There is no guarantee on the execution context in which the `handler` is invoked.
 *
 * @param handler is invoked when the job was cancelled _normally_ with [CancellationException].
 */
public inline fun <T : Job> T.onCancel(
    crossinline handler: (CancellationException) -> Unit,
): T = onCompletion(onCancel = handler)
