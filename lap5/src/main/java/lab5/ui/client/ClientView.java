package lab5.ui.client;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import lab5.config.ClientConfig;
import lab5.model.InventoryTransaction;
import lab5.model.LoginResult;
import lab5.model.Product;
import lab5.model.ProductInput;
import lab5.service.ClientSettingsService;
import lab5.service.WarehouseClientService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class ClientView implements AutoCloseable {
    private final StackPane root = new StackPane();
    private final ClientConfig initialConfig;
    private final String configPath;
    private final String defaultUsername;
    private final ExecutorService connector = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "lab5-client-connect");
        thread.setDaemon(true);
        return thread;
    });
    private WarehouseClientService service;
    private Label toast;

    public ClientView(ClientConfig initialConfig, String configPath, String defaultUsername) {
        this.initialConfig = initialConfig;
        this.configPath = configPath;
        this.defaultUsername = defaultUsername == null ? "" : defaultUsername;
        root.getStyleClass().add("app-root");
        showAccess();
    }

    public StackPane root() {
        return root;
    }

    private void showAccess() {
        TextField host = field(initialConfig.serverHost(), "IP server");
        TextField port = field(String.valueOf(initialConfig.registryPort()), "Cổng Registry");
        Label connectionBadge = badge("Chưa kết nối", "badge-danger");
        Button connect = primary("Kết nối server");
        TextField loginUser = field(defaultUsername, "Tên đăng nhập");
        PasswordField loginPassword = password("Mật khẩu");
        Button login = primary("Đăng nhập");
        login.setDisable(true);
        TextField registerUser = field("", "Tên đăng nhập mới");
        TextField fullName = field("", "Họ và tên");
        PasswordField registerPassword = password("Mật khẩu");
        Button register = primary("Tạo tài khoản");
        register.setDisable(true);

        VBox connection = card("Kết nối RMI", host, port, connectionBadge, connect);
        TabPane tabs = new TabPane();
        Tab loginTab = new Tab("Đăng nhập", form(loginUser, loginPassword, login));
        Tab registerTab = new Tab("Đăng ký", form(registerUser, fullName, registerPassword, register));
        loginTab.setClosable(false);
        registerTab.setClosable(false);
        tabs.getTabs().addAll(loginTab, registerTab);
        tabs.getStyleClass().add("access-tabs");
        Label title = new Label("Quản lý kho phân tán");
        title.getStyleClass().add("hero-title");
        Label subtitle = new Label("Lab 5 · Java RMI · Client " + initialConfig.clientId());
        subtitle.getStyleClass().add("muted");
        VBox accessCard = new VBox(18, title, subtitle, connection, tabs);
        accessCard.getStyleClass().add("access-card");
        accessCard.setMaxWidth(520);
        StackPane access = new StackPane(accessCard);
        access.setPadding(new Insets(32));
        replace(access);

        connect.setOnAction(event -> connect(host, port, connectionBadge, connect, login, register, loginUser));
        login.setOnAction(event -> login(loginUser, loginPassword, login));
        register.setOnAction(event -> register(registerUser, registerPassword, fullName, register,
                loginUser, tabs, loginTab));
    }

    private void connect(TextField host, TextField port, Label badge, Button connect,
                         Button login, Button register, TextField username) {
        int selectedPort;
        try {
            selectedPort = Integer.parseInt(port.getText().trim());
            if (selectedPort < 1 || selectedPort > 65535) throw new NumberFormatException();
        } catch (NumberFormatException error) {
            showToast("Cổng Registry không hợp lệ.", true);
            return;
        }
        String selectedHost = host.getText().trim();
        connect.setDisable(true);
        connect.setText("Đang kết nối...");
        setBadge(badge, "Đang kết nối", "badge-warning");
        WarehouseClientService.connect(selectedHost, selectedPort, initialConfig.clientId(), connector)
                .whenComplete((connected, error) -> Platform.runLater(() -> {
                    connect.setDisable(false);
                    connect.setText("Kết nối lại");
                    if (error != null) {
                        setBadge(badge, "Mất kết nối", "badge-danger");
                        showToast(message(error), true);
                        return;
                    }
                    if (service != null) service.close();
                    service = connected;
                    service.onLeaseStatus(text -> Platform.runLater(() -> showToast(text, true)));
                    login.setDisable(false);
                    register.setDisable(false);
                    setBadge(badge, "Đã kết nối", "badge-success");
                    ClientSettingsService.save(Path.of(configPath), selectedHost, selectedPort,
                            initialConfig.clientId(), username.getText());
                    showToast("Đã kết nối tới RMI Registry.", false);
                }));
    }

    private void login(TextField username, PasswordField password, Button button) {
        if (service == null) return;
        button.setDisable(true);
        service.login(username.getText(), password.getText()).whenComplete((result, error) ->
                Platform.runLater(() -> {
                    button.setDisable(false);
                    password.clear();
                    if (error != null) showToast(message(error), true);
                    else if (!result.authenticated()) showToast(result.message(), true);
                    else showDashboard(result);
                }));
    }

    private void register(TextField username, PasswordField password, TextField fullName, Button button,
                          TextField loginUser, TabPane tabs, Tab loginTab) {
        if (service == null) return;
        button.setDisable(true);
        service.register(username.getText(), password.getText(), fullName.getText())
                .whenComplete((result, error) -> Platform.runLater(() -> {
                    button.setDisable(false);
                    password.clear();
                    if (error != null) showToast(message(error), true);
                    else if (!result.registered()) showToast(result.message(), true);
                    else {
                        loginUser.setText(result.username());
                        tabs.getSelectionModel().select(loginTab);
                        showToast("Đăng ký thành công. Hãy đăng nhập.", false);
                    }
                }));
    }

    private void showDashboard(LoginResult identity) {
        TableView<Product> table = productTable();
        TextField search = field("", "Tìm mã hoặc tên sản phẩm");
        Label peerBadge = badge("Đang kiểm tra peer", "badge-warning");
        Label state = muted("Sẵn sàng");
        Label selectedTitle = new Label("Chưa chọn sản phẩm");
        selectedTitle.getStyleClass().add("section-title");
        Label selectedDetail = muted("Chọn một dòng để thao tác.");
        selectedDetail.setWrapText(true);
        Button add = primary("+ Thêm sản phẩm");
        Button edit = secondary("Sửa");
        Button importStock = secondary("Nhập kho");
        Button exportStock = secondary("Xuất kho");
        Button history = secondary("Lịch sử");
        Button delete = danger("Xóa");
        List<Button> selectionActions = List.of(edit, importStock, exportStock, history, delete);
        selectionActions.forEach(button -> button.setDisable(true));

        table.getSelectionModel().selectedItemProperty().addListener((ignored, before, product) -> {
            selectionActions.forEach(button -> button.setDisable(product == null));
            selectedTitle.setText(product == null ? "Chưa chọn sản phẩm"
                    : product.productCode() + " · " + product.productName());
            selectedDetail.setText(product == null ? "Chọn một dòng để thao tác."
                    : "Danh mục: " + product.category() + "\nTồn kho: " + product.quantity()
                    + " · Giá: " + NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"))
                    .format(product.price()));
        });

        Button find = secondary("Tìm");
        Button refresh = secondary("Làm mới");
        HBox tools = new HBox(8, search, find, refresh, add);
        HBox.setHgrow(search, Priority.ALWAYS);
        tools.setAlignment(Pos.CENTER_LEFT);
        VBox detail = new VBox(12, new Label("SẢN PHẨM ĐÃ CHỌN"), selectedTitle, selectedDetail,
                edit, importStock, exportStock, history, delete);
        detail.getStyleClass().add("detail-panel");
        detail.setMinWidth(230);
        VBox tableArea = new VBox(12, tools, table, state);
        VBox.setVgrow(table, Priority.ALWAYS);
        SplitPane content = new SplitPane(tableArea, detail);
        content.setDividerPositions(0.76);
        content.getStyleClass().add("content-split");

        Region spacer = new Region();
        VBox sidebar = new VBox(12, navTitle("Lab 5"), navItem("Kho hàng", true),
                navItem("Đồng bộ RMI", false), spacer, peerBadge);
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getStyleClass().add("sidebar");
        Label heading = new Label("Quản lý kho hàng");
        heading.getStyleClass().add("page-title");
        Label user = muted(identity.fullName() + " · " + identity.username());
        Region topSpacer = new Region();
        HBox topbar = new HBox(16, heading, topSpacer, peerBadge, user);
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.getStyleClass().add("topbar");
        BorderPane dashboard = new BorderPane(content, topbar, null, null, sidebar);
        BorderPane.setMargin(content, new Insets(20));
        replace(dashboard);

        Runnable load = () -> loadProducts(table, search.getText(), state);
        find.setOnAction(event -> load.run());
        search.setOnAction(event -> load.run());
        refresh.setOnAction(event -> { search.clear(); load.run(); refreshPeer(peerBadge); });
        add.setOnAction(event -> productDialog(null).ifPresent(input -> service.createProduct(input,
                identity.username()).whenComplete(done(state, product -> {
                    showToast("Đã thêm " + product.productCode() + ".", false); load.run();
                }))));
        edit.setOnAction(event -> withLease(table.getSelectionModel().getSelectedItem(), state, product ->
                productDialog(product).ifPresentOrElse(input -> service.updateProduct(product.id(), input,
                                identity.username()).whenComplete(done(state, updated -> {
                            showToast("Đã lưu " + updated.productCode() + ".", false); load.run();
                        })), () -> service.releaseLease())));
        importStock.setOnAction(event -> changeInventory(table.getSelectionModel().getSelectedItem(), true,
                identity.username(), state, load));
        exportStock.setOnAction(event -> changeInventory(table.getSelectionModel().getSelectedItem(), false,
                identity.username(), state, load));
        delete.setOnAction(event -> deleteProduct(table.getSelectionModel().getSelectedItem(), identity.username(), state, load));
        history.setOnAction(event -> showHistory(table.getSelectionModel().getSelectedItem(), state));
        load.run();
        refreshPeer(peerBadge);
    }

    private void loadProducts(TableView<Product> table, String search, Label state) {
        state.setText("Đang tải danh sách sản phẩm...");
        table.setPlaceholder(styled("Đang tải dữ liệu...", "skeleton"));
        service.listProducts(search).whenComplete((products, error) -> Platform.runLater(() -> {
            if (error != null) {
                state.setText("Không tải được dữ liệu.");
                table.setPlaceholder(styled("Mất kết nối hoặc server đang lỗi.", "empty-state"));
                showToast(message(error), true);
                return;
            }
            table.setItems(FXCollections.observableArrayList(products));
            table.setPlaceholder(styled("Không có sản phẩm phù hợp.", "empty-state"));
            state.setText(products.isEmpty() ? "Chưa có dữ liệu." : "Đã tải " + products.size() + " sản phẩm.");
        }));
    }

    private void refreshPeer(Label peerBadge) {
        setBadge(peerBadge, "Đang kiểm tra peer", "badge-warning");
        service.isPeerAvailable().whenComplete((available, error) -> Platform.runLater(() -> {
            if (error != null || !Boolean.TRUE.equals(available)) setBadge(peerBadge, "Peer mất kết nối", "badge-danger");
            else setBadge(peerBadge, "Hai server online", "badge-success");
        }));
    }

    private void withLease(Product product, Label state, Consumer<Product> action) {
        if (product == null) return;
        state.setText("Đang xin quyền thao tác...");
        service.acquireLease(product.id()).whenComplete((lease, error) -> Platform.runLater(() -> {
            if (error != null || lease == null) {
                state.setText("Không lấy được quyền thao tác.");
                showToast(error == null
                        ? "Không thể mở giao diện thao tác vì sản phẩm đang bị khóa."
                        : message(error), true);
            } else {
                state.setText("Đã giữ quyền thao tác.");
                action.accept(product);
            }
        }));
    }

    private void changeInventory(Product product, boolean importing, String username, Label state, Runnable reload) {
        withLease(product, state, selected -> {
            TextInputDialog dialog = new TextInputDialog("1");
            dialog.setTitle(importing ? "Nhập kho" : "Xuất kho");
            dialog.setHeaderText(selected.productCode() + " · tồn hiện tại " + selected.quantity());
            dialog.setContentText("Số lượng:");
            var answer = dialog.showAndWait();
            if (answer.isEmpty()) { service.releaseLease(); return; }
            int quantity;
            try {
                quantity = Integer.parseInt(answer.get());
                if (quantity <= 0 || (!importing && quantity > selected.quantity())) throw new NumberFormatException();
            } catch (NumberFormatException error) {
                service.releaseLease();
                showToast("Số lượng không hợp lệ hoặc vượt tồn kho.", true);
                return;
            }
            service.changeInventory(selected.id(), quantity, importing, username)
                    .whenComplete(done(state, transaction -> {
                        showToast("Tồn kho mới: " + transaction.quantityAfter() + ".", false); reload.run();
                    }));
        });
    }

    private void deleteProduct(Product product, String username, Label state, Runnable reload) {
        withLease(product, state, selected -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Xóa sản phẩm " + selected.productCode() + "?", ButtonType.CANCEL, ButtonType.OK);
            confirm.setHeaderText("Xác nhận xóa mềm");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                service.releaseLease();
                return;
            }
            service.deleteProduct(selected.id(), username).whenComplete(done(state, ignored -> {
                showToast("Đã xóa " + selected.productCode() + ".", false); reload.run();
            }));
        });
    }

    private void showHistory(Product product, Label state) {
        if (product == null) return;
        state.setText("Đang tải lịch sử...");
        service.transactionHistory(product.id()).whenComplete((history, error) -> Platform.runLater(() -> {
            if (error != null) { showToast(message(error), true); return; }
            TableView<InventoryTransaction> table = new TableView<>(FXCollections.observableArrayList(history));
            table.getColumns().add(textColumn("Mã", InventoryTransaction::transactionCode, 170));
            table.getColumns().add(textColumn("Loại", InventoryTransaction::transactionType, 90));
            table.getColumns().add(textColumn("SL", value -> String.valueOf(value.quantity()), 70));
            table.getColumns().add(textColumn("Trước → Sau", value -> value.quantityBefore() + " → " + value.quantityAfter(), 120));
            table.getColumns().add(textColumn("Người tạo", InventoryTransaction::createdBy, 120));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
            table.getColumns().add(textColumn("Thời gian", value -> formatter.format(value.createdAt()), 160));
            table.setPrefSize(760, 380);
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Lịch sử " + product.productCode());
            dialog.getDialogPane().setContent(table);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
            state.setText("Sẵn sàng");
        }));
    }

    private java.util.Optional<ProductInput> productDialog(Product product) {
        TextField code = field(product == null ? "" : product.productCode(), "Mã sản phẩm");
        TextField name = field(product == null ? "" : product.productName(), "Tên sản phẩm");
        TextField category = field(product == null ? "" : product.category(), "Danh mục");
        TextField price = field(product == null ? "" : product.price().toPlainString(), "Giá");
        if (product != null) code.setDisable(true);
        Dialog<ProductInput> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "Thêm sản phẩm" : "Sửa sản phẩm");
        dialog.getDialogPane().setContent(form(code, name, category, price));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;
            try {
                BigDecimal amount = new BigDecimal(price.getText().trim());
                if (code.getText().isBlank() || name.getText().isBlank() || category.getText().isBlank()
                        || amount.signum() < 0) throw new IllegalArgumentException();
                return new ProductInput(code.getText().trim(), name.getText().trim(), category.getText().trim(), amount);
            } catch (Exception error) {
                showToast("Điền đủ thông tin và giá không âm.", true);
                return null;
            }
        });
        return dialog.showAndWait();
    }

    private <T> java.util.function.BiConsumer<T, Throwable> done(Label state, Consumer<T> success) {
        return (value, error) -> Platform.runLater(() -> {
            if (error != null) {
                state.setText("Thao tác thất bại.");
                showToast(message(error), true);
            } else {
                state.setText("Sẵn sàng");
                success.accept(value);
            }
        });
    }

    private static TableView<Product> productTable() {
        TableView<Product> table = new TableView<>();
        table.getColumns().add(textColumn("Mã", Product::productCode, 100));
        table.getColumns().add(textColumn("Tên sản phẩm", Product::productName, 210));
        table.getColumns().add(textColumn("Danh mục", Product::category, 150));
        table.getColumns().add(textColumn("Giá", product -> product.price().toPlainString(), 120));
        table.getColumns().add(textColumn("Tồn", product -> String.valueOf(product.quantity()), 80));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private static <T> TableColumn<T, String> textColumn(String title,
            java.util.function.Function<T, String> value, double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void replace(Node content) {
        root.getChildren().setAll(content);
        FadeTransition fade = new FadeTransition(Duration.millis(190), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void showToast(String text, boolean error) {
        if (toast != null) root.getChildren().remove(toast);
        toast = styled(text, "toast");
        Label shownToast = toast;
        toast.getStyleClass().add(error ? "toast-error" : "toast-success");
        StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toast, new Insets(24));
        root.getChildren().add(toast);
        FadeTransition fade = new FadeTransition(Duration.millis(error ? 4200 : 3000), toast);
        fade.setFromValue(1);
        fade.setToValue(error ? 0.92 : 0);
        fade.setOnFinished(event -> root.getChildren().remove(shownToast));
        fade.play();
    }

    private static TextField field(String value, String prompt) {
        TextField field = new TextField(value);
        field.setPromptText(prompt);
        return field;
    }

    private static PasswordField password(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        return field;
    }

    private static VBox form(Node... nodes) {
        VBox form = new VBox(10, nodes);
        form.setPadding(new Insets(16, 0, 8, 0));
        return form;
    }

    private static VBox card(String title, Node... nodes) {
        Label heading = styled(title, "section-title");
        VBox card = new VBox(10);
        card.getChildren().add(heading);
        card.getChildren().addAll(nodes);
        card.getStyleClass().add("card");
        return card;
    }

    private static Button primary(String text) { return button(text, "button-primary"); }
    private static Button secondary(String text) { return button(text, "button-secondary"); }
    private static Button danger(String text) { return button(text, "button-danger"); }
    private static Button button(String text, String style) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private static Label badge(String text, String style) {
        Label badge = styled(text, "badge");
        badge.getStyleClass().add(style);
        return badge;
    }

    private static void setBadge(Label badge, String text, String style) {
        badge.setText(text);
        badge.getStyleClass().removeAll("badge-success", "badge-warning", "badge-danger");
        badge.getStyleClass().add(style);
    }

    private static Label navTitle(String text) { return styled(text, "sidebar-title"); }
    private static Label navItem(String text, boolean active) { return styled(text, active ? "nav-active" : "nav-item"); }
    private static Label muted(String text) { return styled(text, "muted"); }
    private static Label styled(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static String message(Throwable error) {
        Throwable selected = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return selected.getMessage() == null ? "Không thể thực hiện thao tác." : selected.getMessage();
    }

    @Override
    public void close() {
        if (service != null) service.close();
        connector.shutdownNow();
    }
}
