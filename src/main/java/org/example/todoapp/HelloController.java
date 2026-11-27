package org.example.todoapp;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.control.cell.CheckBoxListCell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class HelloController {
    @FXML private TextField inputField;
    @FXML private ListView<TodoItem> todoList;
    @FXML private Label countLabel;
    @FXML private Label messageLabel;
    @FXML private CheckBox onlyUndoneCheck;
    @FXML private DatePicker dueDatePicker;
    // 一覧のデータ。チェックの ON/OFF でも「変更あり」と分かるようにしておく
    private final ObservableList<TodoItem> items =
            FXCollections.observableArrayList(item -> new Observable[]{item.doneProperty()});
    // 保存先：C:\Users\（ユーザー名）\todo.txt
    private static final Path SAVE_FILE = Path.of(System.getProperty("user.home"), "todo.txt");

    // 画面ができた直後に1回だけ呼ばれる（C# の Form_Load）
    @FXML
    public void initialize() {
        // 追加機能：未完了だけ表示するチェックボックス
        // FilteredList は items を「条件で絞り込んで見せる窓」。元の items はそのまま
        FilteredList<TodoItem> filtered = new FilteredList<>(items);
        onlyUndoneCheck.selectedProperty().addListener((obs, oldValue, onlyUndone) ->
                filtered.setPredicate(onlyUndone ? item -> !item.isDone() : null));
        todoList.setItems(filtered);
        // 各行をチェックボックス付きで表示し、TodoItem の done とつなぐ
        todoList.setCellFactory(CheckBoxListCell.forListView(TodoItem::doneProperty));
        // 追加機能：ダブルクリックでタイトルを書き換える
        todoList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                onEditTitle();
            }
        });
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
        // 追加機能：期限日（未選択なら null）
        items.add(new TodoItem(text, false, dueDatePicker.getValue()));
        inputField.clear();
        dueDatePicker.setValue(null);
        inputField.requestFocus();
    }

    // 選択中の項目のタイトルをダイアログで書き換える
    private void onEditTitle() {
        TodoItem selected = todoList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        // 絞り込み中は画面の行番号と items の位置がずれるので、items 側の位置を探す
        int index = items.indexOf(selected);
        TextInputDialog dialog = new TextInputDialog(selected.getTitle());
        dialog.setTitle("タイトルの変更");
        dialog.setHeaderText(null);
        dialog.setContentText("新しいタイトル：");
        dialog.showAndWait()
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                // 差し替えると一覧の表示が更新され、保存も走る
                .ifPresent(text -> items.set(index, new TodoItem(text, selected.isDone(), selected.getDueDate())));
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
    // 1行 = 「完了なら1、未完了なら0」＋タブ＋期限日（なければ空）＋タブ＋タイトル
    // 例：0\t2026-12-01\t買い物 ／ 期限なし：0\t\t買い物
    private void save() {
        List<String> lines = items.stream()
                .map(item -> (item.isDone() ? "1" : "0")
                        + "\t" + (item.getDueDate() == null ? "" : item.getDueDate().toString())
                        + "\t" + item.getTitle())
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
                    .map(this::parseLine)
                    .toList();
            items.setAll(loaded);
        } catch (IOException e) {
            showError("読み込みに失敗しました：" + e.getMessage());
        }
    }

    // 1行を TodoItem に戻す。期限日を付ける前の形式（完了＋タブ＋タイトル）も読める
    private TodoItem parseLine(String line) {
        String[] parts = line.split("\t", 3);
        boolean done = parts[0].equals("1");
        if (parts.length == 3) {
            try {
                LocalDate due = parts[1].isEmpty() ? null : LocalDate.parse(parts[1]);
                return new TodoItem(parts[2], done, due);
            } catch (DateTimeParseException e) {
                // 2列目が日付でない → 古い形式でタイトルにタブが入っていた
            }
        }
        String[] old = line.split("\t", 2);
        return new TodoItem(old[1], done);
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }
}