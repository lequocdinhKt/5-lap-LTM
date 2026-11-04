package lab5.network.rmi;

import lab5.model.InventoryTransaction;
import lab5.model.Product;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * Interface đồng bộ trực tiếp qua Java RMI giữa 2 Server (Server A và Server B).
 * Truyền trực tiếp các DTO Serializable giữa hai server.
 */
public interface SyncService extends Remote {

    /**
     * Kiểm tra trạng thái hoạt động của Server đối diện.
     */
    default boolean syncUser(UUID id, String username, String password, String fullName) throws RemoteException {
        throw new RemoteException("Peer does not support user synchronization. Update both servers.");
    }

    boolean ping() throws RemoteException;

    /**
     * Đồng bộ thêm mới sản phẩm sang Server đối diện.
     */
    boolean syncCreateProduct(Product product) throws RemoteException;

    /**
     * Đồng bộ cập nhật thông tin sản phẩm sang Server đối diện.
     */
    boolean syncUpdateProduct(Product product) throws RemoteException;

    /**
     * Đồng bộ xóa mềm sản phẩm sang Server đối diện.
     */
    boolean syncDeleteProduct(UUID productId, long fencingToken) throws RemoteException;

    /**
     * Đồng bộ cập nhật tồn kho và lưu lịch sử giao dịch sang Server đối diện.
     */
    boolean syncInventory(UUID productId, int newQuantity, long fencingToken,
                          InventoryTransaction transaction) throws RemoteException;

}
