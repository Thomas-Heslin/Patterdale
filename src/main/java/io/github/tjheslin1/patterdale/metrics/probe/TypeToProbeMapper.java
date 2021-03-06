/*
 * Copyright 2017 Thomas Heslin <tjheslin1@gmail.com>.
 *
 * This file is part of Patterdale-jvm.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.tjheslin1.patterdale.metrics.probe;

import io.github.tjheslin1.patterdale.database.DBConnectionPool;
import org.slf4j.Logger;

import static java.lang.String.format;

public class TypeToProbeMapper {

    private static final String EXISTS = "exists";
    private static final String LIST = "list";

    private final Logger logger;

    public TypeToProbeMapper(Logger logger) {
        this.logger = logger;
    }

    public OracleSQLProbe createProbe(String databaseName, DBConnectionPool dbConnectionPool, Probe probe) {
        Probe dbLabelledProbe = probe.dbLabelled(databaseName);
        switch (dbLabelledProbe.type) {
            case EXISTS: {
                return new ExistsOracleSQLProbe(dbLabelledProbe, dbConnectionPool, logger);
            }
            case LIST: {
                return new ListOracleSQLProbe(dbLabelledProbe, dbConnectionPool, logger);
            }
            default: {
                throw new IllegalArgumentException(
                        format("Expected the provided 'type' value '%s' to match an OracleSqlProbe definition.", dbLabelledProbe.type));
            }
        }
    }
}
