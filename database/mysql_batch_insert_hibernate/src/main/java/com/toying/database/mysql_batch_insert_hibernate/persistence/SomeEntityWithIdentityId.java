package com.toying.database.mysql_batch_insert_hibernate.persistence;

import javax.persistence.*;

@Entity
@Table(name = SomeEntityWithIdentityId.TABLE_NAME)
public class SomeEntityWithIdentityId extends EntityBase {
    public final static String TABLE_NAME = "ENTITY_IDENTITY";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(nullable = false, length = 50)
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
