package com.plit.FO.qna.inquirychat;

import com.plit.FO.qna.inquirychat.InquiryRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InquiryRoomRepository extends JpaRepository<InquiryRoom, Long> {
    Optional<InquiryRoom> findByUserId(Long userId);

    List<InquiryRoom> findByAdminIdIsNullOrderByCreatedAtAsc();
}