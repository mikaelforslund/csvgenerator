package com.example.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private String accountId;
        
    private AccountData accountData;
    private List<DataItem> dataItems;
    private List<OtherData> otherData; 
}
