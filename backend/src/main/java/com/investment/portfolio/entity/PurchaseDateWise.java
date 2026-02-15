package com.investment.portfolio.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "purchase_date_wise")
public class PurchaseDateWise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_date")
    private LocalDate date;

    @Column(name = "company")
    private String company;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "price")
    private Double price;

    @Column(name = "investment")
    private Double investment;

    public PurchaseDateWise() {}

    public PurchaseDateWise(LocalDate date, String company, Double quantity, Double price, Double investment) {
        this.date = date;
        this.company = company;
        this.quantity = quantity;
        this.price = price;
        this.investment = investment;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getInvestment() { return investment; }
    public void setInvestment(Double investment) { this.investment = investment; }
}
