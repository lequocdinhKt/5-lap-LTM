package lab5.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** Quản lý commit/rollback để application service chỉ mô tả thứ tự nghiệp vụ. */
public final class TransactionManager {
    private final DataSource dataSource;

    public TransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T execute(Work<T> work) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (Exception | Error error) {
                rollback(connection, error);
                throw error;
            }
        }
    }

    private static void rollback(Connection connection, Throwable original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }

    @FunctionalInterface
    public interface Work<T> {
        T apply(Connection connection) throws Exception;
    }
}
