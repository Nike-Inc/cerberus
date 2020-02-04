/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.config.database;

import java.sql.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * Port of org.apache.ibatis.type.OffsetDateTimeTypeHandler from old lib, the current one appears to
 * be broken? https://github.com/mybatis/mybatis-3/issues/1751
 */
public class OffsetDateTimeTypeHandler extends BaseTypeHandler<OffsetDateTime> {

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, OffsetDateTime parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setTimestamp(i, Timestamp.from(parameter.toInstant()));
  }

  @Override
  public OffsetDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnName);
    return getOffsetDateTime(timestamp);
  }

  @Override
  public OffsetDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnIndex);
    return getOffsetDateTime(timestamp);
  }

  @Override
  public OffsetDateTime getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    Timestamp timestamp = cs.getTimestamp(columnIndex);
    return getOffsetDateTime(timestamp);
  }

  private static OffsetDateTime getOffsetDateTime(Timestamp timestamp) {
    if (timestamp != null) {
      return OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
    }
    return null;
  }
}
