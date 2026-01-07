package Server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class Auth {
    private final Map<String, User> users = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Auth() {
        this.users.put("admin", new User("admin", "admin", true));
    }

    public boolean register(String username, String pass){
        this.lock.lock();
        boolean aux;
        try{
            if (!this.users.containsKey(username)){
                this.users.put(username, new User(username, pass, false));
                aux = true;
                return aux;
            }

            aux = false;
        } finally{
            lock.unlock();
        }
        return aux;
    }

    public User login( String username, String pass){
        this.lock.lock();

        User res;

        try{
            User aux = this.users.get(username);
            if( aux != null && aux.password.equals(pass)){
                res = aux;
                return res;
            }
            res = null;

        } finally {
            this.lock.unlock();
        }
        return res;
    }
    
}
