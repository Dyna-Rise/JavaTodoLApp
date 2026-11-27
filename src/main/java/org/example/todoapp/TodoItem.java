package org.example.todoapp;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TodoItem {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final String title;
    private final BooleanProperty done = new SimpleBooleanProperty();
    // 期限日（付けないときは null）
    private final LocalDate dueDate;

    public TodoItem(String title , boolean done){
        this(title, done, null);
    }

    public TodoItem(String title, boolean done, LocalDate dueDate) {
        this.title = title;
        this.done.set(done);
        this.dueDate = dueDate;
    }

    public String getTitle(){
        return title;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    //チェックボックスと「つなぐ」ためのプロパティ
    public BooleanProperty doneProperty(){
        return done;
    }

    public boolean isDone() {
        return done.get();
    }

    //一覧に表示される文字
    @Override
    public String toString(){
        if (dueDate == null) {
            return title;
        }
        return title + "（期限：" + dueDate.format(DISPLAY_FORMAT) + "）";
    }

}
