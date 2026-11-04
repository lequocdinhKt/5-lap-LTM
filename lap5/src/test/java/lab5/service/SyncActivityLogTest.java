package lab5.service;

import lab5.model.SyncActivity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncActivityLogTest {
    @Test
    void keepsNewestActivityFirst() {
        SyncActivityLog log = new SyncActivityLog();
        log.accept(SyncActivity.success("A", "THÊM", "SP001"));
        log.accept(SyncActivity.failure("B", "LỖI", "Mất kết nối"));
        assertEquals("LỖI", log.snapshot().get(0).action());
        assertEquals("THÊM", log.snapshot().get(1).action());
    }
}
