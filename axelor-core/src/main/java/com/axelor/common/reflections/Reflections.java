package com.axelor.common.reflections;

/**
 * The {@link Reflections} utilities provides fast and easy way to search for
 * resources and types.
 * 
 */
public final class Reflections {

	private Reflections() {
	}

	/**
	 * Return a {@link ClassFinder} to search for the sub types of the given
	 * type.
	 * 
	 * @param type
	 *            the super type
	 * @return an instance of {@link ClassFinder}
	 */
	public static <T> ClassFinder<T> findSubTypesOf(Class<T> type) {
		return new ClassFinder<>(type);
	}

	/**
	 * Return a {@link ClassFinder} to search for types.
	 * 
	 * @return an instance of {@link ClassFinder}
	 */
	public static ClassFinder<?> findTypes() {
		return findSubTypesOf(Object.class);
	}

	/**
	 * Return a {@link ResourceFinder} to search for resources.
	 * 
	 * @return an instance of {@link ResourceFinder}
	 */
	public static ResourceFinder findResources() {
		return new ResourceFinder();
	}
}
