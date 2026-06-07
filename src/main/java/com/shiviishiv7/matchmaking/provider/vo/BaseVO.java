package com.shiviishiv7.matchmaking.provider.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseVO {

    private int statusCode;
    private String message;
    private String description;
    private Object data;

    public BaseVO(int statusCode, String message, String description) {
        this.statusCode = statusCode;
        this.message = message;
        this.description = description;
    }

    public BaseVO(int statusCode, String message, String description, Object data) {
        this.statusCode = statusCode;
        this.message = message;
        this.description = description;
        this.data = data;
    }
}
