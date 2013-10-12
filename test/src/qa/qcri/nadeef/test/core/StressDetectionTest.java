/*
 * QCRI, NADEEF LICENSE
 * NADEEF is an extensible, generalized and easy-to-deploy data cleaning platform built at QCRI.
 * NADEEF means "Clean" in Arabic
 *
 * Copyright (c) 2011-2013, Qatar Foundation for Education, Science and Community Development (on
 * behalf of Qatar Computing Research Institute) having its principle place of business in Doha,
 * Qatar with the registered address P.O box 5825 Doha, Qatar (hereinafter referred to as "QCRI")
 *
 * NADEEF has patent pending nevertheless the following is granted.
 * NADEEF is released under the terms of the MIT License, (http://opensource.org/licenses/MIT).
 */

package qa.qcri.nadeef.test.core;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import qa.qcri.nadeef.core.datamodel.CleanPlan;
import qa.qcri.nadeef.core.datamodel.NadeefConfiguration;
import qa.qcri.nadeef.core.pipeline.CleanExecutor;
import qa.qcri.nadeef.core.util.Bootstrap;
import qa.qcri.nadeef.core.util.Violations;
import qa.qcri.nadeef.core.util.sql.DBConnectionPool;
import qa.qcri.nadeef.core.util.sql.DBInstaller;
import qa.qcri.nadeef.test.NadeefTestBase;
import qa.qcri.nadeef.test.TestDataRepository;
import qa.qcri.nadeef.tools.DBConfig;
import qa.qcri.nadeef.tools.Tracer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * Stress testing on detection.
 */
@RunWith(Parameterized.class)
public class StressDetectionTest extends NadeefTestBase {
    public StressDetectionTest(String config_) {
        super(config_);
    }

    @Before
    public void setUp() {
        try {
            Bootstrap.start(testConfig);
            NadeefConfiguration.setMaxIterationNumber(1);
            Tracer.setVerbose(true);
            Tracer.setInfo(true);
            DBInstaller.uninstall(NadeefConfiguration.getDbConfig());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @After
    public void teardown() {
        Tracer.printDetectSummary("");
        Bootstrap.shutdown();
    }

    @Test
    public void cleanExecutorTest10k() {
        Tracer.setVerbose(false);
        try {
            CleanPlan cleanPlan = TestDataRepository.getStressPlan10k();
            CleanExecutor executor =
                new CleanExecutor(cleanPlan, NadeefConfiguration.getDbConfig());
            List<String> tableNames = cleanPlan.getRule().getTableNames();
            int correctResult =
                getViolationCount(
                    cleanPlan.getSourceDBConfig(),
                    tableNames.get(0),
                    "zipcode",
                    "city"
                );
            executor.detect();
            verifyViolationResult(correctResult, executor.getConnectionPool());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void cleanExecutorTest30k() {
        Tracer.setVerbose(false);
        Tracer.setInfo(true);
        try {
            CleanPlan cleanPlan = TestDataRepository.getStressPlan30k();
            List<String> tableNames = cleanPlan.getRule().getTableNames();
            int correctResult =
                getViolationCount(
                    cleanPlan.getSourceDBConfig(),
                    tableNames.get(0),
                    "zipcode",
                    "city"
                );
            CleanExecutor executor = new CleanExecutor(cleanPlan);
            executor.detect();
            verifyViolationResult(correctResult, executor.getConnectionPool());
            Tracer.printDetectSummary("");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void cleanExecutorTest40k() {
        Tracer.setVerbose(false);
        Tracer.setInfo(true);
        try {
            CleanPlan cleanPlan = TestDataRepository.getStressPlan40k();
            List<String> tableNames = cleanPlan.getRule().getTableNames();
            int correctResult =
                getViolationCount(
                    cleanPlan.getSourceDBConfig(),
                    tableNames.get(0),
                    "zipcode",
                    "city"
                );
            CleanExecutor executor = new CleanExecutor(cleanPlan);
            executor.detect();
            verifyViolationResult(correctResult, executor.getConnectionPool());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void cleanExecutorTest80k() {
        Tracer.setVerbose(false);
        Tracer.setInfo(true);
        try {
            CleanPlan cleanPlan = TestDataRepository.getStressPlan80k();
            List<String> tableNames = cleanPlan.getRule().getTableNames();
            int correctResult =
                getViolationCount(
                    cleanPlan.getSourceDBConfig(),
                    tableNames.get(0),
                    "zipcode",
                    "city"
                );
            CleanExecutor executor = new CleanExecutor(cleanPlan);
            executor.detect();
            verifyViolationResult(correctResult, executor.getConnectionPool());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private int getViolationCount(
        DBConfig dbConfig,
        String tableName,
        String left,
        String right
    ) {
        Connection conn = null;
        PreparedStatement ps1 = null, ps2 = null, ps3 = null;
        int totalViolation = 0;
        try {
            conn = DBConnectionPool.createConnection(dbConfig);
            ps1 =
                conn.prepareStatement(
                    "select distinct " + right + " from " + tableName + " where " + left + " = ?"
                );

            ps2 = conn.prepareStatement(
                "select count(*) as count from " + tableName + " where " + left + " = ? and " +
                    right + " = ?"
            );

            ps3 =
                conn.prepareStatement(
                    "select distinct(" + left + ") from " + tableName
                );

            ResultSet rs3 = ps3.executeQuery();
            while (rs3.next()) {
                String lvalue = rs3.getString(left);
                ps1.setString(1, lvalue);
                ps2.setString(1, lvalue);
                ResultSet rs1 = ps1.executeQuery();
                // distinct rhs with same lhs
                List<Integer> distincts = Lists.newArrayList();
                int totalDistinct = 0;
                while (rs1.next()) {
                    // calc. the violations
                    String rvalue = rs1.getString(right);
                    ps2.setString(2, rvalue);
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        distincts.add(rs2.getInt("count"));
                    }
                    totalDistinct += rs2.getInt("count");
                }

                int sum = 0;
                for (int i = 0; i < distincts.size(); i ++) {
                    int csize = distincts.get(i);
                    sum += csize * (totalDistinct - csize);
                    totalDistinct -= csize;
                }

                totalViolation += sum;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        } finally {
            try {
                if (ps1 != null) {
                    ps1.close();
                }

                if (ps2 != null) {
                    ps2.close();
                }

                if (ps3 != null) {
                    ps3.close();
                }

                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
                // ignore;
            }
        }

        return totalViolation * 4;
    }

    private void verifyViolationResult(int expectRow, DBConnectionPool pool) throws Exception {
        int rowCount = Violations.getViolationRowCount(pool);
        Assert.assertEquals(expectRow, rowCount);
    }
}
