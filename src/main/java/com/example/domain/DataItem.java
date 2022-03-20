package com.example.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataItem {
    private String transactionId;
    private String description;
    private double amount;

    @Builder.Default
    private List<String> tags = new ArrayList<>();
}
