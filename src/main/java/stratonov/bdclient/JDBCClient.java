package stratonov.bdclient;

import java.util.List;

/**
 * Created by strat on 23.08.15.
 */
public interface JDBCClient {
    boolean accessToDB(String login, String password);
    String getLogin();
    List<String> getTableNames();
    List<String> getTableColumns();

}
