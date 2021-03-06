package io.github.tjheslin1.patterdale.metrics;

import io.github.tjheslin1.patterdale.database.DBConnection;
import io.github.tjheslin1.patterdale.database.DBConnectionPool;
import io.github.tjheslin1.patterdale.metrics.probe.*;
import org.assertj.core.api.WithAssertions;
import org.junit.Test;
import org.slf4j.Logger;
import testutil.WithMockito;

import java.sql.*;
import java.util.List;

import static io.github.tjheslin1.patterdale.metrics.probe.Probe.probe;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class MetricsUseCaseTest implements WithAssertions, WithMockito {

    private static final Probe PROBE = probe("name","SQL", "exists", "name", "label");

    private final ResultSet resultSet = mock(ResultSet.class);
    private final ResultSetMetaData resultSetMetaData = mock(ResultSetMetaData.class);
    private final PreparedStatement preparedStatement = mock(PreparedStatement.class);
    private final Connection connection = mock(Connection.class);
    private final DBConnection dbConnection = mock(DBConnection.class);
    private final DBConnectionPool dbConnectionPool = mock(DBConnectionPool.class);
    private final Logger logger = mock(Logger.class);

    @Test
    public void scrapeMetricsReturnsSuccess() throws Exception {
        givenAllProbesAreSuccessful();

        List<OracleSQLProbe> probes = singletonList(new ExistsOracleSQLProbe(PROBE, dbConnectionPool, logger));

        MetricsUseCase metricsUseCase = new MetricsUseCase(probes);
        List<ProbeResult> probeResults = metricsUseCase.scrapeMetrics();

        assertThat(probeResults)
                .isEqualTo(singletonList(new ProbeResult(1, PROBE)));
    }

    @Test
    public void scrapeMetricsReturnsSuccessForMultipleProbes() throws Exception {
        when(resultSetMetaData.getColumnCount()).thenReturn(2);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getDouble(1)).thenReturn(4.5, 6.7);
        when(resultSet.getString(2)).thenReturn("example SQL");
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(any())).thenReturn(preparedStatement);
        when(dbConnection.connection()).thenReturn(connection);
        when(dbConnectionPool.pool()).thenReturn(dbConnection);

        List<OracleSQLProbe> probes = asList(
                new ListOracleSQLProbe(PROBE, dbConnectionPool, logger),
                new ListOracleSQLProbe(PROBE, dbConnectionPool, logger)
        );

        MetricsUseCase metricsUseCase = new MetricsUseCase(probes);
        List<ProbeResult> probeResults = metricsUseCase.scrapeMetrics();

        assertThat(probeResults).isEqualTo(asList(
                new ProbeResult(4.5, PROBE, singletonList("example SQL")),
                new ProbeResult(6.7, PROBE, singletonList("example SQL"))
        ));
    }

    @Test
    public void scrapeMetricsReturnsFailureIfAnyProbeFails() throws Exception {
        givenASuccessfulProbeFollowedByAFailedProbe();
        List<OracleSQLProbe> probes = asList(
                new ExistsOracleSQLProbe(PROBE, dbConnectionPool, logger),
                new ExistsOracleSQLProbe(PROBE, dbConnectionPool, logger)
        );

        MetricsUseCase metricsUseCase = new MetricsUseCase(probes);
        List<ProbeResult> probeResults = metricsUseCase.scrapeMetrics();

        assertThat(probeResults).isEqualTo(asList(
                new ProbeResult(1, PROBE),
                new ProbeResult(0, PROBE)
        ));
    }

    @Test
    public void scrapeMetricsReturnsFailureIfAllProbeFails() throws Exception {
        givenAllFailedProbes();
        List<OracleSQLProbe> probes = asList(
                new ExistsOracleSQLProbe(PROBE, dbConnectionPool, logger),
                new ExistsOracleSQLProbe(PROBE, dbConnectionPool, logger)
        );

        MetricsUseCase metricsUseCase = new MetricsUseCase(probes);
        List<ProbeResult> probeResults = metricsUseCase.scrapeMetrics();

        assertThat(probeResults).isEqualTo(asList(
                new ProbeResult(0, PROBE),
                new ProbeResult(0, PROBE)
        ));
    }

    private void givenAllProbesAreSuccessful() throws SQLException {
        givenProbesReturnValues(1);
    }

    private void givenASuccessfulProbeFollowedByAFailedProbe() throws SQLException {
        givenProbesReturnValues(1, 0);
    }

    private void givenAllFailedProbes() throws SQLException {
        givenProbesReturnValues(0);
    }

    private void givenProbesReturnValues(int firstValue, Integer... subsequenceValues) throws SQLException {
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(firstValue, subsequenceValues);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(any())).thenReturn(preparedStatement);
        when(dbConnection.connection()).thenReturn(connection);
        when(dbConnectionPool.pool()).thenReturn(dbConnection);
    }
}