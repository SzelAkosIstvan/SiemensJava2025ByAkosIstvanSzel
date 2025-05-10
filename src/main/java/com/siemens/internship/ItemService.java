package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private ConcurrentLinkedQueue<Item> processedItems = new ConcurrentLinkedQueue<>();        // changed variable type to a thread safe one
    private AtomicInteger processedCount = new AtomicInteger(0);                     // changed variable type to a thread safe one


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     Explanation about what was wrong with the given code:
        the threads were making changes on shared variables without synchronization (processedItems and processedCount)
        - it may cause race conditions or unpredictable behaviour

        the function returns processedItems without waiting for other threads to end their processes

        the CompletableFuture.runAsync() was not followed up by a collector what should make the function wait until all processes are done

        wrong variable type usage (int instead of AtomicInteger, List instead of ConcurrentLinkedQueue)
        - the new variables are thread safe against the previous ones
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {

        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Optional<Item>>> processedFutures = itemIds.stream()             // starts to run the processing task on each item asynchronously
                .map(itemId -> CompletableFuture.supplyAsync(() -> processItem(itemId)))        // runAsync changed to supplyAsync because runAsync doesn't return values and I need to collect them
                .toList();

        CompletableFuture<Void> allDone = CompletableFuture.allOf(processedFutures.toArray(new CompletableFuture[0])); // wait for all the futures to be processed


        return allDone.thenApply(v -> processedFutures.stream()                 // using a stream, we iterate through all the processed futures
                .map(CompletableFuture::join)                                   // join each future to get its result
                .filter(Optional::isPresent)                                    // keep only the successful results
                .map(Optional::get)                                             // extract the actual Item object
                .collect(Collectors.toList())                                   // collect all successfully processed items
        );

    }

    private Optional<Item> processItem(Long itemId) {                           // the processing is separated to get a cleaner look
        try {
            Thread.sleep(100);

            Item item = itemRepository.findById(itemId).orElse(null);
            if (item == null) {
                return Optional.empty();                                        // return value changed regarding the function header
            }

            processedCount.incrementAndGet();

            item.setStatus("PROCESSED");
            processedItems.add(item);
            return Optional.of(itemRepository.save(item));                      // return the saved item object and save the changes to the database

        } catch (InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
            return Optional.empty();                                            // In case of any error, the Error is logged and the processed item is just nothing
        }
    }

}

