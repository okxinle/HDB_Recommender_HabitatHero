package habitathero.auth;

import habitathero.auth.db.Db;

public class AuthDbDemo {
    public static void main(String[] args) throws Exception {
        Db.initSchema();
        System.out.println("Schema initialised OK. Check HabitatHero/data/habitathero.db");
    }
}