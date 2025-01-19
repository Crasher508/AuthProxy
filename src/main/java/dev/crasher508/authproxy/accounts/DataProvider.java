package dev.crasher508.authproxy.accounts;

import com.google.gson.JsonObject;

import java.util.Map;

public interface DataProvider {

    Map<String, JsonObject> getAllAccounts();

    JsonObject getAccountByName(String name);

    boolean saveAccount(String name, JsonObject account);
}
