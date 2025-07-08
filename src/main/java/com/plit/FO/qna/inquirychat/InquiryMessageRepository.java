package com.plit.FO.qna.inquirychat;

import com.plit.FO.qna.inquirychat.InquiryMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryMessageRepository extends JpaRepository<InquiryMessage, Long> {
    List<InquiryMessage> findByInquiryRoomIdOrderBySentAtAsc(Long roomId);
}