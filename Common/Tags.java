package Common;

public class Tags {
    // auth
    public static final int TAG_LOGIN = 1;
    public static final int TAG_REGISTER = 2;

    // events
    public static final int TAG_ADD_EVENT = 10;    // Adicionar evento ao dia corrente
    public static final int TAG_NEW_DAY = 11;      // Avançar para novo dia (admin)
    public static final int TAG_GET_CURRENT_DAY = 12; // Obter dia atual

    // aggregations
    public static final int TAG_AGG_QUANTITY = 20;     // Quantidade vendida
    public static final int TAG_AGG_VOLUME = 21;       // Volume de vendas (valor total)
    public static final int TAG_AGG_AVG_PRICE = 22;    // Preço médio
    public static final int TAG_AGG_MAX_PRICE = 23;    // Preço máximo

    // filters
    public static final int TAG_FILTER_EVENTS = 30;    // Filtrar eventos por produtos

    // notifications
    public static final int TAG_WAIT_SIMULTANEOUS = 40;  // Esperar vendas simultâneas
    public static final int TAG_WAIT_CONSECUTIVE = 41;   // Esperar vendas consecutivas
    
    // Status de resposta
    public static final byte STATUS_OK = 0;
    public static final byte STATUS_ERROR = 1;
    public static final byte STATUS_INVALID_CREDENTIALS = 2;
    public static final byte STATUS_USER_EXISTS = 3;
    public static final byte STATUS_NOT_AUTHENTICATED = 4;
    public static final byte STATUS_NOT_ADMIN = 5;           // Operação requer admin
    public static final byte STATUS_INVALID_PARAMS = 6;      // Parâmetros inválidos
    public static final byte STATUS_NOT_ENOUGH_DATA = 7;     // Não há dados suficientes
}