/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.annotations;

/** The change track event types. */
public enum TrackEvent {
  ALWAYS,
  CREATE,
  UPDATE,
  DEFAULT
}
