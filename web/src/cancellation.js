// Preparation can download a model and does not accept the SDK's request AbortSignal.
// Race cancellation locally too, and discard late results in ai.js.
export function cancellable(operation, signal, timeoutMs = 180000) {
  return new Promise((resolve, reject) => {
    let timer;
    const abort = () => finish(reject, new Error("CANCELLED"));
    const finish = (done, value) => {
      clearTimeout(timer);
      signal.removeEventListener("abort", abort);
      done(value);
    };
    if (signal.aborted) {
      reject(new Error("CANCELLED"));
      return;
    }
    signal.addEventListener("abort", abort, { once: true });
    timer = setTimeout(() => finish(reject, new Error("TIMEOUT")), timeoutMs);
    Promise.resolve(operation).then(
      (value) => finish(resolve, value),
      (error) => finish(reject, error),
    );
  });
}
