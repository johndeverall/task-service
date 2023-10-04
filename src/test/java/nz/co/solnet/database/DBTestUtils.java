package nz.co.solnet.database;

public class DBTestUtils {

    public static void cleanDatabase() {
        DatabaseContext.getInstance().cleanDatabase();
    }
}
