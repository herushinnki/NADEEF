/*
 * QCRI, NADEEF LICENSE
 * NADEEF is an extensible, generalized and easy-to-deploy data cleaning platform built at QCRI.
 * NADEEF means “Clean” in Arabic
 *
 * Copyright (c) 2011-2013, Qatar Foundation for Education, Science and Community Development (on
 * behalf of Qatar Computing Research Institute) having its principle place of business in Doha,
 * Qatar with the registered address P.O box 5825 Doha, Qatar (hereinafter referred to as "QCRI")
 *
 * NADEEF has patent pending nevertheless the following is granted.
 * NADEEF is released under the terms of the MIT License, (http://opensource.org/licenses/MIT).
 */

package qa.qcri.nadeef.tools;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.base.Stopwatch;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import java.io.*;
import java.lang.String;
import java.sql.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * CSVTools is a simple tool which dumps CSV data into database given a table name.
 */
public class CSVTools {
    private static final int BULKSIZE = 1024;
    private static PushbackReader pushbackReader =
        new PushbackReader(new StringReader(""), 1024 * 1024);

    // <editor-fold desc="Public methods">

    /**
     * Reads the content from CSV file.
     * @param file CSV file.
     * @param separator separator.
     * @return a list of tokens (the header line is skipped).
     */
    public static List<String[]> read(File file, String separator) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String[]> result = Lists.newArrayList();
        String line;
        int count = 0;
        while ((line = reader.readLine()) != null) {
            if (Strings.isNullOrEmpty(line)) {
                continue;
            }

            count ++;
            // skip the header
            if (count == 1) {
                continue;
            }

            String[] tokens = line.split(separator);
            result.add(tokens);
        }
        return result;
    }

    /**
     * Dumps CSV file content into a database with default schema name and generated table name.
     * @param conn JDBC connection.
     * @param file CSV file.
     * @return new created table name.
     */
    public static String dump(Connection conn, File file)
            throws IllegalAccessException, SQLException, IOException {
        String fileName = Files.getNameWithoutExtension(file.getName());
        String tableName = dump(conn, file, fileName, true);
        return tableName;
    }

    /**
     * Dumps CSV file content into a specified database. It replaces the table if the table
     * already existed.
     * @param conn JDBC connection.
     * @param file CSV file.
     * @param tableName new created table name.
     * @param overwrite it overrites existing table if it exists.
     *
     * @return new created table name.
     */
    public static String dump(
            Connection conn,
            File file,
            String tableName,
            boolean overwrite
    ) throws IllegalAccessException, SQLException, IOException {
        Tracer tracer = Tracer.getTracer(CSVTools.class);
        Stopwatch stopwatch = new Stopwatch().start();
        String fullTableName = null;

        try {
            if (conn.isClosed()) {
                throw new IllegalAccessException("JDBC connection is already closed.");
            }

            conn.setAutoCommit(false);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder header = new StringBuilder(reader.readLine());
            // TODO: make it other DB compatible
            header.insert(0, "TID SERIAL PRIMARY KEY,");
            fullTableName = "csv_" + tableName;

            // overwrite existing table?
            if (!overwrite) {
                DatabaseMetaData meta = conn.getMetaData();
                ResultSet tables = meta.getTables(null, null, fullTableName, null);
                if (tables.next()) {
                    tracer.info(
                        "Found table " + fullTableName + " exists and choose not to overwrite."
                    );
                    return fullTableName;
                }
            }

            Statement stat = conn.createStatement();
            stat.setFetchSize(1024);
            String sql = "DROP TABLE IF EXISTS " + fullTableName + " CASCADE";
            tracer.verbose(sql);
            stat.execute(sql);


            // create the table
            sql = "CREATE TABLE " + fullTableName + "( " + header + ")";
            tracer.verbose(sql);
            stat.execute(sql);
            tracer.info("Successfully created table " + fullTableName);

            // Batch load the data
            StringBuilder sb = new StringBuilder();
            CopyManager copyManager = ((PGConnection)conn).getCopyAPI();

            int lineCount = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                sb.append(lineCount + 1);
                sb.append(',');
                sb.append(line);
                sb.append('\n');
                if (lineCount % BULKSIZE == 0) {
                    pushbackReader.unread(sb.toString().toCharArray());
                    copyManager.copyIn(
                        "COPY " + fullTableName + " FROM STDIN WITH CSV",
                        pushbackReader
                    );
                    sb.delete(0, sb.length());
                }
                lineCount ++;
            }

            pushbackReader.unread(sb.toString().toCharArray());
            copyManager.copyIn("COPY " + fullTableName + " FROM STDIN WITH CSV", pushbackReader);

            conn.commit();
            stat.close();
            tracer.info(
                "Dumped " + lineCount + " rows in " +
                stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms."
            );
            stopwatch.stop();
        } catch (Exception ex) {
            tracer.err("Cannot load file " + file.getName(), ex);
            if (conn != null) {
                PreparedStatement stat =
                    conn.prepareStatement("DROP TABLE IF EXISTS " + tableName);
                stat.execute();
                stat.close();
                conn.commit();
            }
        }
        return fullTableName;
    }
    // </editor-fold>
}