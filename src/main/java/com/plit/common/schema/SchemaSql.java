package com.plit.common.schema;

public class SchemaSql {

    // DROP 전용
    public static final String DROP_USER_TABLE = "DROP TABLE IF EXISTS user;";
    public static final String DROP_PARTY_TABLE = "DROP TABLE IF EXISTS party;";
    public static final String DROP_PARTY_FIND_POSITION_TABLE = "DROP TABLE IF EXISTS party_find_position;";
    public static final String DROP_PARTY_MEMBER_TABLE = "DROP TABLE IF EXISTS party_member;";
    public static final String DROP_CLAN_TABLE = "DROP TABLE IF EXISTS clan;";
    public static final String DROP_CLAN_MEMBER_TABLE = "DROP TABLE IF EXISTS clan_member;";
    public static final String DROP_CLAN_JOIN_REQUEST_TABLE = "DROP TABLE IF EXISTS clan_join_request;";
    public static final String DROP_QNA_TABLE = "DROP TABLE IF EXISTS qna;";
    public static final String DROP_CHAT_ROOM_TABLE = "DROP TABLE IF EXISTS chat_room;";
    public static final String DROP_CHAT_ROOM_USER_TABLE = "DROP TABLE IF EXISTS chat_room_user;";
    public static final String DROP_CHAT_MESSAGE_TABLE = "DROP TABLE IF EXISTS chat_message;";
    public static final String DROP_INQUIRY_ROOM_TABLE = "DROP TABLE IF EXISTS inquiry_room;";
    public static final String DROP_INQUIRY_MESSAGE_TABLE = "DROP TABLE IF EXISTS inquiry_message;";
    public static final String DROP_BLOCKED_USER_TABLE = "DROP TABLE IF EXISTS blocked_user;";
    public static final String DROP_FRIEND_TABLE = "DROP TABLE IF EXISTS friend;";
    public static final String DROP_BLACKLIST_TABLE = "DROP TABLE IF EXISTS blacklist;";
    public static final String DROP_IMAGE_TABLE = "DROP TABLE IF EXISTS image;";
    public static final String DROP_MATCH_SUMMARY_TABLE = "DROP TABLE IF EXISTS match_summary;";
    public static final String DROP_MATCH_PLAYER_TABLE = "DROP TABLE IF EXISTS match_player;";

    public static final String DROP_ALL =
            "SET FOREIGN_KEY_CHECKS = 0;" +
                    DROP_PARTY_FIND_POSITION_TABLE +
                    DROP_PARTY_MEMBER_TABLE +
                    DROP_PARTY_TABLE +
                    DROP_USER_TABLE +
                    DROP_CLAN_MEMBER_TABLE +
                    DROP_CLAN_JOIN_REQUEST_TABLE +
                    DROP_CLAN_TABLE +
                    DROP_QNA_TABLE +
                    DROP_CHAT_ROOM_USER_TABLE +
                    DROP_CHAT_MESSAGE_TABLE +
                    DROP_CHAT_ROOM_TABLE +
                    DROP_INQUIRY_MESSAGE_TABLE +
                    DROP_INQUIRY_ROOM_TABLE +
                    DROP_BLOCKED_USER_TABLE +
                    DROP_FRIEND_TABLE +
                    DROP_BLACKLIST_TABLE +
                    DROP_IMAGE_TABLE +
                    DROP_MATCH_PLAYER_TABLE +
                    DROP_MATCH_SUMMARY_TABLE +
                    "SET FOREIGN_KEY_CHECKS = 1;";

    public static final String CREATE_PARTY_TABLE = """
        CREATE TABLE party (
            party_seq BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '번호',
            party_name VARCHAR(30) NOT NULL COMMENT '파티 이름',
            party_type VARCHAR(6) NOT NULL COMMENT '타입',
            created_at DATETIME NOT NULL COMMENT '생성일자',
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
            created_by varchar(255) NOT NULL COMMENT '생성자 ID',
            party_end_time DATETIME NOT NULL COMMENT '파티 종료 일자',
            party_status VARCHAR(7) DEFAULT 'WAITING' COMMENT '파티 상태 (WAITING, FULL, CLOSED 등)',
            party_headcount INT NOT NULL COMMENT '파티 인원 수',
            party_max INT NOT NULL COMMENT '최대 모집 인원 수',
            memo TEXT COMMENT '메모',
            main_position VARCHAR(10) NOT NULL COMMENT '주 포지션'
        );
        """;

