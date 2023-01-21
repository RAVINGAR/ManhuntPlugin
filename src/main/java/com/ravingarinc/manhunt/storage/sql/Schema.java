package com.ravingarinc.manhunt.storage.sql;

@SuppressWarnings("PMD.FieldNamingConventions")
public class Schema {
    public static final String PLAYERS = "players";

    public static final String UUID = "uuid";
    public static final String LAST_ATTEMPT = "last_attempt";
    public static final String createTable = "CREATE TABLE IF NOT EXISTS " + PLAYERS + " (" +
            UUID + " TEXT PRIMARY KEY," +
            LAST_ATTEMPT + " TEXT NOT NULL) WITHOUT ROWID;";


    public static final String selectAll = "SELECT * FROM " + PLAYERS;

    public static final String select = "SELECT " + LAST_ATTEMPT +
            " FROM " + PLAYERS +
            " WHERE " + UUID + " = ?";

    public static final String insert = "INSERT INTO " + PLAYERS + "(" +
            UUID + "," +
            LAST_ATTEMPT + ") VALUES(?,?)";

    public static final String update = "UPDATE " + PLAYERS + " SET " +
            LAST_ATTEMPT + " = ? " +
            "WHERE " + UUID + " = ?";

}
