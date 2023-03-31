type CacheEntry<T> = {
  value: T;
  accessAt: number;
  accessCount: number;
};

export type CacheOptions = {
  maxCount?: number;
  minHits?: number;
  minTime?: number;
};

export class Cache<T> {
  #cache: Map<string, CacheEntry<T>>;
  #options: Required<CacheOptions>;

  constructor(options: CacheOptions = {}) {
    this.#cache = new Map();
    this.#options = {
      maxCount: options.maxCount || 1000,
      minHits: options.minHits || 5,
      minTime: options.minTime || 1000 * 60 * 60,
    };
  }

  clear() {
    this.#cache.clear();
  }

  getOrLoad(key: string, load: () => T): T {
    const entry = this.#cache.get(key);
    if (entry) {
      entry.accessAt = Date.now();
      entry.accessCount += 1;
      return entry.value;
    }

    const value = load();
    this.#cache.set(key, {
      accessAt: Date.now(),
      accessCount: 1,
      value,
    });

    // evict old and least used entries
    this.#evictLeastUsed();

    return value;
  }

  #evictLeastUsed() {
    const now = Date.now();
    const leastUsed: string[] = [];

    for (const [key, entry] of this.#cache.entries()) {
      const diff = now - entry.accessAt;
      const hits = entry.accessCount;
      if (hits < this.#options.minHits && diff > this.#options.minTime) {
        leastUsed.push(key);
      }
    }

    for (const key of leastUsed) {
      this.#cache.delete(key);
    }
  }
}
