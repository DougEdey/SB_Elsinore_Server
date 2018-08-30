export type TaskID = number;

// for now we just use rAF but we should use some kind of fastDOM implementation
export function read(callback: FrameRequestCallback): TaskID {
  return requestAnimationFrame(callback);
}

export function write(callback: FrameRequestCallback): TaskID {
  return requestAnimationFrame(callback);
}

export function clear(taskID: TaskID): void {
  return cancelAnimationFrame(taskID);
}
