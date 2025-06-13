package org.shark.melian.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.shark.melian.model.ChunkDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkPageDto {
    private List<ChunkDto> chunks;
    private Boolean hasMore;
    private String nextAfterId;
}
