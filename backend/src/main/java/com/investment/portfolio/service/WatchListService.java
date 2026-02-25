package com.investment.portfolio.service;

import com.investment.portfolio.entity.Instrument;
import com.investment.portfolio.entity.WatchList;
import com.investment.portfolio.repository.InstrumentRepository;
import com.investment.portfolio.repository.WatchListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WatchListService {

    @Autowired
    private WatchListRepository watchListRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    public List<WatchList> getAllWatchLists() {
        return watchListRepository.findAll();
    }

    public Optional<WatchList> getWatchListById(Long id) {
        return watchListRepository.findById(id);
    }

    public Optional<WatchList> getWatchListByName(String name) {
        return watchListRepository.findByName(name);
    }

    @Transactional
    public WatchList createWatchList(String name, String description) {
        if (watchListRepository.existsByName(name)) {
            throw new IllegalArgumentException("Watch list with name '" + name + "' already exists");
        }
        WatchList watchList = new WatchList(name, description);
        return watchListRepository.save(watchList);
    }

    @Transactional
    public WatchList updateWatchList(Long id, String name, String description) {
        WatchList watchList = watchListRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Watch list not found with id: " + id));

        Optional<WatchList> existingWithName = watchListRepository.findByName(name);
        if (existingWithName.isPresent() && !existingWithName.get().getId().equals(id)) {
            throw new IllegalArgumentException("Watch list with name '" + name + "' already exists");
        }

        watchList.setName(name);
        watchList.setDescription(description);
        return watchListRepository.save(watchList);
    }

    @Transactional
    public void deleteWatchList(Long id) {
        WatchList watchList = watchListRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Watch list not found with id: " + id));
        watchListRepository.delete(watchList);
    }

    @Transactional
    public WatchList addInstrumentToWatchList(Long watchListId, Long instrumentId) {
        WatchList watchList = watchListRepository.findById(watchListId)
                .orElseThrow(() -> new IllegalArgumentException("Watch list not found with id: " + watchListId));

        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with id: " + instrumentId));

        watchList.addInstrument(instrument);
        return watchListRepository.save(watchList);
    }

    @Transactional
    public WatchList addInstrumentBySymbol(Long watchListId, String tradingSymbol) {
        WatchList watchList = watchListRepository.findById(watchListId)
                .orElseThrow(() -> new IllegalArgumentException("Watch list not found with id: " + watchListId));

        Instrument instrument = instrumentRepository.findByTradingSymbol(tradingSymbol)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with symbol: " + tradingSymbol));

        watchList.addInstrument(instrument);
        return watchListRepository.save(watchList);
    }

    @Transactional
    public WatchList removeInstrumentFromWatchList(Long watchListId, Long instrumentId) {
        WatchList watchList = watchListRepository.findById(watchListId)
                .orElseThrow(() -> new IllegalArgumentException("Watch list not found with id: " + watchListId));

        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found with id: " + instrumentId));

        watchList.removeInstrument(instrument);
        return watchListRepository.save(watchList);
    }

    @Transactional
    public WatchList addMultipleInstruments(Long watchListId, List<Long> instrumentIds) {
        WatchList watchList = watchListRepository.findById(watchListId)
                .orElseThrow(() -> new IllegalArgumentException("Watch list not found with id: " + watchListId));

        List<Instrument> instruments = instrumentRepository.findAllById(instrumentIds);
        instruments.forEach(watchList::addInstrument);

        return watchListRepository.save(watchList);
    }
}
