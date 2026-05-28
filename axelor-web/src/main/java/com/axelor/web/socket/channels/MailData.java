/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.socket.channels;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
public record MailData(
    MailCommand command, String model, Long recordId, List<Map<String, Object>> messages)
    implements Serializable {}
