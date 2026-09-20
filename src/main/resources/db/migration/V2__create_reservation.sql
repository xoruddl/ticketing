-- 예약. 사용자가 회차의 좌석 하나를 잡은 것.
-- 좌석은 공연에 붙어 있고 회차마다 따로 팔리므로, "이 회차의 이 좌석이 팔렸는가"는 좌석이 아니라 이 표의 행으로 판단한다.
-- V1과 같은 이유로 외래 키는 두지 않는다 (Step 4에서 테이블 소유가 갈라질 수 있다).

-- status 값과 뜻:
--   HELD      선점. expires_at 까지 결제하지 않으면 만료된다
--   CONFIRMED 확정. 결제가 끝났다
--   EXPIRED   만료. 유효 시간 안에 결제되지 않았다
--   CANCELED  취소됨
-- V1의 grade 와 같은 이유로 MySQL enum 대신 varchar 로 둔다. 상태가 늘어도 스키마를 바꾸지 않는다.
create table reservation
(
    id          bigint      not null auto_increment,
    schedule_id bigint      not null,
    seat_id     bigint      not null,
    user_id     bigint      not null,
    status      varchar(20) not null,
    -- 선점 유효 시간이 끝나는 시각. 설정값으로 매번 계산하지 않고 선점할 때 박아둔다.
    -- 설정을 바꿔도 이미 만들어진 선점의 기한이 움직이지 않고, 만료 판정이 이 값과 현재 시각의 비교 한 번으로 끝난다.
    -- 확정·만료·취소된 예약에서는 더 이상 쓰지 않는다.
    expires_at  datetime(6) not null,
    primary key (id),
    -- 한 회차의 한 좌석을 찾는 인덱스. 여기에 유니크 제약을 두면 이중 선점이 막히지만, 일부러 두지 않는다.
    -- Step 1에서 동시 요청으로 이중 선점이 실제로 일어나는 것을 본 뒤에 Step 2에서 막을 방법을 고른다.
    index idx_reservation_schedule_seat (schedule_id, seat_id),
    -- 내 예매 목록 조회용.
    index idx_reservation_user (user_id)
) engine = InnoDB;
