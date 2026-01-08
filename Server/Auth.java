package Server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Auth {
    private final Map<String, User> users = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Auth() {
        this.users.put("admin", new User("admin", "admin", true));
    }

    public boolean register(String username, String pass){
        this.lock.writeLock().lock();
        boolean aux;
        try{
            if (!this.users.containsKey(username)){
                this.users.put(username, new User(username, pass, false));
                aux = true;
                return aux;
            }

            aux = false;
        } finally{
            lock.writeLock().unlock();
        }
        return aux;
    }

    public User login( String username, String pass){
        this.lock.readLock().lock();

        User res;

        try{
            User aux = this.users.get(username);
            if( aux != null && aux.password.equals(pass)){
                res = aux;
                return res;
            }
            res = null;

        } finally {
            this.lock.readLock().unlock();
        }
        return res;
    }
    
}
