/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

/**
 * Summary of a processed batch of audit log groups.
 *
 * @param processed the number of groups processed successfully
 * @param failed the number of groups which failed, their error being recorded for a later retry
 * @param lastAuditLogId the cursor to fetch the next batch from
 * @param hasMore whether groups may remain after this batch
 */
record BatchResult(int processed, int failed, long lastAuditLogId, boolean hasMore) {}
