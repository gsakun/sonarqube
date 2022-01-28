/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.auth.saml;

import com.onelogin.saml2.Auth;
import org.joda.time.Instant;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.SamlMessageIdDto;

import static java.util.Objects.requireNonNull;

@ServerSide
public class SamlMessageIdChecker {

  private final DbClient dbClient;

  public SamlMessageIdChecker(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public void check(Auth auth) {
    String messageId = requireNonNull(auth.getLastMessageId(), "Message ID is missing");
    Instant lastAssertionNotOnOrAfter = auth.getLastAssertionNotOnOrAfter().stream()
      .sorted()
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Missing NotOnOrAfter element"));
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.samlMessageIdDao().selectByMessageId(dbSession, messageId)
        .ifPresent(m -> {
          throw new IllegalArgumentException("This message has already been processed");
        });
      dbClient.samlMessageIdDao().insert(dbSession, new SamlMessageIdDto()
        .setMessageId(messageId)
        .setExpirationDate(lastAssertionNotOnOrAfter.getMillis()));
      dbSession.commit();
    }
  }

}