    public static final String CREATE_PARTY_FIND_POSITION_TABLE = """
        CREATE TABLE party_find_position (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            party_seq BIGINT NOT NULL,
            position VARCHAR(10) NOT NULL,
            FOREIGN KEY (party_seq) REFERENCES party(party_seq) ON DELETE CASCADE
        );
        """;

    public static final String CREATE_PARTY_MEMBER_TABLE = """
        CREATE TABLE party_member (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            party_seq BIGINT NOT NULL COMMENT '파티 번호',
            user_seq INT NOT NULL COMMENT '유저 seq',
            join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '가입 시간',
            role VARCHAR(10) DEFAULT 'MEMBER' COMMENT '역할',
            message TEXT,
            status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT '신청 상태',
            position VARCHAR(10) NOT NULL COMMENT '신청한 포지션',
            UNIQUE(party_seq, user_seq)
        );
        """;

    public static final String CREATE_USER_TABLE = """
        CREATE TABLE user (
            user_seq INT AUTO_INCREMENT PRIMARY KEY,
            user_id VARCHAR(30) NOT NULL UNIQUE,
            user_pwd VARCHAR(100) NOT NULL,
            user_nickname VARCHAR(30) NOT NULL UNIQUE,
            use_yn CHAR(1) NOT NULL,
            is_banned BOOLEAN NOT NULL DEFAULT FALSE,
            user_modi_id VARCHAR(16),
            user_modi_date DATE,
            user_create_date DATE NOT NULL,
            user_auth VARCHAR(6) NOT NULL,
            riot_game_name VARCHAR(50),
            riot_tag_line VARCHAR(20),
            puuid VARCHAR(100)
        );
        """;

    public static final String CREATE_CLAN_TABLE = """
        CREATE TABLE clan (
            clan_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '클랜 ID',
            clan_name VARCHAR(100) COMMENT '클랜 이름',
            clan_intro LONGTEXT COMMENT '소개글',
            clan_created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
            kakao_link VARCHAR(255) COMMENT '카카오톡 오픈채팅 링크',
            discord_link VARCHAR(255) COMMENT '디스코드 초대 링크',
            min_tier VARCHAR(30) COMMENT '가입 최소 티어',
            tier VARCHAR(30) COMMENT '클랜 대표 티어',
            image_url VARCHAR(255) COMMENT '클랜 이미지 URL',
            leader_id BIGINT COMMENT '클랜 리더 ID',
            use_yn VARCHAR(255) COMMENT '사용 여부'
        );
        """;

    public static final String CREATE_CLAN_MEMBER_TABLE = """
        CREATE TABLE clan_member (
            clan_member_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '클랜 멤버 ID',
            user_id BIGINT COMMENT '유저 ID',
            clan_id BIGINT COMMENT '클랜 ID',
            role VARCHAR(20) COMMENT '클랜 내 역할',
            joined_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '가입 일자',
            status VARCHAR(20) COMMENT '가입 상태',
            main_position VARCHAR(20) COMMENT '주 포지션',
            intro VARCHAR(30) COMMENT '소개글',
            tier VARCHAR(20) COMMENT '티어 정보'
        );
        """;

    public static final String CREATE_CLAN_JOIN_REQUEST_TABLE = """
        CREATE TABLE clan_join_request (
            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '가입 요청 ID',
            user_id BIGINT COMMENT '유저 ID',
            clan_id BIGINT COMMENT '클랜 ID',
            status VARCHAR(255) COMMENT '요청 상태',
            requested_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '요청 일자',
            responded_at DATETIME COMMENT '응답 일자',
            nickname VARCHAR(255) COMMENT '닉네임',
            position ENUM('ADC','ALL','JUNGLE','MID','SUPPORT','TOP') COMMENT '신청 포지션',
            request_at DATETIME(6),
            intro VARCHAR(255) COMMENT '소개글',
            main_position ENUM('ADC','ALL','JUNGLE','MID','SUPPORT','TOP') COMMENT '주요 포지션',
            tier VARCHAR(255) COMMENT '티어 정보'
        );
        """;

