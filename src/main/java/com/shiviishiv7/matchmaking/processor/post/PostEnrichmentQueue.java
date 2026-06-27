package com.shiviishiv7.matchmaking.processor.post;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostEnrichmentQueue {

    @Value("${matchmaking.enrichment.workers:5}")
    private int workerCount;

    @Value("${matchmaking.enrichment.queue-capacity:500}")
    private int queueCapacity;

    private final PostEnrichmentProcessor enrichmentProcessor;

    private BlockingQueue<PostEnrichmentTask> queue;
    private ExecutorService workerPool;

    @PostConstruct
    public void start() {
        queue = new LinkedBlockingQueue<>(queueCapacity);
        workerPool = Executors.newFixedThreadPool(workerCount);
        for (int i = 0; i < workerCount; i++) {
            workerPool.submit(this::runWorker);
        }
        log.info("PostEnrichmentQueue started with {} workers, capacity={}", workerCount, queueCapacity);
    }

    public boolean enqueue(PostEnrichmentTask task) {
        boolean accepted = queue.offer(task);
        if (!accepted) {
            log.error("ALERT_FOR_ERROR: Enrichment queue full (capacity={}), dropping postId={}", queueCapacity, task.postId());
        }
        return accepted;
    }

    private void runWorker() {
        log.info("Enrichment worker started: {}", Thread.currentThread().getName());
        while (!Thread.currentThread().isInterrupted()) {
            try {
                PostEnrichmentTask task = queue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    enrichmentProcessor.enrich(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // enrich() handles its own errors and sends WS error notification;
                // catching here prevents the worker thread from dying on unexpected exceptions
                log.error("ALERT_FOR_ERROR: Unexpected error in enrichment worker: {}", e.getMessage(), e);
            }
        }
        log.info("Enrichment worker stopped: {}", Thread.currentThread().getName());
    }

    @PreDestroy
    public void stop() {
        workerPool.shutdownNow();
        log.info("PostEnrichmentQueue shut down, {} tasks still pending", queue.size());
    }
}
