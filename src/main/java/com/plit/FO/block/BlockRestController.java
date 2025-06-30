package com.plit.FO.block;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blocks")
public class BlockRestController {

    @Autowired
    private BlockService blockService;

    @PostMapping("/{blockNo}/release")
    public ResponseEntity<?> releaseBlock(@PathVariable Integer blockNo) {
        blockService.releaseBlock(blockNo);
        return ResponseEntity.ok().build();
    }
}