    public static final String CREATE_QNA_TABLE = """
        CREATE TABLE qna (
            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'qna 번호',
            user_id BIGINT COMMENT 'qna 요청 유저 ID',
            title VARCHAR(255) COMMENT 'qna 제목',
            content TEXT COMMENT '문의 내용',
            answer VARCHAR(255) COMMENT '답변',
            status VARCHAR(255) COMMENT '상태',
            asked_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '요청 시간',
            answered_at DATETIME COMMENT '답변 시간',
            file_path VARCHAR(255) COMMENT '첨부파일 경로',
            file_name VARCHAR(255) COMMENT '첨부파일명',
            delete_yn VARCHAR(1) DEFAULT 'N' COMMENT '삭제 여부',
            user_seq INT COMMENT '유저 시퀀스',
            category VARCHAR(255) COMMENT '문의 카테고리',
            admin_deleted BIT(1) COMMENT '관리자 삭제 여부'
        );
        """;

    public static final String CREATE_CHAT_ROOM_TABLE = """
        CREATE TABLE chat_room (
            chat_room_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '채팅방 번호',
            chat_room_type VARCHAR(20) COMMENT '채팅방 타입',
            chat_room_max INT COMMENT '채팅방 최대 인원',
            chat_room_headcount INT COMMENT '채팅방 현재 인원수',
            chat_room_name VARCHAR(100) COMMENT '채팅방 이름',
            chat_room_created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '채팅방 개설 시간',
            party_id BIGINT NULL COMMENT '파티채팅 호출 시 필요한 파티 아이디'
        );
        """;

    public static final String CREATE_CHAT_MESSAGE_TABLE = """
        CREATE TABLE chat_message (
            no BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '메세지 번호',
            chat_room_id BIGINT COMMENT '채팅방 번호',
            sender_id BIGINT COMMENT '보낸사람 닉네임',
            content TEXT COMMENT '메시지 내용',
            sent_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '메시지 보낸 시간'
        );
        """;

    public static final String CREATE_CHAT_ROOM_USER_TABLE = """
        CREATE TABLE chat_room_user (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            chat_room_id BIGINT NOT NULL,
            user_id BIGINT NOT NULL,
            joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            is_read BOOLEAN DEFAULT FALSE
        );
        """;

    public static final String CREATE_INQUIRY_ROOM_TABLE = """
        CREATE TABLE inquiry_room (
            inquiry_room_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '문의 채팅방 ID',
            user_id BIGINT NOT NULL COMMENT '채팅방 만든 유저',
            admin_id BIGINT COMMENT '응답 담당 관리자 ID',
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '채팅방 생성 시간'
        );
        """;

    public static final String CREATE_INQUIRY_MESSAGE_TABLE = """
        CREATE TABLE inquiry_message (
            inquiry_message_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '문의 메시지 ID',
            inquiry_room_id BIGINT NOT NULL COMMENT '문의 채팅방 ID',
            sender_id BIGINT NOT NULL COMMENT '메시지 보낸 유저 ID',
            content TEXT COMMENT '메시지 내용',
            sent_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '메시지 전송시간'
        );
        """;

    public static final String CREATE_BLOCKED_USER_TABLE = """
        CREATE TABLE blocked_user (
            no BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '차단유저 번호',
            blocker_id BIGINT COMMENT '차단한 유저',
            blocked_user_id BIGINT COMMENT '차단당한 유저',
            blocked_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '차단 시간',
            is_released BOOLEAN DEFAULT FALSE COMMENT '차단 해제'
        );
        """;

    public static final String CREATE_FRIEND_TABLE = """
        CREATE TABLE friend (
            friends_no BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '친구 관계 번호',
            from_user_id BIGINT COMMENT '친구요청을 보낸 유저 ID',
            to_user_id BIGINT COMMENT '친구요청을 받은 유저 ID',
            status VARCHAR(20) DEFAULT 'PENDING' COMMENT '친구관계 상태',
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '친구 신청 요청 시간',
            memo TEXT DEFAULT NULL COMMENT '친구 신청 목록 메모'
        );
        """;

