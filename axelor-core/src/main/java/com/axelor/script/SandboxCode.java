/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

/**
 * Marks the classes compiled by {@link GroovyScriptSupport}: scripts and templates ({@link
 * SandboxScript}), their closures ({@link SandboxClosure}) and the classes they declare (see {@link
 * GroovyClassCheck}).
 *
 * <p>The code of these classes is checked by the policy, and their supertypes are allowed by it, so
 * they are allowed as receivers. Their presence on the call stack also identifies sandboxed code,
 * see {@link GroovySandbox#denyFromSandbox(String)}.
 */
public interface SandboxCode {}
