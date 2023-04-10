export type Task<T> = () => Promise<T>;

export class TaskQueue {
  #queue: Task<any>[] = [];
  #running = false;

  #run() {
    if (this.#running) return;
    this.#running = true;
    const task = this.#queue.shift();
    if (task) {
      task().finally(() => {
        this.#running = false;
        this.#run();
      });
    } else {
      this.#running = false;
    }
  }

  add<T>(task: Task<T>) {
    return new Promise<T>((resolve, reject) => {
      this.#queue.push(() => task().then(resolve).catch(reject));
      this.#run();
    });
  }
}
