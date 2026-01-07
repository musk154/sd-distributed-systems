package Common;

public class Protocolo {
    // Tags de operação
    public static final int TAG_LOGIN = 1;
    public static final int TAG_REGISTER = 2;
    
    // Status de resposta
    public static final byte STATUS_OK = 0;
    public static final byte STATUS_ERROR = 1;
    public static final byte STATUS_INVALID_CREDENTIALS = 2;
    public static final byte STATUS_USER_EXISTS = 3;
}
