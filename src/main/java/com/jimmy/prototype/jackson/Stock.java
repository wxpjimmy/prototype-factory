package com.jimmy.prototype.jackson;

import java.util.Date;
import java.util.Objects;

public class Stock {
    private String name;
    private double price;
    private Date date;
    private String desc;

    public Stock(String name, double price, Date date, String desc) {
        this.name = name;
        this.price = price;
        this.date = date;
        this.desc = desc;
    }

    public Stock() {

    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return this.price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date dt) {
        this.date = dt;
    }

    public String getDesc() {
        return  this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "name='" + name + '\'' +
                ", price=" + price +
                ", date=" + date +
                ", desc='" + desc + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return Double.compare(stock.price, price) == 0 &&
                Objects.equals(name, stock.name) &&
                Objects.equals(date, stock.date) &&
                Objects.equals(desc, stock.desc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, price, date, desc);
    }
}
