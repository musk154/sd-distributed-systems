package Server;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPool {
    private final Queue<Runnable> queue;     
    private final Thread[] workers;
    private final Lock lock;                  
    private final Condition notEmpty;         

    public ThreadPool(int numWorkers) {
        this.queue = new LinkedList<>();       
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();  
        this.workers = new Thread[numWorkers];

        for (int i = 0; i < numWorkers; i++) {      // ciclo para começar os workers
            workers[i] = new Thread(() -> {
                while (true) {                      // cada thread tem este while true
                    try {
                        Runnable task = take();  
                        task.run();                 // executa a tarefa
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            workers[i].start();
        }
    }

    public void submit(Runnable task) {
        lock.lock();
        try {
            queue.add(task);      // adiciona ao final da fila
            notEmpty.signal();    // acorda uma thread
        } finally {
            lock.unlock();
        }
    }

    private Runnable take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await(); 
            }
            return queue.poll();   // remove do início da fila o que garante ordering
        } finally {
            lock.unlock();
        }
    }
}