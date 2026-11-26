package org.example.todoapp;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class TodoItem {
    private final String title;
    //bool型はJavaではboolean、SimpleBooleanPropertyは値に変化が起きたことを伝える機能がある
    private final BooleanProperty done = new SimpleBooleanProperty();

    public TodoItem(String title,boolean done){
        this.title = title;
        this.done.set(done);
    }

    public String getTitle(){
        return title;
    }

    public boolean isDone(){
        return done.get();
    }

    //監視対象として今自分のdone状態がどうなっているのかを渡す
    public BooleanProperty doneProperty(){
        return done;
    }

    //クラスは基本的にtoString()を持ってます
    @Override
    public String toString(){
        return title;
    }
}
