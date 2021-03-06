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

import io.github.tjheslin1.patterdale.ValueType;
import io.github.tjheslin1.patterdale.database.DBConnectionPool;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

/**
 * {@link OracleSQLProbe} implementation which expects the provided SQL to return rows with 2 columns.
 * The first column is expected to be a String representing a returned value which will be populated as a label in the metric.
 * The second column is expected to be a Double value which will be assigned as the metric value.
 */
public class ListOracleSQLProbe extends ValueType implements OracleSQLProbe {

    private final Probe probe;
    private final DBConnectionPool connectionPool;
    private final Logger logger;

    public ListOracleSQLProbe(Probe probe, DBConnectionPool connectionPool, Logger logger) {
        this.probe = probe;
        this.connectionPool = connectionPool;
        this.logger = logger;
    }

    /**
     * @return a List of {@link ProbeResult}. Each {@link ProbeResult} represents a row returned from the provided SQL.
     */
    @Override
    public List<ProbeResult> probe() {
        try (Connection connection = connectionPool.pool().connection();
             PreparedStatement preparedStatement = connection.prepareStatement(probe.query())) {
            ResultSet resultSet = preparedStatement.executeQuery();

            List<ProbeResult> probeResults = new ArrayList<>();
            while (resultSet.next()) {
                int columnCount = resultSet.getMetaData().getColumnCount();
                double metricValue = resultSet.getDouble(1);

                List<String> dynamicLabels = new ArrayList<>(columnCount - 1);
                for (int columnIndex = 2; columnIndex <= columnCount; columnIndex++) {
                    String dynamicLabel = resultSet.getString(columnIndex).replaceAll("\\s+", " ");
                    dynamicLabels.add(dynamicLabel);
                }

                probeResults.add(new ProbeResult(metricValue, probe, dynamicLabels));
            }

            return probeResults;
        } catch (Exception e) {
            String message = format("Error occurred executing query: '%s'", probe.query());
            logger.error(message, e);
            return emptyList();
        }
    }
}
