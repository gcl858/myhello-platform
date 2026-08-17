package org.acme.transfer.api.dto;

public class A16220Response {

    public String code;
    public String message;
    public String Account;

    public A16220Response() {
    }

    public A16220Response(String code, String message, String account) {
        this.code = code;
        this.message = message;
        this.Account = account;
    }
}