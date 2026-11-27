package org.example.todoapp;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class HelloController {
    @FXML private TextField inputField;
    @FXML private ListView<TodoItem> todoList;
    @FXML private Label countLabel;
    @FXML private Label messageLabel;
    // 一覧のデータ。チェックの ON/OFF でも「変更あり」と分かるようにしておく
    private final ObservableList<TodoItem> items =
            FXCollections.observableArrayList(item -> new Observable[]{item.doneProperty()});
    // 保存先：C:\Users\（ユーザー名）\todo.txt
    private static final Path SAVE_FILE = Path.of(System.getProperty("user.home"), "todo.txt");

    // 画面ができた直後に1回だけ呼ばれる（C# の Form_Load）
    @FXML
    public void initialize() {
        todoList.setItems(items);
        // 各行をチェックボックス付きで表示し、TodoItem の done とつなぐ
        todoList.setCellFactory(CheckBoxListCell.forListView(TodoItem::doneProperty));
        load();          // 段階4：起動時に読み込む
        updateCount();
        // 一覧が変わるたびに、件数の表示と保存をする
        items.addListener((ListChangeListener<TodoItem>) change -> {
            updateCount();
            save();      // 段階4
        });
    }

    // 追加ボタン／入力欄で Enter
    @FXML
    private void onAdd() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            // 追加機能：空欄のまま追加したらメッセージを出す
            messageLabel.setText("やることを入力してください");
            inputField.requestFocus();
            return;
        }
        messageLabel.setText("");
        items.add(new TodoItem(text, false));
        inputField.clear();
        inputField.requestFocus();
    }

    // 選択を削除
    @FXML
    private void onDelete() {
        TodoItem selected = todoList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            items.remove(selected);
        }
    }

    // 完了を片付ける
    @FXML
    private void onClearDone() {
        items.removeIf(TodoItem::isDone);
    }

    // 「残り ○ 件 / 全 ○ 件」 … Stream API（C# の LINQ）
    private void updateCount() {
        // 追加機能：0件のときは「やることはありません」と出す
        if (items.isEmpty()) {
            countLabel.setText("やることはありません");
            return;
        }
        long remaining = items.stream()
                .filter(item -> !item.isDone())
                .count();
        countLabel.setText("残り " + remaining + " 件 / 全 " + items.size() + " 件");
    }

    // ===== 段階4：ファイルに保存・読み込み =====
    // 1行 = 「完了なら1、未完了なら0」＋タブ＋タイトル
    private void save() {
        List<String> lines = items.stream()
                .map(item -> (item.isDone() ? "1" : "0") + "\t" + item.getTitle())
                .toList();
        try {
            Files.write(SAVE_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            showError("保存に失敗しました：" + e.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(SAVE_FILE)) {
            return;
        }
        try {
            List<TodoItem> loaded = Files.readAllLines(SAVE_FILE, StandardCharsets.UTF_8).stream()
                    .filter(line -> line.contains("\t"))
                    .map(line -> {
                        String[] parts = line.split("\t", 2);
                        return new TodoItem(parts[1], parts[0].equals("1"));
                    })
                    .toList();
            items.setAll(loaded);
        } catch (IOException e) {
            showError("読み込みに失敗しました：" + e.getMessage());
        }
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }
}