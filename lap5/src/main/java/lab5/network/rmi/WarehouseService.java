package lab5.network.rmi;

import lab5.model.LoginResult;
import lab5.model.ProductInput;
import lab5.model.RegistrationResult;
import lab5.model.InventoryTransaction;
import lab5.model.Product;
import lab5.service.WarehouseException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

public interface WarehouseService extends Remote {
    LoginResult login(String username, String password) throws RemoteException;
    RegistrationResult register(String username, String password, String fullName) throws RemoteException;
    List<Product> listProducts(String search) throws RemoteException;
    Product createProduct(ProductInput input, String clientId, String username) throws RemoteException;
    Product updateProduct(UUID productId, ProductInput input, UUID lockToken,
                          long fencingToken, String clientId, String username)
            throws RemoteException;
    void deleteProduct(UUID productId, UUID lockToken, long fencingToken,
                       String clientId, String username) throws RemoteException;
    InventoryTransaction changeInventory(UUID productId, int quantity, boolean importStock,
                                         UUID lockToken, long fencingToken,
                                         String clientId, String username)
            throws RemoteException;
    List<InventoryTransaction> transactionHistory(UUID productId) throws RemoteException;
    boolean isPeerAvailable() throws RemoteException;
}
