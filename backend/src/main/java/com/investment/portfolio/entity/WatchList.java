package com.investment.portfolio.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "watch_lists")
public class WatchList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToMany
    @JoinTable(
        name = "watchlist_instrument_map",
        joinColumns = @JoinColumn(name = "watchlist_id"),
        inverseJoinColumns = @JoinColumn(name = "instrument_id")
    )
    private Set<Instrument> instruments = new HashSet<>();

    public WatchList() {}

    public WatchList(String name) {
        this.name = name;
    }

    public WatchList(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<Instrument> getInstruments() { return instruments; }
    public void setInstruments(Set<Instrument> instruments) { this.instruments = instruments; }

    public void addInstrument(Instrument instrument) {
        this.instruments.add(instrument);
    }

    public void removeInstrument(Instrument instrument) {
        this.instruments.remove(instrument);
    }
}
