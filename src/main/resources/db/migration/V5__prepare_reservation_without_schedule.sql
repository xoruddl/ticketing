-- 회차를 빼는 동안의 중간 단계. 회차 없이 좌석만으로 예약을 만들 수 있게 한다 (DECISIONS.md "도메인: 회차(Schedule)를 뺀다").
--
-- 엔티티가 아직 schedule_id 를 매핑하고 있어 컬럼을 바로 지울 수 없다 (ddl-auto: validate).
-- 그래서 여기서는 비워둘 수만 있게 하고, 엔티티에서 매핑을 뗀 뒤 V6에서 schedule 테이블과 함께 지운다.
alter table reservation
    modify column schedule_id bigint null;

-- 회차 없이 "이 좌석이 팔렸는가"를 묻는 인덱스. 기존 idx_reservation_schedule_seat 는 schedule_id 가 앞이라
-- seat_id 만으로 찾을 때 타지 못한다. 기존 인덱스는 회차 기준 조회가 남아 있는 동안 두고 V6에서 지운다.
-- V2와 같은 이유로 유니크 제약은 두지 않는다. 이중 선점을 막는 방법은 Step 2에서 고른다.
create index idx_reservation_seat on reservation (seat_id);
