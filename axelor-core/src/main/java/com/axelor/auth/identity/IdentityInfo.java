/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.identity;

import com.axelor.auth.db.MFAMethod;
import java.util.List;

/** Holds identity verification requirements for a user. */
public record IdentityInfo(
    boolean requiresPassword, boolean requiresMfa, List<MFAMethod> mfaMethods) {}
