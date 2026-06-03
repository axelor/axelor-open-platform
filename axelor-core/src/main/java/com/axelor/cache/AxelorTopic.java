/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

/** Distributed topic. Messages are delivered to all message listeners. */
public interface AxelorTopic {

  /**
   * Publishes the given message to the listeners.
   *
   * @param message the message to publish
   * @return the number of listeners that received the message
   */
  long publish(Object message);

  /**
   * Subscribes a listener to messages published to this topic.
   *
   * @param type type of message
   * @param listener the listener to be notified
   * @return the listener id, which can be used to {@link #removeListener(Integer...) remove} the
   *     listener
   */
  <M> int addListener(Class<M> type, MessageListener<? extends M> listener);

  /**
   * Removes the listener with the given id.
   *
   * @param listenerIds listener ids to remove
   */
  void removeListener(Integer... listenerIds);

  /**
   * A listener for messages received on a {@link AxelorTopic}.
   *
   * @param <M> message
   */
  @FunctionalInterface
  interface MessageListener<M> {

    /**
     * Invoked when a message is received.
     *
     * @param message the received message
     */
    void onMessage(M message);
  }
}
