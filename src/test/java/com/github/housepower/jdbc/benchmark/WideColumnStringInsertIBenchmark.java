package com.github.housepower.jdbc.benchmark;

import com.google.common.base.Strings;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class WideColumnStringInsertIBenchmark extends AbstractIBenchmark{

    AtomicInteger tableMaxId = new AtomicInteger();

    @Benchmark
    @Test
    public void benchInsertNative() throws Exception {
        withConnection(benchInsert, ConnectionType.NATIVE);
    }

    @Benchmark
    @Test
    public void benchInsertHttp() throws Exception {
        withConnection(benchInsert, ConnectionType.HTTP);
    }

    public WithConnection benchInsert = new WithConnection(){
        @Override
        public void apply(Connection connection) throws Exception {
            Statement stmt = connection.createStatement();

            int tableId = tableMaxId.getAndIncrement();
            String testTable = "test2_" + tableId;


            stmt.executeQuery("DROP TABLE IF EXISTS " + testTable);
            String createSQL = "CREATE TABLE " + testTable + " (";
            for (int i = 0; i < columnNum; i++) {
                createSQL += "col_" + i + " String";
                if (i + 1 != columnNum) {
                    createSQL += ",\n";
                }
            }
            createSQL += ") Engine = Log";

            stmt.executeQuery(createSQL);
            String params = Strings.repeat("?, ", columnNum);
            PreparedStatement pstmt = connection.prepareStatement("INSERT INTO "  + testTable +" values("   + params.substring(0, params.length()-2) + ")");


            for (int i = 0; i < batchSize; i++) {
                for (int j = 0; j < columnNum; j++ ) {
                    pstmt.setString(j + 1, j + 1 + "");
                }
                pstmt.addBatch();
            }
            int []res = pstmt.executeBatch();
            Assert.assertEquals(res.length, batchSize);
            stmt.executeQuery("DROP TABLE " + testTable);
        }
    };

}