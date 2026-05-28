/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor;

import com.axelor.test.GuiceExtension;
import com.axelor.test.GuiceModules;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceExtension.class)
@GuiceModules({TestModule.class})
@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class AbstractTest {}
