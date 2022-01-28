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
package org.sonar.db.dump;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.sonar.db.SQDatabase;

import static com.google.common.base.Preconditions.checkState;

class SQSchemaDumper {
  private static final String LINE_SEPARATOR = System.lineSeparator();
  private static final String HEADER = "" +
    "###############################################################" + LINE_SEPARATOR +
    "####  Description of SonarQube's schema in H2 SQL syntax   ####" + LINE_SEPARATOR +
    "####                                                       ####" + LINE_SEPARATOR +
    "####   This file is autogenerated and stored in SCM to     ####" + LINE_SEPARATOR +
    "####   conveniently read the SonarQube's schema at any     ####" + LINE_SEPARATOR +
    "####   point in time.                                      ####" + LINE_SEPARATOR +
    "####                                                       ####" + LINE_SEPARATOR +
    "####          DO NOT MODIFY THIS FILE DIRECTLY             ####" + LINE_SEPARATOR +
    "####    use gradle task :server:sonar-db-dao:dumpSchema    ####" + LINE_SEPARATOR +
    "###############################################################" + LINE_SEPARATOR;
  private static final String TABLE_SCHEMA_MIGRATIONS = "SCHEMA_MIGRATIONS";
  private static final Comparator<String> SCHEMA_MIGRATIONS_THEN_NATURAL_ORDER = ((Comparator<String>) (o1, o2) -> {
    if (o1.equals(TABLE_SCHEMA_MIGRATIONS)) {
      return -1;
    }
    if (o2.equals(TABLE_SCHEMA_MIGRATIONS)) {
      return 1;
    }
    return 0;
  }).thenComparing(String.CASE_INSENSITIVE_ORDER);

  String dumpToText() throws SQLException {
    SQDatabase database = SQDatabase.newH2Database("SQSchemaDumper", true);
    database.start();

    try (Connection connection = database.getDataSource().getConnection();
         Statement statement = connection.createStatement()) {
      List<String> tableNames = getSortedTableNames(statement);

      StringBuilder res = new StringBuilder(HEADER);
      for (String tableName : tableNames) {
        res.append(LINE_SEPARATOR);
        dumpTable(statement, res, tableName);
      }
      return res.toString();
    }
  }

  /**
   * List of all tables in database sorted in natural order except that table {@link #TABLE_SCHEMA_MIGRATIONS SCHEMA_MIGRATIONS}
   * will always be first.
   */
  private List<String> getSortedTableNames(Statement statement) throws SQLException {
    checkState(statement.execute("SHOW TABLES"), "can't list tables");
    List<String> tableNames = new ArrayList<>();
    try (ResultSet rSet = statement.getResultSet()) {
      while (rSet.next()) {
        tableNames.add(rSet.getString(1));
      }
    }
    tableNames.sort(SCHEMA_MIGRATIONS_THEN_NATURAL_ORDER);
    return tableNames;
  }

  private void dumpTable(Statement statement, StringBuilder res, String tableName) throws SQLException {
    checkState(statement.execute("SCRIPT NODATA NOSETTINGS TABLE " + tableName), "Can't get schema dump of table %s", tableName);
    try (ResultSet resultSet = statement.getResultSet()) {
      while (resultSet.next()) {
        String sql = resultSet.getString(1);
        if (isIgnoredStatement(sql)) {
          continue;
        }

        String cleanedSql = sql
          .replaceAll(" \"PUBLIC\"\\.", " ")
          .replaceAll(" MEMORY TABLE ", " TABLE ");
        if (cleanedSql.startsWith("CREATE TABLE")) {
          cleanedSql = fixAutoIncrementIdColumn(cleanedSql);
        }
        res.append(cleanedSql).append(LINE_SEPARATOR);
      }
    }
  }

  private static boolean isIgnoredStatement(String sql) {
    return sql.startsWith("CREATE SEQUENCE") || sql.startsWith("CREATE USER") || sql.startsWith("--");
  }

  /**
   * Hacky hacky hack: H2 generates DDL for auto increment column which varies from one run to another and is hardly
   * readable for user.
   * It's currently reasonable to assume:
   * <ul>
   *   <li>all existing auto increment columns are called ID</li>
   *   <li>it's not a practice to create auto increment anymore => no new will be added which could have a different name</li>
   * </ul>
   */
  private String fixAutoIncrementIdColumn(String cleanedSql) {
    String res = fixAutoIncrementIdColumn(cleanedSql, "\"ID\" INTEGER DEFAULT (NEXT VALUE FOR ", "\"ID\" INTEGER NOT NULL AUTO_INCREMENT (1,1)");
    res = fixAutoIncrementIdColumn(res, "\"ID\" BIGINT DEFAULT (NEXT VALUE FOR ", "\"ID\" BIGINT NOT NULL AUTO_INCREMENT (1,1)");
    return res;
  }

  private static String fixAutoIncrementIdColumn(String sql, String src, String tgt) {
    int idAutoGenColumn = sql.indexOf(src);
    if (idAutoGenColumn < 0) {
      return sql;
    }

    int comma = sql.indexOf(",", idAutoGenColumn + 1);
    checkState(comma > -1, "can't find end of ID column declaration??");
    StringBuilder bar = new StringBuilder(sql);
    bar.replace(idAutoGenColumn, comma, tgt);
    return bar.toString();
  }
}
