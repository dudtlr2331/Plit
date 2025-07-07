package com.plit.BO.user;

import lombok.Data;

import java.util.List;

@Data
public class BoUserDTO {
    private List<Integer> userSeqList;
    private String action;
}
