/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.file;

import com.axelor.test.GuiceModules;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

@GuiceModules(AbstractBaseFile.FileStoreTestModule.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FsStoreTest extends AbstractBaseFile {}
