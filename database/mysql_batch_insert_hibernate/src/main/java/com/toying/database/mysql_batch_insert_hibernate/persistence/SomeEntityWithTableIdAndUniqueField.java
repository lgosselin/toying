package com.toying.database.mysql_batch_insert_hibernate.persistence;

import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;

@Entity
@Table(name = SomeEntityWithTableIdAndUniqueField.TABLE_NAME)
@SQLInsert(sql = "INSERT IGNORE INTO " + SomeEntityWithTableIdAndUniqueField.TABLE_NAME + " (anotherLong, someString, ts, id) values (?, ?, ?, ?) ")
public class SomeEntityWithTableIdAndUniqueField extends EntityBase {
    public final static String TABLE_NAME = "ENTITY_TABLE_UNIQUE";

    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "DEFAULT")
    @TableGenerator(
            name = "DEFAULT",
            table = "ID_VAULT", pkColumnName = "table_name", valueColumnName = "id",
            allocationSize = 50)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(nullable = false, length = 50, unique = true)
    public String getSomeString() {
        return someString;
    }

    public void setSomeString(String someString) {
        this.someString = someString;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public long getAnotherLong() {
        return anotherLong;
    }

    public void setAnotherLong(long anotherLong) {
        this.anotherLong = anotherLong;
    }
}
