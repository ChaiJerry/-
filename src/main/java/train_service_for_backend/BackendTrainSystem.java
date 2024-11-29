package train_service_for_backend;

import java.util.concurrent.*;

public class BackendTrainSystem {
    private final int poolSize; // 线程池的大小
    private final ExecutorService executorService;
    public BackendTrainSystem(int poolSize) {
        this.poolSize = poolSize;
        executorService = Executors.newFixedThreadPool(poolSize);
    }




}
