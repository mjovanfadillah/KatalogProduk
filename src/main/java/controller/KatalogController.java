package controller;

import data.ProdukRepository;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import model.Produk;

public class KatalogController {

    @FXML private TextField namaField;
    @FXML private TextField hargaField;
    @FXML private ComboBox<String> fotoBox;
    @FXML private Label pesanLabel;
    @FXML private Label jumlahLabel;
    @FXML private Label formTitleLabel;
    @FXML private Button simpanButton;
    @FXML private Button batalButton;
    @FXML private TilePane katalogPane;

    private final ProdukRepository repository = new ProdukRepository();
    private Long editingId = null;

    @FXML
    private void initialize() {
        fotoBox.getItems().addAll("roti.png", "susu.png", "kopi.png");
        fotoBox.setValue("roti.png");
        try {
            showData();
        } catch (SQLException e) {
            setPesan("Gagal membaca database: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void showData() throws SQLException {
        ArrayList<Produk> daftar = repository.getAll();
        katalogPane.getChildren().clear();
        for (Produk produk : daftar) {
            katalogPane.getChildren().add(createProductCard(produk));
        }
        jumlahLabel.setText("Jumlah produk: " + daftar.size());
    }

    private VBox createProductCard(Produk produk) {
        ImageView fotoView = new ImageView();
        fotoView.setFitWidth(170);
        fotoView.setFitHeight(110);
        fotoView.setPreserveRatio(true);
        StackPane bingkai = new StackPane();
        bingkai.setPrefSize(180, 120);
        bingkai.setMinSize(180, 120);
        bingkai.setMaxSize(180, 120);

        URL lokasi = getClass().getResource("/image/" + produk.getFoto());
        if (lokasi != null) {
            Image gambar = new Image(lokasi.toExternalForm(), 170, 110, true, true);
            if (!gambar.isError()) {
                fotoView.setImage(gambar);
                bingkai.getChildren().add(fotoView);
            } else {
                bingkai.getChildren().add(new Label("Gambar tidak terbaca"));
            }
        } else {
            bingkai.getChildren().add(new Label("Gambar tidak tersedia"));
        }

        Label namaLabel = new Label(produk.getNama());
        namaLabel.setWrapText(true);
        namaLabel.getStyleClass().add("nama-produk");
        Label hargaLabel = new Label(formatRupiah(produk.getHarga()));
        Label idLabel = new Label("ID: " + produk.getId());
        idLabel.getStyleClass().add("muted");

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("button-edit");
        editButton.setOnAction(event -> startEdit(produk));

        Button deleteButton = new Button("Hapus");
        deleteButton.getStyleClass().add("button-delete");
        deleteButton.setOnAction(event -> deleteProduct(produk));

        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(8, editButton, deleteButton);
        VBox kartu = new VBox(8, bingkai, namaLabel, hargaLabel, idLabel, actions);
        kartu.setMinWidth(220);
        kartu.setPrefWidth(220);
        kartu.setMaxWidth(220);
        kartu.getStyleClass().add("kartu-produk");
        return kartu;
    }

    @FXML
    private void saveProduct() {
        final Produk produk;
        try {
            String nama = namaField.getText();
            String teksHarga = hargaField.getText();
            if (teksHarga == null || teksHarga.isBlank()) {
                throw new IllegalArgumentException("Harga wajib diisi.");
            }
            long harga = Long.parseLong(teksHarga.trim());
            produk = new Produk(nama, harga, fotoBox.getValue());
        } catch (NumberFormatException e) {
            setPesan("Harga harus berupa angka bulat, contoh 12000.", true);
            return;
        } catch (IllegalArgumentException e) {
            setPesan(e.getMessage(), true);
            return;
        }

        try {
            if (editingId == null) {
                repository.add(produk);
                setPesan("Produk berhasil ditambahkan.", false);
            } else {
                boolean updated = repository.update(editingId, produk);
                if (!updated) {
                    setPesan("Produk tidak ditemukan atau sudah dihapus.", true);
                } else {
                    setPesan("Produk berhasil diperbarui.", false);
                }
            }
            resetForm();
            showData();
        } catch (SQLException e) {
            setPesan("Gagal menyimpan perubahan: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void startEdit(Produk produk) {
        editingId = produk.getId();
        namaField.setText(produk.getNama());
        hargaField.setText(String.valueOf(produk.getHarga()));
        fotoBox.setValue(produk.getFoto());
        formTitleLabel.setText("Edit Produk (ID: " + produk.getId() + ")");
        simpanButton.setText("Simpan Perubahan");
        batalButton.setVisible(true);
        batalButton.setManaged(true);
        setPesan("Ubah data pada form, lalu tekan Simpan Perubahan.", false);
        namaField.requestFocus();
    }

    private void deleteProduct(Produk produk) {
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Hapus");
        konfirmasi.setHeaderText("Hapus produk " + produk.getNama() + "?");
        konfirmasi.setContentText("Data yang dihapus tidak dapat dikembalikan.");
        konfirmasi.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    boolean deleted = repository.delete(produk.getId());
                    if (deleted) {
                        if (editingId != null && editingId == produk.getId()) {
                            resetForm();
                        }
                        showData();
                        setPesan("Produk berhasil dihapus.", false);
                    } else {
                        setPesan("Produk tidak ditemukan atau sudah dihapus.", true);
                    }
                } catch (SQLException e) {
                    setPesan("Gagal menghapus produk: " + e.getMessage(), true);
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void cancelEdit() {
        resetForm();
        setPesan("Mode edit dibatalkan.", false);
    }

    private void resetForm() {
        editingId = null;
        namaField.clear();
        hargaField.clear();
        fotoBox.setValue("roti.png");
        formTitleLabel.setText("Tambah Produk");
        simpanButton.setText("Simpan Produk");
        batalButton.setVisible(false);
        batalButton.setManaged(false);
    }

    private void setPesan(String pesan, boolean error) {
        pesanLabel.setText(pesan);
        pesanLabel.getStyleClass().removeAll("message-error", "message-success");
        pesanLabel.getStyleClass().add(error ? "message-error" : "message-success");
    }

    private String formatRupiah(long nilai) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        return "Rp" + format.format(nilai);
    }
}
