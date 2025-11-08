package org.stone.springboot.annotation;
import java.sql.SQLException;
import java.sql.Statement;
public class Person {
    private String name;

    private int age;

    private Object statement;

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setStatement(Object statement) {
        this.statement = statement;
    }

    public boolean cancelStatement() throws SQLException{
        return false;
    }
}