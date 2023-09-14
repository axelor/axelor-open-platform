type CacheEntry<T> = {
  value: T;
  accessAt: number;
  accessCount: number;
};

/**
 * Cache options.
 *
 */
export type CacheOptions = {
  /**
   * Maximum number of entries in the cache.
   */
  maxCount?: number;

  /**
   * Minimum number of hits before an entry is evicted.
   */
  minHits?: number;

  /**
   * Minimum time in milliseconds before an entry is evicted.
   */
  minTime?: number;
};

/**
 * A cache that evicts old and least used entries.
 *
 */
export class Cache<T> {
  #cache: Map<string, CacheEntry<T>>;
  #options: Required<CacheOptions>;

  /**
   * Create a new cache.
   *
   * @param options Cache options.
   */
  constructor(options: CacheOptions = {}) {
    this.#cache = new Map();
    this.#options = {
      maxCount: options.maxCount || 1000,
      minHits: options.minHits || 5,
      minTime: options.minTime || 1000 * 60 * 60,
    };
  }

  /**
   * Clear the cache.
   */
  clear() {
    this.#cache.clear();
  }

  /**
   * Remove an entry from the cache.
   *
   * @param key cache key
   */
  delete(key: string) {
    this.#cache.delete(key);
  }

  /**
   * Get an entry from the cache.
   *
   * @param key cache key
   * @returns cached value or undefined if not found
   */
  get(key: string): T | undefined {
    const entry = this.#cache.get(key);
    if (entry) {
      entry.accessAt = Date.now();
      entry.accessCount += 1;
      return entry.value;
    }
  }

  /**
   * Put an entry in the cache.
   *
   * @param key cache key
   * @param value the value to cache
   */
  put(key: string, value: T) {
    this.#cache.set(key, {
      accessAt: Date.now(),
      accessCount: 1,
      value,
    });

    // evict old and least used entries
    this.#evictLeastUsed();
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

/**
 * A cache that loads values on cache misses.
 *
 */
export class LoadingCache<T> extends Cache<T> {
  /**
   * Get an entry from the cache or load it if not found.
   *
   * @param key cache key
   * @param loader function to load the value
   * @returns cached value or loaded value
   */
  get(key: string): T | undefined;
  get(key: string, loader: (key: string) => T): T;
  get(key: string, loader?: (key: string) => T): T | undefined {
    let value = super.get(key);
    if (value) {
      return value;
    }

    if (loader !== undefined) {
      value = loader(key);
      this.put(key, value);
    }

    return value;
  }
}
