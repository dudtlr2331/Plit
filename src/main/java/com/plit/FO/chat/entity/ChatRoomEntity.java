package com.plit.FO.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Column(name = "chat_room_type")
    private String chatRoomType;

    @Column(name = "chat_room_max")
    private Integer chatRoomMax;

    @Column(name = "chat_room_headcount")
    private Integer chatRoomHeadcount;

    @Column(name = "chat_room_name")
    private String chatRoomName;

    @Column(name = "chat_room_created_at")
    private LocalDateTime chatRoomCreatedAt;
}
