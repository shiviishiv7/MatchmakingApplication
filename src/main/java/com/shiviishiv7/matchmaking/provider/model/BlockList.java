package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.provider.vo.BlockListVO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * When user A blocks user B, B will never appear in A's discovery results
 * for any category. The block is one-directional — B can still see A
 * unless B also blocks A.
 */
@Entity
@Table(name = "BLOCK_LIST",
    uniqueConstraints = @UniqueConstraint(columnNames = {"blockerId", "blockedId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BlockList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "blockerId", nullable = false)
    private Integer blockerId;

    @Column(name = "blockedId", nullable = false)
    private Integer blockedId;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "blockedAt")
    private LocalDateTime blockedAt;

    public BlockList fromVO(BlockListVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setBlockerId(vo.getBlockerId());
        this.setBlockedId(vo.getBlockedId());
        this.setReason(vo.getReason());
        this.setBlockedAt(vo.getBlockedAt());
        return this;
    }

    public BlockListVO toVO() {
        BlockListVO vo = new BlockListVO();
        vo.setId(this.getId());
        vo.setBlockerId(this.getBlockerId());
        vo.setBlockedId(this.getBlockedId());
        vo.setReason(this.getReason());
        vo.setBlockedAt(this.getBlockedAt());
        return vo;
    }
}
