/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.stone.springboot.beecp.util;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/*
 * DataSource Util
 *
 *  @author Chris Liao
 */
public class DataSourceUtil {
    public static final int Type_Statement = 1;
    public static final int Type_PreparedStatement = 2;
    public static final int Type_CallableStatement = 3;

    public static Map<String, Boolean> getInitSQLMap() {
        Map<String, Boolean> sqlMap = new HashMap<>(10);
        sqlMap.put("drop table TEST_USER", Boolean.TRUE);
        sqlMap.put("drop table TEST_USER2", Boolean.TRUE);
        sqlMap.put("drop PROCEDURE BEECP_HELLO", Boolean.TRUE);

        String sqlBuilder3 = "CREATE PROCEDURE BEECP_HELLO()" +
                "PARAMETER STYLE JAVA READS SQL DATA LANGUAGE JAVA EXTERNAL NAME" +
                "'org.stone.springboot.beecp.derby.DerbyStoreProcedure.hello'";

        sqlMap.put("CREATE TABLE TEST_USER(USER_ID VARCHAR(10),USER_NAME VARCHAR(10))", Boolean.FALSE);
        sqlMap.put("CREATE TABLE TEST_USER2(USER_ID VARCHAR(10),USER_NAME VARCHAR(10))", Boolean.FALSE);
        sqlMap.put(sqlBuilder3, Boolean.FALSE);
        return sqlMap;
    }

    public static void executeSQLMap(DataSource ds, Map<String, Boolean> sqlMap, int statementType) throws SQLException {
        if (statementType == Type_Statement) {//Statement
            try (Connection con = ds.getConnection()) {
                for (Map.Entry<String, Boolean> sqlEntry : sqlMap.entrySet()) {
                    try (Statement st = con.createStatement()) {
                        try {
                            st.execute(sqlEntry.getKey());
                        } catch (SQLException e) {
                            if (sqlEntry.getValue().booleanValue()) {
                                e.printStackTrace(System.out);
                            } else {
                                throw e;
                            }
                        }
                    }
                }
            }
        } else if (statementType == Type_PreparedStatement) {//PreparedStatement
            try (Connection con = ds.getConnection()) {
                for (Map.Entry<String, Boolean> sqlEntry : sqlMap.entrySet()) {
                    try (PreparedStatement ps = con.prepareStatement(sqlEntry.getKey())) {
                        try {
                            ps.execute();
                        } catch (SQLException e) {
                            if (sqlEntry.getValue().booleanValue()) {
                                e.printStackTrace(System.out);
                            } else {
                                throw e;
                            }
                        }
                    }
                }
            }
        } else if (statementType == Type_CallableStatement) {//CallableStatement
            try (Connection con = ds.getConnection()) {
                for (Map.Entry<String, Boolean> sqlEntry : sqlMap.entrySet()) {
                    try (CallableStatement cs = con.prepareCall(sqlEntry.getKey())) {
                        try {
                            cs.execute();
                        } catch (SQLException e) {
                            if (sqlEntry.getValue().booleanValue()) {
                                e.printStackTrace(System.out);
                            } else {
                                throw e;
                            }
                        }
                    }
                }
            }
        }
    }
}