    public static final String CREATE_BLACKLIST_TABLE = """
        CREATE TABLE blacklist (
            blacklist_no BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '비매너유저 번호',
            reporter_id BIGINT COMMENT '신고유저 ID',
            reported_user_id BIGINT COMMENT '비매너유저 ID',
            reason TEXT COMMENT '신고 사유',
            status VARCHAR(20) DEFAULT 'PENDING' COMMENT '신고 처리 상태',
            handled_by BIGINT COMMENT '처리 담당자',
            reported_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '신고 시간',
            handled_at DATETIME COMMENT '처리 시간'
        );
        """;

    public static final String CREATE_IMAGE_TABLE = """
        CREATE TABLE image (
            id BIGINT NOT NULL AUTO_INCREMENT,
            name VARCHAR(100) NOT NULL,
            type VARCHAR(50) NOT NULL,
            image_url VARCHAR(255) NOT NULL,
            created_at DATETIME(6) DEFAULT NULL,
            PRIMARY KEY (id),
            KEY idx_name_type (name, type)
        );
        """;

    public static final String CREATE_MATCH_SUMMARY_TABLE = """
        CREATE TABLE match_summary (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            match_id VARCHAR(100) NOT NULL,
            puuid VARCHAR(100) NOT NULL,
            champion_id INT,
            win BOOLEAN,
            team_position VARCHAR(20),
            champion_name VARCHAR(50),
            kills INT,
            deaths INT,
            assists INT,
            kda_ratio DOUBLE,
            tier VARCHAR(30),
            game_end_timestamp DATETIME,
            game_mode VARCHAR(30),
            INDEX idx_match_id (match_id),
            INDEX idx_puuid (puuid)
        );
        """;

    public static final String CREATE_MATCH_PLAYER_TABLE = """
        CREATE TABLE match_player (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            match_id VARCHAR(100),
            puuid VARCHAR(100),
            champion_id INT,
            summoner_name VARCHAR(100),
            champion_name VARCHAR(100),
            kills INT,
            deaths INT,
            assists INT,
            kda_ratio DOUBLE,
            cs INT,
            cs_per_min DOUBLE,
            total_damage_dealt_to_champions INT,
            total_damage_taken INT,
            team_position VARCHAR(50),
            tier VARCHAR(50),
            main_rune1 INT,
            main_rune2 INT,
            stat_rune1 INT,
            stat_rune2 INT,
            wards_placed INT,
            wards_killed INT,
            game_end_timestamp DATETIME,
            game_duration_minutes INT,
            game_duration_seconds INT,
            game_mode VARCHAR(50),
            team_id INT,
            win BOOLEAN,
            item_ids VARCHAR(255),
            gold_earned INT
        );
        """;

    // 전체 테이블 한꺼번에 초기화
    public static final String CREATE_ALL =
            CREATE_USER_TABLE +
            CREATE_PARTY_TABLE +
            CREATE_PARTY_FIND_POSITION_TABLE +
            CREATE_PARTY_MEMBER_TABLE +
            CREATE_CLAN_TABLE +
            CREATE_CLAN_MEMBER_TABLE +
            CREATE_CLAN_JOIN_REQUEST_TABLE +
            CREATE_QNA_TABLE +
            CREATE_CHAT_ROOM_TABLE +
            CREATE_CHAT_MESSAGE_TABLE +
            CREATE_CHAT_ROOM_USER_TABLE +
            CREATE_INQUIRY_ROOM_TABLE +
            CREATE_INQUIRY_MESSAGE_TABLE +
            CREATE_BLOCKED_USER_TABLE +
            CREATE_FRIEND_TABLE +
            CREATE_BLACKLIST_TABLE +
            CREATE_IMAGE_TABLE +
            CREATE_MATCH_SUMMARY_TABLE +
            CREATE_MATCH_PLAYER_TABLE;
}