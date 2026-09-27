-- 공연·회차·좌석. 엔티티는 연관관계 대신 ID로 서로를 참조하므로 외래 키는 두지 않는다.
-- Step 4에서 모듈을 나누면 테이블 소유가 갈라질 수 있어, 참조 무결성은 애플리케이션이 지킨다.

-- 공연. 예: 레미제라블 @ 블루스퀘어
create table performance
(
    id    bigint       not null auto_increment,
    title varchar(100) not null,
    venue varchar(100) not null,
    primary key (id)
) engine = InnoDB;

-- 회차. 같은 좌석이 회차마다 따로 팔린다.
create table schedule
(
    id             bigint      not null auto_increment,
    performance_id bigint      not null,
    start_at       datetime(6) not null,
    primary key (id),
    index idx_schedule_performance (performance_id)
) engine = InnoDB;

-- 좌석. 모든 회차가 같은 행을 함께 쓰고, 예매 가능 여부는 회차별 예약으로 판단한다.
-- section, row_name, seat_number 는 Seat 엔티티의 SeatPosition 값 객체가 풀려 저장된 컬럼이다.
-- grade 는 MySQL enum 대신 varchar 로 두어, 등급이 늘어도 스키마를 바꾸지 않는다.
create table seat
(
    id             bigint      not null auto_increment,
    performance_id bigint      not null,
    section        varchar(10) not null,
    row_name       varchar(10) not null,
    seat_number    int         not null,
    grade          varchar(10) not null,
    price          bigint      not null,
    primary key (id),
    -- 한 공연에 같은 위치의 좌석이 두 번 만들어지지 않게 한다. performance_id 로 좌석을 찾는 인덱스도 겸한다.
    unique key uk_seat_position (performance_id, section, row_name, seat_number)
) engine = InnoDB;
