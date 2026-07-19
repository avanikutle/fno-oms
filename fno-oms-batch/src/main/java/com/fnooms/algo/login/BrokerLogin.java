package com.fnooms.algo.login;

public interface BrokerLogin {
    /**
     * Executes the login flow and returns the JWT/Access Token.
     */
    String login(String userId, String password, String totp) throws Exception;
}
